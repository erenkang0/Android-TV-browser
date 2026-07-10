package com.erenkang.tvbrowser.cursor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PointF
import android.os.SystemClock
import android.util.AttributeSet
import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.widget.FrameLayout
import kotlin.math.abs
import kotlin.math.min

/**
 * Overlay layout that renders a virtual pointer controlled by the D-pad.
 * Direction keys move the pointer with acceleration, DPAD_CENTER injects
 * real touch events into [targetView] at the pointer position, and pushing
 * against a screen edge scrolls the page underneath.
 */
class CursorLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    /** The view (current tab's WebView) that receives clicks and scrolling. */
    var targetView: View? = null

    /** Called when the user pushes UP while the cursor is at the top edge and the page is at scroll 0. */
    var onEdgeBumpTop: (() -> Unit)? = null

    private val density = resources.displayMetrics.density
    private val cursorRadius = 11f * density
    private val baseSpeed = 360f * density   // px/s
    private val maxSpeed = 2200f * density   // px/s
    private val accel = 1600f * density      // px/s^2

    private val position = PointF()
    private val pressedDirs = HashSet<Int>()
    private var moveStartTime = 0L
    private var lastFrameTime = 0L
    private var centerPressed = false
    private var touchDownTime = 0L
    private var edgeBumpFired = false
    private var positionInitialized = false

    private var cursorVisible = false
    private val hideRunnable = Runnable {
        cursorVisible = false
        invalidate()
    }

    private val shadowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(70, 0, 0, 0)
    }
    private val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(200, 255, 255, 255)
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f * density
        color = Color.argb(220, 30, 40, 55)
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            val now = SystemClock.uptimeMillis()
            val dt = min(now - lastFrameTime, 48L) / 1000f
            lastFrameTime = now
            step(dt)
            if (pressedDirs.isNotEmpty()) {
                postOnAnimation(this)
            }
        }
    }

    init {
        setWillNotDraw(false)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (!positionInitialized && w > 0 && h > 0) {
            position.set(w / 2f, h / 2f)
            positionInitialized = true
        } else {
            position.x = position.x.coerceIn(0f, w.toFloat())
            position.y = position.y.coerceIn(0f, h.toFloat())
        }
    }

    fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                showCursor()
                if (event.repeatCount == 0) {
                    if (pressedDirs.isEmpty()) {
                        moveStartTime = SystemClock.uptimeMillis()
                        lastFrameTime = moveStartTime
                        postOnAnimation(frameRunnable)
                    }
                    pressedDirs.add(keyCode)
                    edgeBumpFired = false
                }
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (event.repeatCount == 0) {
                    showCursor()
                    centerPressed = true
                    touchDownTime = SystemClock.uptimeMillis()
                    dispatchMotion(MotionEvent.ACTION_DOWN)
                }
                return true
            }
        }
        return false
    }

    fun handleKeyUp(keyCode: Int): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                pressedDirs.remove(keyCode)
                return true
            }
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_BUTTON_A -> {
                if (centerPressed) {
                    dispatchMotion(MotionEvent.ACTION_UP)
                    centerPressed = false
                }
                return true
            }
        }
        return false
    }

    fun releaseAllKeys() {
        pressedDirs.clear()
        if (centerPressed) {
            dispatchMotion(MotionEvent.ACTION_CANCEL)
            centerPressed = false
        }
    }

    fun centerCursor() {
        if (width > 0 && height > 0) {
            position.set(width / 2f, height / 2f)
            invalidate()
        }
    }

    private fun step(dt: Float) {
        val held = (SystemClock.uptimeMillis() - moveStartTime) / 1000f
        val speed = min(maxSpeed, baseSpeed + accel * held)
        var dx = 0f
        var dy = 0f
        if (KeyEvent.KEYCODE_DPAD_LEFT in pressedDirs) dx -= speed * dt
        if (KeyEvent.KEYCODE_DPAD_RIGHT in pressedDirs) dx += speed * dt
        if (KeyEvent.KEYCODE_DPAD_UP in pressedDirs) dy -= speed * dt
        if (KeyEvent.KEYCODE_DPAD_DOWN in pressedDirs) dy += speed * dt
        if (dx == 0f && dy == 0f) return

        val newX = (position.x + dx).coerceIn(0f, width.toFloat())
        val newY = (position.y + dy).coerceIn(0f, height.toFloat())

        // Pushing against an edge scrolls the page in that direction.
        val target = targetView
        if (target != null) {
            var scrollX = 0
            var scrollY = 0
            if (dx < 0 && newX <= 0f) scrollX = dx.toInt()
            if (dx > 0 && newX >= width.toFloat()) scrollX = dx.toInt()
            if (dy < 0 && newY <= 0f) scrollY = dy.toInt()
            if (dy > 0 && newY >= height.toFloat()) scrollY = dy.toInt()
            if (scrollX != 0 || scrollY != 0) {
                if (scrollY < 0 && target.scrollY <= 0) {
                    if (!edgeBumpFired) {
                        edgeBumpFired = true
                        onEdgeBumpTop?.invoke()
                    }
                } else {
                    target.scrollBy(scrollX, scrollY)
                }
            }
        }

        val moved = abs(newX - position.x) > 0.1f || abs(newY - position.y) > 0.1f
        position.set(newX, newY)
        if (centerPressed && moved) {
            dispatchMotion(MotionEvent.ACTION_MOVE)
        }
        showCursor()
        invalidate()
    }

    private fun dispatchMotion(action: Int) {
        val target = targetView ?: return
        val now = SystemClock.uptimeMillis()
        val event = MotionEvent.obtain(touchDownTime, now, action, position.x, position.y, 0)
        event.source = InputDevice.SOURCE_TOUCHSCREEN
        target.dispatchTouchEvent(event)
        event.recycle()
    }

    private fun showCursor() {
        if (!cursorVisible) {
            cursorVisible = true
            invalidate()
        }
        removeCallbacks(hideRunnable)
        postDelayed(hideRunnable, HIDE_DELAY_MS)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!cursorVisible) return
        val r = if (centerPressed) cursorRadius * 0.85f else cursorRadius
        canvas.drawCircle(position.x, position.y + 2f * density, r + 2f * density, shadowPaint)
        canvas.drawCircle(position.x, position.y, r, fillPaint)
        canvas.drawCircle(position.x, position.y, r, strokePaint)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(hideRunnable)
        removeCallbacks(frameRunnable)
    }

    companion object {
        private const val HIDE_DELAY_MS = 3500L
    }
}
