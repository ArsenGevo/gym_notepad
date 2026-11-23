package com.gymnotepad.app.data

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import androidx.room.withTransaction
import com.gymnotepad.app.data.local.GymDatabase
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class BackupRepository(
    private val context: Context,
    private val database: GymDatabase,
    private val settingsRepository: UserSettingsRepository,
) {
    private val insertOrder = listOf(
        "workout_sessions", "exercises", "workout_templates", "body_weight_measurements",
        "session_exercises", "exercise_load_options", "template_exercises", "workout_sets",
    )

    suspend fun exportTo(uri: Uri) = withContext(Dispatchers.IO) {
        val root = database.withTransaction {
            JSONObject()
                .put("format", "gym-notepad-backup")
                .put("formatVersion", 1)
                .put("databaseVersion", 4)
                .put("exportedAt", System.currentTimeMillis())
                .put("settings", settingsRepository.settings.value.toJson())
                .put("tables", JSONObject().also { tables ->
                    insertOrder.forEach { table -> tables.put(table, readTable(table)) }
                })
        }
        requireNotNull(context.contentResolver.openOutputStream(uri, "wt")).bufferedWriter().use {
            it.write(root.toString(2))
        }
    }

    suspend fun importFrom(uri: Uri) = withContext(Dispatchers.IO) {
        val text = requireNotNull(context.contentResolver.openInputStream(uri)).use { input ->
            BufferedReader(InputStreamReader(input)).readText()
        }
        val root = JSONObject(text)
        require(root.optString("format") == "gym-notepad-backup") { "Неверный формат файла" }
        require(root.optInt("formatVersion") == 1) { "Неподдерживаемая версия резервной копии" }
        require(root.optInt("databaseVersion") == 4) { "Несовместимая версия базы данных" }
        val tables = root.getJSONObject("tables")
        insertOrder.forEach { require(tables.has(it)) { "В копии отсутствует таблица $it" } }

        database.withTransaction {
            val db = database.openHelper.writableDatabase
            insertOrder.asReversed().forEach { db.execSQL("DELETE FROM $it") }
            insertOrder.forEach { table ->
                val rows = tables.getJSONArray(table)
                repeat(rows.length()) { index ->
                    val values = rows.getJSONObject(index).toContentValues()
                    check(db.insert(table, 0, values) != -1L) { "Не удалось восстановить $table" }
                }
            }
        }
        settingsRepository.replaceFromBackup(root.getJSONObject("settings"))
        database.invalidationTracker.refreshAsync()
    }

    private fun readTable(table: String): JSONArray = JSONArray().also { result ->
        database.openHelper.readableDatabase.query("SELECT * FROM $table").use { cursor ->
            while (cursor.moveToNext()) result.put(cursor.toJson())
        }
    }

    private fun Cursor.toJson() = JSONObject().also { row ->
        columnNames.forEachIndexed { index, name ->
            row.put(name, when (getType(index)) {
                Cursor.FIELD_TYPE_NULL -> JSONObject.NULL
                Cursor.FIELD_TYPE_INTEGER -> getLong(index)
                Cursor.FIELD_TYPE_FLOAT -> getDouble(index)
                Cursor.FIELD_TYPE_BLOB -> error("BLOB is not supported")
                else -> getString(index)
            })
        }
    }
}

private fun JSONObject.toContentValues() = ContentValues().also { values ->
    keys().forEach { key ->
        when (val value = get(key)) {
            JSONObject.NULL -> values.putNull(key)
            is Int -> values.put(key, value)
            is Long -> values.put(key, value)
            is Double -> values.put(key, value)
            is String -> values.put(key, value)
            else -> error("Unsupported value for $key")
        }
    }
}

private fun UserSettings.toJson() = JSONObject()
    .put("weightUnit", weightUnit.name)
    .put("restSeconds", restSeconds)
    .put("autoStartRestTimer", autoStartRestTimer)
