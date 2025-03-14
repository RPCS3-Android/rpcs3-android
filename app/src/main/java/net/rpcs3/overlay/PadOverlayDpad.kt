package net.rpcs3.overlay

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.view.MotionEvent
import net.rpcs3.Digital1Flags

enum class DpadState {
    Idle,
    Top,
    Left,
    Right,
    Bottom,
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
}

class PadOverlayDpad(resources: Resources, imgIdle: Bitmap, imgTop: Bitmap, imgTopLeft: Bitmap) {
    private val drawableIdle = BitmapDrawable(resources, imgIdle)
    private val drawableTop = BitmapDrawable(resources, imgTop)
    private val drawableTopLeft = BitmapDrawable(resources, imgTopLeft)
    private var state = DpadState.Idle
    private var locked = -1

    fun contains(x: Int, y: Int) = drawableIdle.bounds.contains(x, y)

    fun onTouch(event: MotionEvent, pointerIndex: Int, padState: State): Boolean {
        val action = event.actionMasked
        var hit = false

        var activePointerIndex = pointerIndex

        if (locked != -1) {
            activePointerIndex = -1
            for (i in 0..<event.pointerCount) {
                if (locked == event.getPointerId(i)) {
                    activePointerIndex = i
                    break
                }
            }

            if (activePointerIndex == -1) {
                return false
            }
        }

        if (action == MotionEvent.ACTION_DOWN || action == MotionEvent.ACTION_POINTER_DOWN || (action == MotionEvent.ACTION_MOVE && locked != -1)) {
            if (locked == -1) {
                locked = event.getPointerId(pointerIndex)
            }

            hit = true

            val x = event.getX(activePointerIndex)
            val y = event.getY(activePointerIndex)

            val leftDistance = x - drawableIdle.bounds.left
            val topDistance = y - drawableIdle.bounds.top
            val bottomDistance = drawableIdle.bounds.bottom - y
            val rightDistance = drawableIdle.bounds.right - x
            val distanceWidth = drawableIdle.bounds.width() / 2.7

            val left = leftDistance < distanceWidth
            val right = rightDistance < distanceWidth
            val top = topDistance < distanceWidth
            val bottom = bottomDistance < distanceWidth

            padState.digital1 =
                padState.digital1 and (Digital1Flags.CELL_PAD_CTRL_LEFT.bit or Digital1Flags.CELL_PAD_CTRL_UP.bit or Digital1Flags.CELL_PAD_CTRL_DOWN.bit or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit).inv()

            if (top && left) {
                state = DpadState.TopLeft
                padState.digital1 =
                    padState.digital1 or Digital1Flags.CELL_PAD_CTRL_LEFT.bit or Digital1Flags.CELL_PAD_CTRL_UP.bit
            } else if (top && right) {
                state = DpadState.TopRight
                padState.digital1 =
                    padState.digital1 or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit or Digital1Flags.CELL_PAD_CTRL_UP.bit
            } else if (bottom && left) {
                state = DpadState.BottomLeft
                padState.digital1 =
                    padState.digital1 or Digital1Flags.CELL_PAD_CTRL_LEFT.bit or Digital1Flags.CELL_PAD_CTRL_DOWN.bit
            } else if (bottom && right) {
                state = DpadState.BottomRight
                padState.digital1 =
                    padState.digital1 or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit or Digital1Flags.CELL_PAD_CTRL_DOWN.bit
            } else if (top) {
                state = DpadState.Top
                padState.digital1 = padState.digital1 or Digital1Flags.CELL_PAD_CTRL_UP.bit
            } else if (left) {
                state = DpadState.Left
                padState.digital1 = padState.digital1 or Digital1Flags.CELL_PAD_CTRL_LEFT.bit
            } else if (right) {
                state = DpadState.Right
                padState.digital1 = padState.digital1 or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit
            } else if (bottom) {
                state = DpadState.Bottom
                padState.digital1 = padState.digital1 or Digital1Flags.CELL_PAD_CTRL_DOWN.bit
            } else {
                state = DpadState.Idle
            }
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_POINTER_UP) {
            if (event.getPointerId(pointerIndex) == locked) {
                hit = true
                padState.digital1 =
                    padState.digital1 and (Digital1Flags.CELL_PAD_CTRL_LEFT.bit or Digital1Flags.CELL_PAD_CTRL_UP.bit or Digital1Flags.CELL_PAD_CTRL_DOWN.bit or Digital1Flags.CELL_PAD_CTRL_RIGHT.bit).inv()

                state = DpadState.Idle
                locked = -1
            }
        }

        return hit
    }

    fun setBounds(left: Int, top: Int, right: Int, bottom: Int) {
        drawableIdle.setBounds(left, top, right, bottom)
        drawableTop.setBounds(left, top, right, bottom)
        drawableTopLeft.setBounds(left, top, right, bottom)
    }

    var alpha: Int
        get() {
            return drawableIdle.alpha
        }
        set(value) {
            drawableIdle.alpha = value
            drawableTop.alpha = value
            drawableTopLeft.alpha = value
        }


    fun draw(canvas: Canvas) {
        val x = drawableIdle.bounds.centerX()
        val y = drawableIdle.bounds.centerY()
        when (state) {
            DpadState.Idle -> drawableIdle.draw(canvas)
            DpadState.Top -> drawableTop.draw(canvas)
            DpadState.Left -> {
                canvas.save()
                canvas.rotate(270f, x.toFloat(), y.toFloat())
                drawableTop.draw(canvas)
                canvas.restore()
            }

            DpadState.Right -> {
                canvas.save()
                canvas.rotate(90f, x.toFloat(), y.toFloat())
                drawableTop.draw(canvas)
                canvas.restore()
            }

            DpadState.Bottom -> {
                canvas.save()
                canvas.rotate(180f, x.toFloat(), y.toFloat())
                drawableTop.draw(canvas)
                canvas.restore()
            }

            DpadState.TopLeft -> drawableTopLeft.draw(canvas)
            DpadState.TopRight -> {
                canvas.save()
                canvas.rotate(90f, x.toFloat(), y.toFloat())
                drawableTopLeft.draw(canvas)
                canvas.restore()
            }

            DpadState.BottomLeft -> {
                canvas.save()
                canvas.rotate(270f, x.toFloat(), y.toFloat())
                drawableTopLeft.draw(canvas)
                canvas.restore()
            }

            DpadState.BottomRight -> {
                canvas.save()
                canvas.rotate(180f, x.toFloat(), y.toFloat())
                drawableTopLeft.draw(canvas)
                canvas.restore()
            }
        }
    }
}
