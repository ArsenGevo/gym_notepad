package com.gymnotepad.app.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.json.JSONObject

enum class WeightUnit { KILOGRAMS, POUNDS }

data class UserSettings(
    val weightUnit: WeightUnit = WeightUnit.KILOGRAMS,
    val restSeconds: Int = 90,
    val autoStartRestTimer: Boolean = false,
)

class UserSettingsRepository(context: Context) {
    private val preferences = context.getSharedPreferences("user-settings", Context.MODE_PRIVATE)
    private val mutableSettings = MutableStateFlow(read())
    val settings: StateFlow<UserSettings> = mutableSettings

    fun setWeightUnit(unit: WeightUnit) = update(mutableSettings.value.copy(weightUnit = unit))
    fun setRestSeconds(seconds: Int) = update(mutableSettings.value.copy(restSeconds = seconds.coerceIn(30, 600)))
    fun setAutoStartRestTimer(enabled: Boolean) = update(mutableSettings.value.copy(autoStartRestTimer = enabled))

    fun replaceFromBackup(json: JSONObject) = update(
        UserSettings(
            weightUnit = WeightUnit.valueOf(json.getString("weightUnit")),
            restSeconds = json.getInt("restSeconds").coerceIn(30, 600),
            autoStartRestTimer = json.getBoolean("autoStartRestTimer"),
        ),
    )

    private fun read() = UserSettings(
        weightUnit = runCatching { WeightUnit.valueOf(preferences.getString("weight-unit", null).orEmpty()) }
            .getOrDefault(WeightUnit.KILOGRAMS),
        restSeconds = preferences.getInt("rest-seconds", 90).coerceIn(30, 600),
        autoStartRestTimer = preferences.getBoolean("auto-rest", false),
    )

    private fun update(value: UserSettings) {
        preferences.edit()
            .putString("weight-unit", value.weightUnit.name)
            .putInt("rest-seconds", value.restSeconds)
            .putBoolean("auto-rest", value.autoStartRestTimer)
            .apply()
        mutableSettings.value = value
    }
}
