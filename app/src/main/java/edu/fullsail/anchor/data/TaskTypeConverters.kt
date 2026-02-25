package edu.fullsail.anchor.data

import androidx.room.TypeConverter
import edu.fullsail.anchor.Subtask
import org.json.JSONArray
import org.json.JSONObject

/**
 * ADDED FOR SUBTASK PERSISTENCE
 * Room TypeConverter that serialises List<Subtask> to/from a JSON string.
 * Uses Android's built-in org.json — no extra Gradle dependency needed.
 * Registered on AnchorDatabase via @TypeConverters(TaskTypeConverters::class).
 */
class TaskTypeConverters {

    @TypeConverter
    fun fromSubtaskList(subtasks: List<Subtask>): String {
        val array = JSONArray()
        subtasks.forEach { s ->
            val obj = JSONObject()
            obj.put("id",     s.id)
            obj.put("title",  s.title)
            obj.put("isDone", s.isDone)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubtaskList(json: String): List<Subtask> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val array = JSONArray(json)
            (0 until array.length()).map { i ->
                val obj = array.getJSONObject(i)
                Subtask(
                    id     = obj.getString("id"),
                    title  = obj.getString("title"),
                    isDone = obj.getBoolean("isDone")
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}