package com.darshan.focusfeed

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Ai {
    // Keys starting with "sk-ant" go to Claude; any other key is treated as a Gemini key.
    fun ask(key: String, prompt: String): String {
        if (key.isEmpty()) return "ERR:No API key saved. Add it in Settings."
        return if (key.startsWith("sk-ant")) claude(key, prompt) else gemini(key, prompt)
    }

    private fun call(url: String, headers: Map<String, String>, body: String, readMs: Int = 90000): Pair<Int, String> {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = readMs
        c.doOutput = true
        c.setRequestProperty("content-type", "application/json")
        for ((k, v) in headers) c.setRequestProperty(k, v)
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        return Pair(code, txt)
    }

    private fun claude(key: String, prompt: String): String {
        val body = JSONObject().put("model", Config.CLAUDE_MODEL).put("max_tokens", 2500)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt))).toString()
        val (code, txt) = call("https://api.anthropic.com/v1/messages", mapOf("x-api-key" to key, "anthropic-version" to "2023-06-01"), body)
        if (code !in 200..299) return "ERR:$code " + txt.take(200)
        val arr = JSONObject(txt).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("type") == "text") sb.append(o.getString("text"))
        }
        return sb.toString()
    }

    private fun gemini(key: String, prompt: String): String {
        val body = JSONObject().put(
            "contents",
            JSONArray().put(JSONObject().put("parts", JSONArray().put(JSONObject().put("text", prompt))))
        ).toString()
        val errors = ArrayList<String>()
        var busy = 0
        for (m in Config.GEMINI_MODELS) {
            var tries = 0
            while (tries < 2) {
                tries++
                try {
                    val url = "https://generativelanguage.googleapis.com/v1beta/models/" + m + ":generateContent"
                    val (code, txt) = call(url, mapOf("x-goog-api-key" to key), body, 30000)
                    if (code in 200..299) {
                        val parts = JSONObject(txt).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts")
                        val sb = StringBuilder()
                        for (i in 0 until parts.length()) sb.append(parts.getJSONObject(i).optString("text"))
                        if (sb.isNotEmpty()) return sb.toString()
                        errors.add(m + ": empty answer")
                        break
                    }
                    if (code == 503 || code == 429) {
                        busy++
                        errors.add(code.toString() + " " + m)
                        if (tries < 2) { Thread.sleep(3000); continue }
                        break
                    }
                    errors.add(code.toString() + " " + m + " " + txt.take(80))
                    break
                } catch (e: Exception) {
                    errors.add((e.message ?: "network error") + " " + m)
                    break
                }
            }
        }
        val head = if (busy > 0) "Google's free servers are busy or at a limit right now. Wait a minute, then tap Keep going. " else ""
        return "ERR:" + head + "(" + errors.joinToString("; ") + ")"
    }
}
