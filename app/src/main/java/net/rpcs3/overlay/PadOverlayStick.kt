package net.rpcs3.overlay

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

class PadOverlayStick(resources: Resources, val isLeft: Boolean, bg: Bitmap, stick: Bitmap) :
    BitmapDrawable(resources, stick) {
    private var bg = BitmapDrawable(resources, bg)
    private var locked = -1
    fun contains(x: Int, y: Int) = bounds.contains(x, y)

    fun onAdd(event: MotionEvent, pointerIndex: Int, padState: State) {
        locked = event.getPointerId(pointerIndex)
        val x = event.getX(pointerIndex).toInt()
        val y = event.getY(pointerIndex).toInt()

        setBounds(
            x - bounds.width() / 2,
            y - bounds.height() / 2,
            x + bounds.width() / 2,
            y + bounds.height() / 2,
        )
    }
    fun onTouch(event: MotionEvent, pointerIndex: Int, padState: State): Int {
        val action = event.actionMasked

        if (action == MotionEvent.ACTION_MOVE) {
            var activePointerIndex = -1

            for (i in 0..<event.pointerCount) {
                if (locked == event.getPointerId(i)) {
                    activePointerIndex = i
                    break
                }
            }

            if (activePointerIndex == -1) {
                return 0
            }

            val bgCenterX = bg.bounds.centerX()
            val bgCenterY = bg.bounds.centerY()

            var x = event.getX(activePointerIndex)
            var y = event.getY(activePointerIndex)

            x -= bgCenterX
            y -= bgCenterY

            val bgR = hypot((bg.bounds.left - bgCenterX).toFloat(), (bg.bounds.top - bgCenterY).toFloat())
            val stickR = hypot(x, y)

            if (stickR > bgR) {
                val L = atan2(y, x)
                x = bgR * cos(L)
                y = bgR * sin(L)
            }

            val stickX = ((x / bgR) * 128 + 127).toInt()
            val stickY = ((y / bgR) * 128 + 127).toInt()

            if (isLeft) {
                padState.leftStickX = stickX
                padState.leftStickY = stickY
            } else {
                padState.rightStickX = stickX
                padState.rightStickY = stickY
            }

            x += bgCenterX
            y += bgCenterY

            super.setBounds(
                x.toInt() - bounds.width() / 2,
                y.toInt() - bounds.height() / 2,
                x.toInt() + bounds.width() / 2,
                y.toInt() + bounds.height() / 2,
            )

            return 1
        }

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (event.getPointerId(pointerIndex) == locked) {
                locked = -1

                if (isLeft) {
                    padState.leftStickX = 127
                    padState.leftStickY = 127
                } else {
                    padState.rightStickX = 127
                    padState.rightStickY = 127
                }
                return -1
            }
        }

        return 0
    }

    override fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        super.setBounds(left, top, right, bottom)
        bg.setBounds(left, top, right, bottom)
    }

    override fun setAlpha(alpha: Int) {
        super.setAlpha(alpha)
        bg.alpha = alpha
    }

    override fun draw(canvas: Canvas) {
        bg.draw(canvas)
        super.draw(canvas)
    }
}
