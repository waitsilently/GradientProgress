package com.example.myapplication

import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.SweepGradient
import android.util.AttributeSet
import android.view.View
import android.view.animation.Interpolator
import android.view.animation.LinearInterpolator
import kotlin.math.max
import kotlin.math.min

/**
 * 支持渐变的圆形进度条
 *
 * @author zengbowen
 */
class GradientCircleProgressView(context: Context, attrs: AttributeSet?): View(context, attrs) {
    /**
     * 当前进度
     */
    var progress = 0f
        set(value) {
            field = value
            progressChangeCallback?.invoke(field)
            invalidate()
        }

    /**
     * 进度宽度
     */
    var progressWidth = 50f
        set(value) {
            field = value
            backgroundPaint.strokeWidth = field - 2
            progressPaint.strokeWidth = field
            gradientPaint.strokeWidth = field
            invalidate()
        }

    /**
     * 开始角度
     */
    var startAngel = 270f
        set(value) {
            field = value
            invalidate()
        }

    /**
     * 进度颜色
     */
    var progressColor = Color.BLUE
        set(value) {
            field = value
            progressPaint.color = field
            invalidate()
        }

    /**
     * 进度的背景色
     */
    var progressBackgroundColor = Color.WHITE
        set(value) {
            field = value
            backgroundPaint.color = field
            invalidate()
        }

    /**
     * 动画持续时间
     */
    var progressAnimDuration = 1500L

    /**
     * 动画插值器
     */
    var progressAnimInterpolator: Interpolator = LinearInterpolator()

    /**
     * 进度回调
     */
    var progressChangeCallback: ((progress: Float) -> Unit)? = null

    /**
     * 背景画笔
     */
    private val backgroundPaint = Paint()

    /**
     * 进度画笔
     */
    private val progressPaint = Paint()

    /**
     * 渐变色画笔
     */
    private var gradientPaint = Paint()

    /**
     * 一圈的角度
     */
    private val fullAngel = 360f

    /**
     * 一圈的进度
     */
    private val fullProgress = 100

    /**
     * 渐变角度
     */
    private val gradientAngel = 60

    /**
     * 渐变开始色
     */
    private var gradientStartColor = Color.BLUE

    /**
     * 渐变结束色
     */
    private var gradientEndColor = Color.GREEN


    /**
     * Sweep 渐变
     */
    private var shader: SweepGradient? = null

    /**
     * 渐变矩阵
     */
    private val gradientMatrix = Matrix()

    /**
     * 绘制弧度基于的长方形
     */
    private var oval: RectF? = null

    /**
     * 渐变色数组
     */
    private var gradientColors = intArrayOf(
        gradientStartColor,
        gradientEndColor,
        gradientEndColor,
        gradientEndColor,
        gradientStartColor,
        gradientStartColor,
        gradientStartColor
    )

    init {
        backgroundPaint.style = Paint.Style.STROKE
        backgroundPaint.color = progressBackgroundColor
        backgroundPaint.strokeWidth = progressWidth - 2
        backgroundPaint.isAntiAlias = true

        progressPaint.style = Paint.Style.STROKE
        progressPaint.color = progressColor
        progressPaint.strokeWidth = progressWidth
        progressPaint.strokeCap = Paint.Cap.ROUND
        progressPaint.isAntiAlias = true

        gradientPaint.style = Paint.Style.STROKE
        gradientPaint.strokeWidth = progressWidth
        gradientPaint.strokeCap = Paint.Cap.ROUND
        gradientPaint.isAntiAlias = true

        attrs?.let {
            initFromAttrs(context, it)
        }
    }

    /**
     * 根据属性进行初始化
     */
    private fun initFromAttrs(context: Context, attrs: AttributeSet) {
        val typedArray = context.obtainStyledAttributes(attrs, R.styleable.GradientCircleProgressView)
        progressColor = typedArray.getColor(R.styleable.GradientCircleProgressView_progressColor, Color.BLUE)
        progressBackgroundColor = typedArray.getColor(R.styleable.GradientCircleProgressView_progressBackgroundColor, Color.WHITE)
        progressWidth = typedArray.getDimension(R.styleable.GradientCircleProgressView_progressWidth, 50f)
        startAngel = typedArray.getFloat(R.styleable.GradientCircleProgressView_startAngel, 270f)
        val gradientStartColor = typedArray.getColor(R.styleable.GradientCircleProgressView_gradientStartColor, Color.BLUE)
        val gradientEndColor = typedArray.getColor(R.styleable.GradientCircleProgressView_gradientEndColor, Color.GREEN)
        updateGradientColor(gradientStartColor, gradientEndColor)
        typedArray.recycle()
    }

    /**
     * 更新进度
     * [progress] [0..1]
     */
    fun setProgress(progress: Float, animate: Boolean) {
        if (!animate) {
            this.progress = progress
            return
        }
        val animator = ObjectAnimator.ofFloat(this, "progress", this.progress, progress)
        animator.setAutoCancel(true)
        animator.duration = progressAnimDuration
        animator.interpolator = progressAnimInterpolator
        animator.start()
    }

    /**
     * 更新渐变色
     */
    fun updateGradientColor(gradientStartColor: Int, gradientEndColor: Int) {
        this.gradientStartColor = gradientStartColor
        this.gradientEndColor = gradientEndColor
        gradientColors = intArrayOf(
            gradientStartColor,
            gradientEndColor,
            gradientEndColor,
            gradientEndColor,
            gradientStartColor,
            gradientStartColor,
            gradientStartColor
        )
        shader = null
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        val radiosX = centerX - progressWidth / 2f
        val radiosY = centerY - progressWidth / 2f
        if (oval == null) {
            oval = RectF(centerX - radiosX, centerY - radiosY, centerX + radiosX, centerY + radiosY)
        }
        if (shader == null) {
            shader = SweepGradient(centerX, centerY, gradientColors, null)
            gradientPaint.shader = shader
        }
        // 绘制背景
        canvas.drawArc(oval!!, startAngel, fullAngel, false, backgroundPaint)
        // 绘制进度
        val sweepAngle = progress / fullProgress * fullAngel
        canvas.drawArc(oval!!, startAngel, sweepAngle, false, progressPaint)
        // 绘制渐变区域
        drawGradient(canvas, sweepAngle)
    }

    /**
     * 绘制进度末尾的渐变区域
     */
    private fun drawGradient(canvas: Canvas, sweepAngle: Float) {
        // 渐变区域的开始角度
        val gradientStartAngel = if (sweepAngle <= gradientAngel) {
            startAngel
        } else {
            startAngel + sweepAngle - gradientAngel
        }
        // 渐变区域需要旋转的角度
        val offsetAngel = startAngel + sweepAngle
        if (offsetAngel > gradientAngel) {
            gradientMatrix.setRotate(offsetAngel - gradientAngel, width / 2f, height / 2f)
            shader?.setLocalMatrix(gradientMatrix)
        }
        // 绘制渐变区域
        canvas.drawArc(
            oval!!,
            max(gradientStartAngel, startAngel),
            min(sweepAngle, gradientAngel.toFloat()),
            false,
            gradientPaint
        )
    }
}
