package app.omglauncher.ui

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.view.animation.LinearInterpolator
import app.omglauncher.helper.dpToPx

enum class TimerDisplayMode { TIMER, SPINNER, CHECKMARK }

class BeeminderTimerView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : View(context, attrs, defStyleAttr) {

    private val ringBounds = RectF()
    private val textBounds = Rect()

    private val trackPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        alpha = 45
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7.dpToPx().toFloat()
    }

    private val progressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 7.dpToPx().toFloat()
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        textAlign = Paint.Align.CENTER
        textSize = 14.dpToPx().toFloat()
    }

    private val spinnerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.BLACK
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeWidth = 5.dpToPx().toFloat()
    }

    private val checkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#2E7D32")
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
        strokeWidth = 6.dpToPx().toFloat()
    }

    private val checkPath = Path()
    private val spinnerBounds = RectF()
    private var spinnerAngle = 0f

    private val spinnerAnimator = ValueAnimator.ofFloat(0f, 360f).apply {
        duration = 1000L
        repeatCount = ValueAnimator.INFINITE
        interpolator = LinearInterpolator()
        addUpdateListener {
            spinnerAngle = it.animatedValue as Float
            invalidate()
        }
    }

    var displayMode: TimerDisplayMode = TimerDisplayMode.TIMER
        set(value) {
            if (field == value) return
            field = value
            if (value == TimerDisplayMode.SPINNER) spinnerAnimator.start()
            else spinnerAnimator.cancel()
            invalidate()
        }

    var progress: Float = 1f
        set(value) {
            field = value.coerceIn(0f, 1f)
            invalidate()
        }

    var timeText: String = ""
        set(value) {
            field = value
            invalidate()
        }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val defaultSize = (resources.displayMetrics.widthPixels * 0.45f).toInt()
        val measuredWidth = resolveSize(defaultSize, widthMeasureSpec)
        val measuredHeight = resolveSize(defaultSize, heightMeasureSpec)
        val size = minOf(measuredWidth, measuredHeight)
        setMeasuredDimension(size, size)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val inset = progressPaint.strokeWidth / 2f
        ringBounds.set(inset, inset, width - inset, height - inset)
        canvas.drawOval(ringBounds, trackPaint)

        when (displayMode) {
            TimerDisplayMode.TIMER -> {
                canvas.drawArc(ringBounds, -90f, 360f * progress, false, progressPaint)
                if (timeText.isNotEmpty()) {
                    fitTimeText()
                    textPaint.getTextBounds(timeText, 0, timeText.length, textBounds)
                    val centerY = height / 2f - textBounds.exactCenterY()
                    canvas.drawText(timeText, width / 2f, centerY, textPaint)
                }
            }
            TimerDisplayMode.SPINNER -> drawSpinner(canvas)
            TimerDisplayMode.CHECKMARK -> drawCheckmark(canvas)
        }
    }

    private fun drawSpinner(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val radius = minOf(width, height) * 0.2f
        spinnerBounds.set(cx - radius, cy - radius, cx + radius, cy + radius)
        canvas.drawArc(spinnerBounds, spinnerAngle, 270f, false, spinnerPaint)
    }

    private fun drawCheckmark(canvas: Canvas) {
        val cx = width / 2f
        val cy = height / 2f
        val size = minOf(width, height) * 0.2f

        checkPath.reset()
        checkPath.moveTo(cx - size, cy)
        checkPath.lineTo(cx - size * 0.3f, cy + size * 0.7f)
        checkPath.lineTo(cx + size, cy - size * 0.6f)

        canvas.drawPath(checkPath, checkPaint)
    }

    private fun fitTimeText() {
        val horizontalPadding = progressPaint.strokeWidth * 4f
        val availableWidth = (width - horizontalPadding).coerceAtLeast(1f)
        val availableHeight = (height - horizontalPadding).coerceAtLeast(1f)
        val minimumSize = 8.dpToPx().toFloat()
        var size = minOf(width * 0.22f, 64.dpToPx().toFloat()).coerceAtLeast(minimumSize)

        do {
            textPaint.textSize = size
            textPaint.getTextBounds(timeText, 0, timeText.length, textBounds)
            if (textPaint.measureText(timeText) <= availableWidth && textBounds.height() <= availableHeight) return
            size -= 1.dpToPx()
        } while (size > minimumSize)

        textPaint.textSize = minimumSize
    }

    fun setColors(color: Int) {
        trackPaint.color = color
        trackPaint.alpha = 45
        progressPaint.color = color
        textPaint.color = color
        spinnerPaint.color = color
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        spinnerAnimator.cancel()
    }
}
