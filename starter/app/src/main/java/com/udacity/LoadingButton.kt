package com.udacity

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.view.View
import androidx.core.content.res.ResourcesCompat
import androidx.core.content.withStyledAttributes
import kotlin.properties.Delegates

class LoadingButton @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val STROKE_WIDTH = 12f
    }

    var progress: Float = 0f
        set(value) {
            if (field == value) return

            field = when {
                progress < 0 -> 0f
                progress > 100 -> 100f
                else -> value
            }
            animateProgress()
        }

    var text: String = ""

    var buttonState: ButtonState by Delegates.observable(ButtonState.Completed) { _, _, new ->
        when (new) {
            ButtonState.Loading -> {
                //
            }
            else -> {
                progress = 0f
                animProgress = 0f
                text = attrText
                invalidate()
            }
        }
    }


    private var widthSize = 0
    private var heightSize = 0

    private var valueAnimator: ValueAnimator? = null

    private var paint = Paint().apply {
        color = ResourcesCompat.getColor(resources, R.color.white, null)
        isAntiAlias = true
        isDither = true
        strokeWidth = STROKE_WIDTH
    }

    private var attrText: String = ""
    private var attrTextColor: Int = 0
    private var attrTextLoadingColor: Int = 0
    private var attrLoadingBackgroundColor: Int = 0

    private var textBounds = Rect()
    private var animProgress = 0f

    init {
        context.withStyledAttributes(attrs, R.styleable.LoadingButton) {
            attrText = getString(R.styleable.LoadingButton_text) ?: ""
            attrTextColor = getColor(R.styleable.LoadingButton_textColor, 0)
            attrTextLoadingColor = getColor(R.styleable.LoadingButton_loadingTextColor, 0)
            attrLoadingBackgroundColor =
                getColor(R.styleable.LoadingButton_loadingBackgroundColor, 0)
            paint.textSize = getDimensionPixelSize(R.styleable.LoadingButton_textSize, 50).toFloat()

            text = attrText
        }
    }


    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)

        if (buttonState == ButtonState.Loading) {
            paint.color = attrLoadingBackgroundColor
            canvas?.drawRect(
                0f,
                0f,
                ((widthSize * (animProgress / 100f))),
                heightSize.toFloat(),
                paint
            )
        }

        paint.color =
            if (buttonState == ButtonState.Loading) attrTextLoadingColor else attrTextColor
        paint.getTextBounds(text, 0, text.length, textBounds)
        canvas?.drawText(
            text,
            (widthSize / 2 - textBounds.width() / 2).toFloat(),
            (heightSize / 2 + textBounds.height() / 2).toFloat(),
            paint
        )

        if (buttonState == ButtonState.Loading) {
            paint.color = attrTextLoadingColor
            val sweepAngle = (animProgress * 3.6).toFloat()
            canvas?.drawArc(
                (widthSize / 2 + textBounds.width() / 2 + 20).toFloat(),
                (heightSize / 2 - textBounds.height() / 2 - 25).toFloat(),
                (widthSize / 2 + textBounds.width() / 2 + 20 + textBounds.height() + 50).toFloat(),
                (heightSize / 2 + textBounds.height() / 2 + 25).toFloat(),
                0f, sweepAngle, true, paint
            )
        }


    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val minw: Int = paddingLeft + paddingRight + suggestedMinimumWidth
        val w: Int = resolveSizeAndState(minw, widthMeasureSpec, 1)
        val h: Int = resolveSizeAndState(
            MeasureSpec.getSize(w),
            heightMeasureSpec,
            0
        )
        widthSize = w
        heightSize = h
        setMeasuredDimension(w, h)
    }

    private fun animateProgress() {
        Handler(Looper.getMainLooper()).postDelayed({
            valueAnimator?.cancel()
            animProgress = 0f
        }, 100)

        val lastAnimValue = valueAnimator?.animatedValue
        valueAnimator = ValueAnimator.ofFloat((lastAnimValue ?: 0f) as Float, progress).apply {
            duration = if (progress == 100f) 200 else 500
            addUpdateListener {
                val animValue = it.animatedValue as Float
                animProgress = animValue
                postInvalidate()
            }

        }

        Handler(Looper.getMainLooper()).postDelayed({
            valueAnimator?.start()
        }, 100)

    }

}