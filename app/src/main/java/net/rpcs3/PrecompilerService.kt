package net.rpcs3

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.Uri
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import kotlin.concurrent.thread

enum class PrecompilerServiceAction {
    InstallFirmware,
    Install
}

class PrecompilerService : Service() {
    private var progressId = 1L;
    companion object {
        fun start(context: Context, action: PrecompilerServiceAction, uri: Uri?) {
            val intent = Intent(context, PrecompilerService::class.java)
            intent.putExtra("action", action.ordinal)
            intent.putExtra("uri", uri)

            try {
                context.startForegroundService(intent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()

        thread {
            RPCS3.instance.processCompilationQueue()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val uri = intent?.getParcelableExtra<Uri>("uri")
        val action = intent?.getIntExtra("action", 0)
        val isFwInstall = action == PrecompilerServiceAction.InstallFirmware.ordinal

        if (uri == null) {
            stopSelf(startId)
            return START_NOT_STICKY
        }

        val descriptor = contentResolver.openAssetFileDescriptor(uri, "r")
        val fd = descriptor?.parcelFileDescriptor?.fd

        if (fd == null) {
            try {
                descriptor?.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            stopSelf(startId)
            return START_NOT_STICKY
        }

        val installProgress =
            ProgressRepository.create(
                this,
                if (isFwInstall) "Firmware Installation" else "Package Installation"
            ) { entry ->
                if (entry.isFinished()) {
                    if (isFwInstall) {
                        FirmwareRepository.progressChannel.value = null
                    }
                    stopSelf(startId)
                }
            }

        if (isFwInstall) {
            FirmwareRepository.progressChannel.value = installProgress
        }

        try {
            ServiceCompat.startForeground(
                this,
                progressId.toInt(),
                NotificationCompat.Builder(this, "rpcs3-progress").build(),
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

        thread {
            val installResult =
                if (isFwInstall)
                    RPCS3.instance.installFw(fd, installProgress)
                else
                    RPCS3.instance.install(fd, installProgress)

            if (!installResult) {
                try {
                    ProgressRepository.onProgressEvent(installProgress, -1, 0)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            try {
                descriptor.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        return START_STICKY
    }
}