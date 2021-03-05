package com.colvir.core.ui

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.View
import java.util.*

class Throbber
@JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, defStyleRes: Int = 0)
    : View(context, attrs, defStyleAttr, defStyleRes) {

    private var minHeight = 0f
    private var minWidth = 0f
    private var elementWidth = 0f
    private var centerX = 0f
    private var centerY = 0f

    private var spaceBetweenElements = 0f
    private var radius1 = 0f
    private var radius2 = 0f
    private var radius3 = 0f

    private val rotatePerSecond1 = 0.5f
    private val rotatePerSecond2 = 0.7f
    private val rotatePerSecond3 = 0.9f

    private var timer: Timer? = null
    private val fps = 60
    private val timerTickPeriodInMilliseconds = 1000L / fps

    private val rotatePerTick1 = 360f / fps * rotatePerSecond1
    private val rotatePerTick2 = 360f / fps * rotatePerSecond2
    private val rotatePerTick3 = 360f / fps * rotatePerSecond3

    private var currentDegree1 = 0f
    private var currentDegree2 = 120f
    private var currentDegree3 = 240f

    private val colorsPoz = floatArrayOf(0f, 0.5f, 1f)

    private val mPaint = Paint()

    init {
        mPaint.setStyle(Paint.Style.STROKE)
        mPaint.setDither(true)
        mPaint.setAntiAlias(true)

    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()

        stopTimer()
    }

    override fun onVisibilityChanged(changedView: View, visibility: Int) {
        super.onVisibilityChanged(changedView, visibility)

        if (visibility == VISIBLE) {
            startTimer()
        } else {
            stopTimer()
        }
    }

    private fun stopTimer() {
        timer ?: return

        timer?.cancel()
        timer = null
    }

    private fun startTimer() {
        if (timer != null || isInEditMode) {
            return
        }

        timer = Timer().apply {
            scheduleAtFixedRate(object : TimerTask() {
                override fun run() {
                    currentDegree1 += rotatePerTick1
                    if (currentDegree1 >= 360) {
                        currentDegree1 -= 360
                    }

                    currentDegree2 += rotatePerTick2
                    if (currentDegree2 >= 360) {
                        currentDegree2 -= 360
                    }

                    currentDegree3 += rotatePerTick3
                    if (currentDegree3 >= 360) {
                        currentDegree3 -= 360
                    }
                    post {
                        invalidate()
                    }
                }
            }, 0L, timerTickPeriodInMilliseconds)
        }
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        minHeight = kotlin.math.min(w, h).toFloat()
        minWidth = minHeight

        elementWidth = minWidth / 18f
        centerX = minWidth / 2f
        centerY = minHeight / 2f


        spaceBetweenElements = elementWidth / 1.5f
        radius1 = minHeight / 2f - elementWidth / 2f
        radius2 = radius1 - elementWidth - spaceBetweenElements
        radius3 = radius2 - elementWidth - spaceBetweenElements

        mPaint.strokeWidth = elementWidth
        mPaint.shader = SweepGradient(centerX, centerY, colors, colorsPoz)

    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)

        canvas ?: return

        drawArc(canvas, radius1, currentDegree1)
        drawArc(canvas, radius2, currentDegree2)
        drawArc(canvas, radius3, currentDegree3)
    }

    private fun drawArc(canvas: Canvas, radius: Float, rotateDegree: Float) {
        val oval = RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius)

        canvas.save()
        canvas.rotate(rotateDegree, centerX, centerY)
        canvas.drawArc(oval, 0f, 180f, false, mPaint)
        canvas.restore()

    }

}