package com.example.personaz

import android.content.Context
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.ImageView

class PersonazView(context: Context, private val windowManager: WindowManager) :
    ImageView(context) {

    companion object {
        private const val ANIMATION_INTERVAL = 300L
        private const val SIZE_DEFAULT = 300
        private const val CLICK_DRAG_THRESHOLD = 20f
    }

    private lateinit var walkFrame1: Drawable
    private lateinit var walkFrame2: Drawable
    private var walkCurrentFrame = 0

    private lateinit var clickFrame1: Drawable
    private lateinit var clickFrame2: Drawable
    private var clickCurrentFrame = 0

    // Флаг режима: true - постоянная анимация клика, false - ходьба
    private var isClickMode = false

    private var screenWidth = 0
    private var screenHeight = 0

    private var isDragging = false
    private var isDestroyed = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private var walkAnimRunnable: Runnable? = null
    private var clickAnimRunnable: Runnable? = null

    private val layoutParams: WindowManager.LayoutParams

    private var downX = 0f
    private var downY = 0f
    private var startTouchX = 0f
    private var startTouchY = 0f
    private var startWindowX = 0
    private var startWindowY = 0

    private val prefs = context.getSharedPreferences("personaz_prefs", Context.MODE_PRIVATE)
    private var sizePx: Int = prefs.getInt("personaz_size", SIZE_DEFAULT)

    private var currentRotation: Float = 0f
    private var currentScaleX: Float = 1f
    private var currentScaleY: Float = 1f

    private var posX = 0f
    private var posY = 0f

    init {
        walkFrame1 = context.getDrawable(R.drawable.personaz1)!!
        walkFrame2 = context.getDrawable(R.drawable.personaz2)!!
        clickFrame1 = context.getDrawable(R.drawable.anim1)!!
        clickFrame2 = context.getDrawable(R.drawable.anim2)!!
        setImageDrawable(walkFrame1)

        val metrics = context.resources.displayMetrics
        screenWidth = metrics.widthPixels
        screenHeight = metrics.heightPixels

        if (sizePx < 300) sizePx = 300
        if (sizePx > 600) sizePx = 600

        layoutParams = WindowManager.LayoutParams(
            sizePx, sizePx,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = (screenWidth / 2 - sizePx / 2)
            y = (screenHeight - sizePx - 20)
        }

        posX = layoutParams.x.toFloat()
        posY = layoutParams.y.toFloat()

        currentRotation = prefs.getFloat("rotation", 0f)
        currentScaleX = prefs.getFloat("scaleX", 1f)
        currentScaleY = prefs.getFloat("scaleY", 1f)
        applyTransformations()

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isDragging = false
                    downX = event.rawX
                    downY = event.rawY
                    startTouchX = event.rawX
                    startTouchY = event.rawY
                    startWindowX = layoutParams.x
                    startWindowY = layoutParams.y
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaX = event.rawX - downX
                    val deltaY = event.rawY - downY
                    if (kotlin.math.abs(deltaX) > CLICK_DRAG_THRESHOLD || kotlin.math.abs(deltaY) > CLICK_DRAG_THRESHOLD) {
                        isDragging = true
                    }
                    if (isDragging) {
                        val dx = (event.rawX - startTouchX).toInt()
                        val dy = (event.rawY - startTouchY).toInt()
                        val newX = (startWindowX + dx).coerceIn(0, screenWidth - sizePx)
                        val newY = (startWindowY + dy).coerceIn(0, screenHeight - sizePx)
                        layoutParams.x = newX
                        layoutParams.y = newY
                        windowManager.updateViewLayout(this, layoutParams)
                        posX = newX.toFloat()
                        posY = newY.toFloat()
                    }
                    true
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (!isDragging) {
                        toggleMode()   // переключаем режим вместо одноразовой анимации
                    }
                    true
                }
                else -> false
            }
        }

        windowManager.addView(this, layoutParams)
        startWalkAnimation()
    }

    private fun applyTransformations() {
        rotation = currentRotation
        scaleX = currentScaleX
        scaleY = currentScaleY
    }

    fun updateAll(newSize: Int, newRotation: Float, newScaleX: Float, newScaleY: Float) {
        if (newSize != sizePx) {
            sizePx = newSize.coerceIn(300, 600)
            layoutParams.width = sizePx
            layoutParams.height = sizePx
            val maxX = (screenWidth - sizePx).toFloat()
            val maxY = (screenHeight - sizePx).toFloat()
            posX = posX.coerceIn(0f, maxX)
            posY = posY.coerceIn(0f, maxY)
            layoutParams.x = posX.toInt()
            layoutParams.y = posY.toInt()
            try {
                windowManager.updateViewLayout(this, layoutParams)
            } catch (_: Exception) { }
        }

        currentRotation = newRotation
        currentScaleX = newScaleX
        currentScaleY = newScaleY
        applyTransformations()
    }

    // ---------- Анимация ходьбы (постоянная) ----------
    private fun startWalkAnimation() {
        if (isDestroyed) return
        walkAnimRunnable?.let { mainHandler.removeCallbacks(it) }
        walkAnimRunnable = object : Runnable {
            override fun run() {
                if (isDestroyed) return
                // Если мы не в режиме клика, крутим ходьбу
                if (!isClickMode) {
                    walkCurrentFrame = 1 - walkCurrentFrame
                    setImageDrawable(if (walkCurrentFrame == 0) walkFrame1 else walkFrame2)
                }
                mainHandler.postDelayed(this, ANIMATION_INTERVAL)
            }
        }
        mainHandler.post(walkAnimRunnable!!)
    }

    // ---------- Переключение режима (клик / ходьба) ----------
    private fun toggleMode() {
        if (isDestroyed) return

        isClickMode = !isClickMode

        if (isClickMode) {
            // Переключаемся на постоянную анимацию клика
            walkAnimRunnable?.let { mainHandler.removeCallbacks(it) }
            clickAnimRunnable?.let { mainHandler.removeCallbacks(it) }

            clickAnimRunnable = object : Runnable {
                var frame = 0
                override fun run() {
                    if (isDestroyed) return
                    if (isClickMode) {
                        frame = 1 - frame
                        setImageDrawable(if (frame == 0) clickFrame1 else clickFrame2)
                        mainHandler.postDelayed(this, ANIMATION_INTERVAL)
                    }
                }
            }
            mainHandler.post(clickAnimRunnable!!)
        } else {
            // Возвращаемся к ходьбе
            clickAnimRunnable?.let { mainHandler.removeCallbacks(it) }
            // Устанавливаем первый кадр ходьбы, чтобы не оставался кликовый
            setImageDrawable(walkFrame1)
            startWalkAnimation()
        }
    }

    // ---------- Очистка ----------
    fun destroy() {
        isDestroyed = true
        mainHandler.removeCallbacksAndMessages(null)
        try {
            windowManager.removeView(this)
        } catch (_: Exception) { }
    }
}