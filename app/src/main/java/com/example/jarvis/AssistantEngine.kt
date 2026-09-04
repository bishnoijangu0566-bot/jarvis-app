package com.example.jarvis

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object AssistantEngine {

    private val API_KEY = BuildConfig.ANTHROPIC_API_KEY
    private const val MODEL = "claude-sonnet-4-6"

    sealed class Result {
        data class Spoken(val text: String) : Result()
        data class Action(val text: String, val intent: Intent) : Result()
    }

    fun handle(context: Context, command: String): Result {
        val text = command.trim()
        val lower = text.lowercase()

        if (lower.startsWith("call ")) {
            val name = text.substring(5).trim()
            val number = lookupContactNumber(context, name)
            return if (number != null) {
                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number"))
                Result.Action("Calling $name...", intent)
            } else {
                Result.Spoken("I couldn't find a contact named $name.")
            }
        }

        if (lower.startsWith("open ")) {
            val appName = text.substring(5).trim()
            val intent = findAppIntent(context, appName)
            return if (intent != null) {
                Result.Action("Opening $appName...", intent)
            } else {
                Result.Spoken("I couldn't find an app called $appName.")
            }
        }

        return Result.Spoken(askClaude(text))
    }

    private fun lookupContactNumber(context: Context, name: String): String? {
        val resolver = context.contentResolver
        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
            ),
            "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
            arrayOf("%$name%"),
            null
        )
        cursor?.use {
            if (it.moveToFirst()) {
                val numberIndex = it.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                return it.getString(numberIndex)
            }
        }
        return null
    }

    private fun findAppIntent(context: Context, appName: String): Intent? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        } ?: return null
        return pm.getLaunchIntentForPackage(match.packageName)
    }

    private fun askClaude(userMessage: String): String {
        if (API_KEY.isBlank()) {
            return "No API key found. Set ANTHROPIC_API_KEY as a GitHub Actions secret."
        }
        return try {
            val url = URL("https://api.anthropic.com/v1/messages")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("x-api-key", API_KEY)
            conn.setRequestProperty("anthropic-version", "2023-06-01")
            conn.doOutput = true

            val body = JSONObject().apply {
                put("model", MODEL)
                put("max_tokens", 400)
                put("messages", JSONArray().put(
                    JSONObject().apply {
                        put("role", "user")
                        put("content", userMessage)
                    }
                ))
            }

            conn.outputStream.use { it.write(body.toString().toByteArray()) }

            val responseText = conn.inputStream.bufferedReader().use { it.readText() }
            val json = JSONObject(responseText)
            val contentArray = json.getJSONArray("content")
            val builder = StringBuilder()
            for (i in 0 until contentArray.length()) {
                val block = contentArray.getJSONObject(i)
                if (block.optString("type") == "text") {
                    builder.append(block.optString("text"))
                }
            }
            builder.toString().ifBlank { "I didn't get a response." }
        } catch (e: Exception) {
            "Something went wrong talking to the AI: ${e.message}"
        }
    }
}
