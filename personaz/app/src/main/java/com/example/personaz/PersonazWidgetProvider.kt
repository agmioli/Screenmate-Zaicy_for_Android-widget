package com.example.personaz

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

/**
 * Провайдер виджета, который отображает картинку nawidget.png
 * и при нажатии открывает настройки персонажа.
 */
class PersonazWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    private fun updateAppWidget(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int
    ) {
        val views = RemoteViews(context.packageName, R.layout.personaz_widget_layout)

        // Intent для открытия настроек
        val intent = Intent(context, MainActivity::class.java).apply {
            putExtra("action", "settings")
        }
        // Используем appWidgetId как requestCode, чтобы каждый виджет имел уникальный PendingIntent
        val pendingIntent = PendingIntent.getActivity(
            context,
            appWidgetId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        views.setOnClickPendingIntent(R.id.widget_image, pendingIntent)

        appWidgetManager.updateAppWidget(appWidgetId, views)
    }
}