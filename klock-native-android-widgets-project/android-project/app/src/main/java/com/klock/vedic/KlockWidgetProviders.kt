package com.klock.vedic

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews

object KlockWidgetUpdater {
    const val ACTION_TICK = "com.klock.vedic.ACTION_WIDGET_TICK"

    fun updateAllWidgets(context: Context) {
        val mgr = AppWidgetManager.getInstance(context)
        val snap = VedicTimeCalculator.computeNow(context)

        val launchIntent = Intent(context, MainActivity::class.java)
        val pendingTap = PendingIntent.getActivity(
            context, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val compactIds = mgr.getAppWidgetIds(ComponentName(context, CompactKlockWidget::class.java))
        for (id in compactIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_klock_compact)
            rv.setTextViewText(R.id.tv_vedic_main, String.format("%02d : %02d", snap.ghati, snap.pala))
            rv.setOnClickPendingIntent(R.id.widget_root, pendingTap)
            mgr.updateAppWidget(id, rv)
        }

        val mediumIds = mgr.getAppWidgetIds(ComponentName(context, MediumKlockWidget::class.java))
        for (id in mediumIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_klock_medium)
            rv.setTextViewText(R.id.tv_vedic_main, String.format("%02d : %02d", snap.ghati, snap.pala))
            rv.setTextViewText(R.id.tv_civil_sub, snap.civilTimeFormatted)
            rv.setTextViewText(R.id.tv_muhurta_sub, "Muhūrta ${snap.muhurta}/30")
            rv.setOnClickPendingIntent(R.id.widget_root, pendingTap)
            mgr.updateAppWidget(id, rv)
        }

        val largeIds = mgr.getAppWidgetIds(ComponentName(context, LargeKlockWidget::class.java))
        for (id in largeIds) {
            val rv = RemoteViews(context.packageName, R.layout.widget_klock_large)
            rv.setTextViewText(R.id.tv_vedic_main, String.format("%02d : %02d", snap.ghati, snap.pala))
            rv.setTextViewText(R.id.tv_civil_sub, "Local Time: ${snap.civilTimeFormatted} • ${snap.anchorLabel}")
            rv.setProgressBar(R.id.pb_day_cycle, 100, snap.progressPercent, false)
            rv.setTextViewText(R.id.tv_cycle_pct, "${snap.progressPercent}% of 60-Ghaṭī Cycle")
            rv.setOnClickPendingIntent(R.id.widget_root, pendingTap)
            mgr.updateAppWidget(id, rv)
        }

        if (compactIds.isNotEmpty() || mediumIds.isNotEmpty() || largeIds.isNotEmpty()) {
            scheduleNextMinuteTick(context)
        }
    }

    fun scheduleNextMinuteTick(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CompactKlockWidget::class.java).apply { action = ACTION_TICK }
        val pi = PendingIntent.getBroadcast(
            context, 1001, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val now = System.currentTimeMillis()
        val nextTick = now + 24_000L - (now % 24_000L) // Align to every 1 Pala (24 seconds) while device is awake!
        alarmManager.set(AlarmManager.RTC, nextTick, pi)
    }
}

open class BaseKlockWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        KlockWidgetUpdater.updateAllWidgets(context)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == KlockWidgetUpdater.ACTION_TICK ||
            intent.action == Intent.ACTION_TIME_CHANGED ||
            intent.action == Intent.ACTION_TIMEZONE_CHANGED) {
            KlockWidgetUpdater.updateAllWidgets(context)
        }
    }
}

class CompactKlockWidget : BaseKlockWidget()
class MediumKlockWidget : BaseKlockWidget()
class LargeKlockWidget : BaseKlockWidget()
