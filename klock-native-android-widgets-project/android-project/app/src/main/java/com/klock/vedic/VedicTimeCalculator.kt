package com.klock.vedic

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.webkit.JavascriptInterface
import android.widget.Toast
import org.json.JSONObject
import java.util.Calendar

data class VedicSnapshot(
    val ghati: Int,
    val pala: Int,
    val vipala: Int,
    val muhurta: Int,
    val progressPercent: Int,
    val civilTimeFormatted: String,
    val anchorLabel: String
)

object VedicTimeCalculator {
    private const val PREFS_NAME = "klock_native_prefs"
    private const val KEY_STATE_JSON = "state_json"
    private const val MS_PER_VIPALA = 400L
    private const val MS_PER_PALA = 24_000L
    private const val MS_PER_GHATI = 1_440_000L
    private const val MS_PER_MUHURTA = 2_880_000L
    private const val MS_PER_DAY = 86_400_000L

    fun saveConfigJson(context: Context, json: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_STATE_JSON, json)
            .apply()
        KlockWidgetUpdater.updateAllWidgets(context)
    }

    fun computeNow(context: Context): VedicSnapshot {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val rawJson = prefs.getString(KEY_STATE_JSON, "{}") ?: "{}"
        val config = try { JSONObject(rawJson) } catch (_: Exception) { JSONObject() }

        val anchorMode = config.optString("anchorMode", "civil")
        val manualSunrise = config.optString("manualSunrise", "")

        val cal = Calendar.getInstance()
        val hour = cal.get(Calendar.HOUR_OF_DAY)
        val minute = cal.get(Calendar.MINUTE)
        val second = cal.get(Calendar.SECOND)
        val millis = cal.get(Calendar.MILLISECOND)

        val civilElapsedMs = hour * 3_600_000L + minute * 60_000L + second * 1_000L + millis
        var anchorOffsetMs = 0L
        var anchorLabel = "Civil Anchor"

        if (anchorMode == "sunrise") {
            anchorOffsetMs = if (manualSunrise.matches(Regex("\\d{2}:\\d{2}"))) {
                val parts = manualSunrise.split(":")
                (parts[0].toLong() * 3600L + parts[1].toLong() * 60L) * 1000L
            } else {
                6L * 3_600_000L
            }
            anchorLabel = "Sunrise Anchor"
        }

        val elapsed = ((civilElapsedMs - anchorOffsetMs) % MS_PER_DAY + MS_PER_DAY) % MS_PER_DAY
        val ghati = (elapsed / MS_PER_GHATI).toInt()
        val remGhati = elapsed % MS_PER_GHATI
        val pala = (remGhati / MS_PER_PALA).toInt()
        val vipala = ((remGhati % MS_PER_PALA) / MS_PER_VIPALA).toInt()
        val muhurta = (elapsed / MS_PER_MUHURTA).toInt() + 1
        val pct = ((elapsed * 100L) / MS_PER_DAY).toInt()

        val civilStr = String.format("%02d:%02d", hour, minute)
        return VedicSnapshot(ghati, pala, vipala, muhurta, pct, civilStr, anchorLabel)
    }
}

class WebViewWidgetBridge(private val context: Context) {
    @JavascriptInterface
    fun syncConfiguration(serializedJson: String) {
        VedicTimeCalculator.saveConfigJson(context, serializedJson)
    }

    @JavascriptInterface
    fun requestPinWidget(variant: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val mgr = context.getSystemService(AppWidgetManager::class.java)
            if (mgr != null && mgr.isRequestPinAppWidgetSupported) {
                val providerClass = when (variant.lowercase()) {
                    "compact" -> CompactKlockWidget::class.java
                    "large" -> LargeKlockWidget::class.java
                    else -> MediumKlockWidget::class.java
                }
                val provider = ComponentName(context, providerClass)
                mgr.requestPinAppWidget(provider, null, null)
            } else {
                Toast.makeText(context, "Long-press Home Screen -> Widgets -> Klock", Toast.LENGTH_LONG).show()
            }
        }
    }
}
