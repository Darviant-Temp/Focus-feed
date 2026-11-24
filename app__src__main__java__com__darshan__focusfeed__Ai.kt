package com.darshan.focusfeed

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object Ai {
    fun ask(key: String, prompt: String): String {
        if (key.isEmpty()) return "ERR:No API key saved. Add it in Settings."
        val c = URL("https://api.anthropic.com/v1/messages").openConnection() as HttpURLConnection
        c.requestMethod = "POST"
        c.connectTimeout = 20000
        c.readTimeout = 90000
        c.doOutput = true
        c.setRequestProperty("content-type", "application/json")
        c.setRequestProperty("x-api-key", key)
        c.setRequestProperty("anthropic-version", "2023-06-01")
        val body = JSONObject()
            .put("model", Config.MODEL)
            .put("max_tokens", 2500)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", prompt)))
            .toString()
        c.outputStream.use { it.write(body.toByteArray()) }
        val code = c.responseCode
        val txt = (if (code in 200..299) c.inputStream else c.errorStream).bufferedReader().use { it.readText() }
        if (code !in 200..299) return "ERR:$code " + txt.take(200)
        val arr = JSONObject(txt).getJSONArray("content")
        val sb = StringBuilder()
        for (i in 0 until arr.length()) {
            val o = arr.getJSONObject(i)
            if (o.optString("type") == "text") sb.append(o.getString("text"))
        }
        return sb.toString()
    }
}
