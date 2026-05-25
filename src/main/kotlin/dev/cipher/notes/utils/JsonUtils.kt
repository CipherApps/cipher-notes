package dev.cipher.notes.utils

import dev.cipher.notes.data.TodoItem
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

object JsonUtils {

    fun todoItemsToJson(items: List<TodoItem>): String {
        val arr = JSONArray()
        items.forEach { item ->
            arr.put(JSONObject().apply {
                put("id", item.id)
                put("text", item.text)
                put("done", item.done)
            })
        }
        return arr.toString()
    }

    fun jsonToTodoItems(json: String): List<TodoItem> {
        return try {
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val obj = arr.getJSONObject(i)
                TodoItem(
                    id   = obj.optString("id", UUID.randomUUID().toString()),
                    text = obj.optString("text", ""),
                    done = obj.optBoolean("done", false)
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun newTodoItem(text: String = "") = TodoItem(
        id = UUID.randomUUID().toString(),
        text = text,
        done = false
    )
}
