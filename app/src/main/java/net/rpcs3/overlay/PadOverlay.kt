package net.rpcs3.overlay

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.SurfaceView
import net.rpcs3.Digital1Flags
import net.rpcs3.Digital2Flags
import net.rpcs3.R
import net.rpcs3.RPCS3
import kotlin.math.min

private const val idleAlpha = (0.3 * 255).toInt()

data class State(
    var digital1: Int = 0,
    var digital2: Int = 0,
    var leftStickX: Int = 127,
    var leftStickY: Int = 127,
    var rightStickX: Int = 127,
    var rightStickY: Int = 127
)

class PadOverlay(context: Context?, attrs: AttributeSet?) : SurfaceView(context, attrs) {
    private val buttons: Array<PadOverlayButton>
    private val dpad: PadOverlayDpad
    private val state = State()
    private val leftStick: PadOverlayStick
    private val rightStick: PadOverlayStick
    private val sticks = arrayOf<PadOverlayStick?>(null, null)

    init {
        val metrics = context!!.resources.displayMetrics
        val totalWidth = metrics.widthPixels
        val totalHeight = metrics.heightPixels
        val sizeHint = min(totalHeight, totalWidth)
        val buttonSize = sizeHint / 8

        val btnAreaW = buttonSize * 3
        val btnAreaH = buttonSize * 3

        val btnAreaX = totalWidth - btnAreaW - buttonSize
        val btnAreaY = totalHeight - btnAreaH - buttonSize / 2
        val btnDistance = buttonSize / 8

        val btnCircleX = btnAreaX + btnAreaW - buttonSize - btnDistance
        val btnCircleY = btnAreaY + btnAreaH / 2 - buttonSize / 2

        val btnTriangleX = btnAreaX + btnAreaW / 2 - buttonSize / 2
        val btnTriangleY = btnAreaY + btnDistance

        val btnSquareX = btnAreaX + btnDistance
        val btnSquareY = btnAreaY + btnAreaH / 2 - buttonSize / 2

        val btnCrossX = btnAreaX + btnAreaW / 2 - buttonSize / 2
        val btnCrossY = btnAreaY + btnAreaH - buttonSize - btnDistance

        val dpadW = buttonSize * 3 - btnDistance / 2
        val dpadH = buttonSize * 3 - btnDistance / 2

        val dpadAreaX = buttonSize
        val dpadAreaY = btnAreaY

        val startSelectSize = (buttonSize * 1.5).toInt()
        val btnStartX = totalWidth / 2 + buttonSize * 2
        val btnStartY = buttonSize / 2
        val btnSelectX = totalWidth / 2 - startSelectSize - buttonSize * 2
        val btnSelectY = btnStartY

        val btnL2X = buttonSize
        val btnL2Y = buttonSize

        val btnL1X = btnL2X
        val btnL1Y = btnL2Y + buttonSize + buttonSize / 2

        val btnR2X = totalWidth - buttonSize * 2
        val btnR2Y = btnL2Y

        val btnR1X = btnR2X
        val btnR1Y = btnR2Y + buttonSize + buttonSize / 2

        dpad = createDpad(dpadAreaX, dpadAreaY, dpadW, dpadH)

        leftStick = PadOverlayStick(
            resources,
            true,
            BitmapFactory.decodeResource(resources, R.drawable.left_stick_background),
            BitmapFactory.decodeResource(resources, R.drawable.left_stick)
        )
        rightStick = PadOverlayStick(
            resources,
            false,
            BitmapFactory.decodeResource(resources, R.drawable.right_stick_background),
            BitmapFactory.decodeResource(resources, R.drawable.right_stick)
        )

        leftStick.setBounds(0, 0, buttonSize * 2, buttonSize * 2)
        leftStick.alpha = idleAlpha
        rightStick.setBounds(0, 0, buttonSize * 2, buttonSize * 2)
        rightStick.alpha = idleAlpha

        buttons = arrayOf(
            createButton(
                R.drawable.circle,
                btnCircleX,
                btnCircleY,
                buttonSize,
                buttonSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_CIRCLE
            ),
            createButton(
                R.drawable.triangle,
                btnTriangleX,
                btnTriangleY,
                buttonSize,
                buttonSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_TRIANGLE
            ),
            createButton(
                R.drawable.square,
                btnSquareX,
                btnSquareY,
                buttonSize,
                buttonSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_SQUARE
            ),
            createButton(
                R.drawable.cross,
                btnCrossX,
                btnCrossY,
                buttonSize,
                buttonSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_CROSS
            ),
            createButton(
                R.drawable.start,
                btnStartX,
                btnStartY,
                startSelectSize,
                startSelectSize,
                Digital1Flags.CELL_PAD_CTRL_START,
                Digital2Flags.None
            ),
            createButton(
                R.drawable.select,
                btnSelectX,
                btnSelectY,
                startSelectSize,
                startSelectSize,
                Digital1Flags.CELL_PAD_CTRL_SELECT,
                Digital2Flags.None
            ),

            createButton(
                R.drawable.l1,
                btnL1X,
                btnL1Y,
                startSelectSize,
                startSelectSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_L1
            ),
            createButton(
                R.drawable.l2,
                btnL2X,
                btnL2Y,
                startSelectSize,
                startSelectSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_L2
            ),
            createButton(
                R.drawable.r1,
                btnR1X,
                btnR1Y,
                startSelectSize,
                startSelectSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_R1
            ),
            createButton(
                R.drawable.r2,
                btnR2X,
                btnR2Y,
                startSelectSize,
                startSelectSize,
                Digital1Flags.None,
                Digital2Flags.CELL_PAD_CTRL_R2
            ),
        )

        setWillNotDraw(false)
        requestFocus()

        setOnTouchListener { _, motionEvent ->
            var hit = false

            val action = motionEvent.actionMasked
            val pointerIndex =
                if (action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) motionEvent.actionIndex else 0
            val x = motionEvent.getX(pointerIndex).toInt()
            val y = motionEvent.getY(pointerIndex).toInt()
            val force = action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP
            if (force || (motionEvent.action == MotionEvent.ACTION_MOVE || dpad.contains(
                    x,
                    y
                ))
            ) {
                hit = dpad.onTouch(motionEvent, pointerIndex, state)
            }

            buttons.forEach { button ->
                if (force || (!hit && button.contains(x, y))) {
                    hit = button.onTouch(motionEvent, pointerIndex, state)
                }
            }

            for (i in sticks.indices) {
                val stick = sticks[i] ?: continue
                val touchResult = stick.onTouch(motionEvent, pointerIndex, state)
                if (touchResult < 0) {
                    sticks[i] = null
                    hit = true
                } else {
                    hit = touchResult == 1
                }
            }

            RPCS3.instance.overlayPadData(
                state.digital1,
                state.digital2,
                state.leftStickX,
                state.leftStickY,
                state.rightStickX,
                state.rightStickY
            )

            if (!hit && (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN) && (y > buttonSize && x > buttonSize * 3)) {
                val stickIndex = if (x <= totalWidth / 2) 0 else 1
                val stick = if (stickIndex == 0) leftStick else rightStick

                if (sticks[stickIndex] == null) {
                    sticks[stickIndex] = stick
                    stick.onAdd(motionEvent, pointerIndex, state)
                    hit = true
                }
            }

            if (hit || force) {
                invalidate()
            }

            hit || performClick()
        }
    }

    override fun draw(canvas: Canvas) {
        super.draw(canvas)
        buttons.forEach { button -> button.draw(canvas) }
        dpad.draw(canvas)
        sticks.forEach { it?.draw(canvas) }
    }

    private fun createButton(
        resourceId: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        digital1: Digital1Flags,
        digital2: Digital2Flags
    ): PadOverlayButton {
        val resources = context!!.resources
        val bitmap = BitmapFactory.decodeResource(resources, resourceId)
        val result = PadOverlayButton(resources, bitmap, digital1.bit, digital2.bit)
        result.setBounds(x, y, x + width, y + height)
        result.alpha = idleAlpha
        return result
    }

    private fun createDpad(x: Int, y: Int, width: Int, height: Int): PadOverlayDpad {
        val resources = context!!.resources
        val idleBitmap = BitmapFactory.decodeResource(resources, R.drawable.dpad_idle)
        val topBitmap = BitmapFactory.decodeResource(resources, R.drawable.dpad)
        val topLeftBitmap = BitmapFactory.decodeResource(resources, R.drawable.dpad_top_left)

        val result = PadOverlayDpad(resources, idleBitmap, topBitmap, topLeftBitmap)
        result.setBounds(x, y, x + width, y + height)
        result.alpha = idleAlpha
        return result
    }
}