package edu.fullsail.anchor.data

import androidx.room.TypeConverter
import edu.fullsail.anchor.Subtask
import org.json.JSONArray
import org.json.JSONObject

/**
 * Room TypeConverter that serializes and deserializes List<Subtask> as a JSON string.
 *
 * Room cannot store a List directly in a column, so these converters are called
 * automatically by Room whenever it reads or writes the subtasksJson column.
 *
 * Uses Android's built-in org.json library — no additional Gradle dependency needed.
 * Registered at the database level via @TypeConverters(TaskTypeConverters::class)
 * in AnchorDatabase so it applies to all entities in the database.
 */
class TaskTypeConverters {

    /**
     * Converts a List<Subtask> to a JSON string for storage in Room.
     * Each subtask is serialized as {"id": "...", "title": "...", "isDone": true/false}.
     */
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

    /**
     * Converts a JSON string from Room back into a List<Subtask>.
     * Returns an empty list for blank or empty-array strings, and on any parse error,
     * so a malformed row never crashes the app.
     */
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