package com.example.personaz

import android.app.AlertDialog
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private val REQUEST_CODE_OVERLAY_PERMISSION = 1001
    private lateinit var prefs: SharedPreferences

    private var currentSize = 100
    private var currentRotation = 0f
    private var currentScaleX = 1f
    private var currentScaleY = 1f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = getSharedPreferences("personaz_prefs", MODE_PRIVATE)
        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (!::prefs.isInitialized) {
            prefs = getSharedPreferences("personaz_prefs", MODE_PRIVATE)
        }
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        // Получаем action из extra
        val action = intent?.getStringExtra("action")

        when (action) {
            "stop" -> {
                stopPersonazService()
                finishAffinity()
            }
            "settings" -> {
                // Открываем настройки без проверки разрешения
                showSettingsDialog()
            }
            else -> {
                // Обычный запуск — запрашиваем разрешение
                checkOverlayPermissionAndStart()
            }
        }
    }

    private fun checkOverlayPermissionAndStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivityForResult(intent, REQUEST_CODE_OVERLAY_PERMISSION)
            } else {
                startPersonazService()
                finish()
            }
        } else {
            startPersonazService()
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_OVERLAY_PERMISSION) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startPersonazService()
                finish()
            } else {
                Toast.makeText(this, "Разрешение не получено", Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun showSettingsDialog() {
        currentSize = prefs.getInt("personaz_size", 100)
        currentRotation = prefs.getFloat("rotation", 0f)
        currentScaleX = prefs.getFloat("scaleX", 1f)
        currentScaleY = prefs.getFloat("scaleY", 1f)

        val dialogView = layoutInflater.inflate(R.layout.dialog_settings, null)

        val seekBarSize = dialogView.findViewById<SeekBar>(R.id.seekBarSize)
        val textSize = dialogView.findViewById<TextView>(R.id.textSizeValue)
        val seekBarRotation = dialogView.findViewById<SeekBar>(R.id.seekBarRotation)
        val textRotation = dialogView.findViewById<TextView>(R.id.textRotationValue)

        val btnMirrorH = dialogView.findViewById<Button>(R.id.btnMirrorHorizontal)
        val btnMirrorV = dialogView.findViewById<Button>(R.id.btnMirrorVertical)
        val btnReset = dialogView.findViewById<Button>(R.id.btnReset)
        val btnHide = dialogView.findViewById<Button>(R.id.btnHidePersonaz)
        val btnShow = dialogView.findViewById<Button>(R.id.btnShowPersonaz)

        seekBarSize.progress = currentSize - 20
        textSize.text = "$currentSize px"
        seekBarRotation.progress = (currentRotation + 180).toInt()
        textRotation.text = "${currentRotation.toInt()}°"

        fun applyAndSave(showToast: Boolean = true) {
            prefs.edit()
                .putInt("personaz_size", currentSize)
                .putFloat("rotation", currentRotation)
                .putFloat("scaleX", currentScaleX)
                .putFloat("scaleY", currentScaleY)
                .apply()

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                if (showToast) {
                    Toast.makeText(this, "Для отображения персонажа нужно разрешение", Toast.LENGTH_SHORT).show()
                }
                return
            }

            if (FloatingPersonazService.instance == null) {
                startPersonazService()
            }
            FloatingPersonazService.instance?.updatePersonazSettings(
                currentSize,
                currentRotation,
                currentScaleX,
                currentScaleY
            )
        }

        seekBarSize.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val realSize = progress + 20
                textSize.text = "$realSize px"
                currentSize = realSize
                applyAndSave(false)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarRotation.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val angle = progress - 180
                textRotation.text = "$angle°"
                currentRotation = angle.toFloat()
                applyAndSave(false)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnMirrorH.setOnClickListener {
            currentScaleX = -currentScaleX
            applyAndSave()
            Toast.makeText(this, "Отзеркалено: ${if (currentScaleX == 1f) "выкл" else "вкл"}", Toast.LENGTH_SHORT).show()
        }

        btnMirrorV.setOnClickListener {
            currentScaleY = -currentScaleY
            applyAndSave()
            Toast.makeText(this, "Перевернуто: ${if (currentScaleY == 1f) "выкл" else "вкл"}", Toast.LENGTH_SHORT).show()
        }

        btnReset.setOnClickListener {
            currentSize = 100
            currentRotation = 0f
            currentScaleX = 1f
            currentScaleY = 1f

            seekBarSize.progress = currentSize - 20
            textSize.text = "$currentSize px"
            seekBarRotation.progress = (currentRotation + 180).toInt()
            textRotation.text = "${currentRotation.toInt()}°"

            applyAndSave()
            Toast.makeText(this, "Настройки сброшены", Toast.LENGTH_SHORT).show()
        }

        btnHide.setOnClickListener {
            FloatingPersonazService.instance?.hidePersonaz()
            Toast.makeText(this, "Персонаж скрыт", Toast.LENGTH_SHORT).show()
        }

        btnShow.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "Для показа персонажа нужно разрешение", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            FloatingPersonazService.instance?.showPersonaz()
            Toast.makeText(this, "Персонаж показан", Toast.LENGTH_SHORT).show()
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Настройки персонажа")
            .setView(dialogView)
            .setPositiveButton("Закрыть") { _, _ -> }
            .setCancelable(true)
            .create()

        dialog.setOnDismissListener {
            finish()
        }

        dialog.setCanceledOnTouchOutside(true)
        dialog.show()
    }

    private fun startPersonazService() {
        val intent = Intent(this, FloatingPersonazService::class.java)
        startService(intent)
    }

    private fun stopPersonazService() {
        val intent = Intent(this, FloatingPersonazService::class.java)
        stopService(intent)
    }
}