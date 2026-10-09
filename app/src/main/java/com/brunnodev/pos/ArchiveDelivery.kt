package com.brunnodev.pos

import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

class ArchiveDelivery(
    private val token: String,
    endpoint: String = "https://vercel-home-telemetry-api.vercel.app/api/runs",
) {
    private val destination =
        URL(endpoint).also { require(it.protocol == "https" && it.userInfo == null && it.ref == null) }

    suspend fun send(key: String, kind: String, payload: String): Boolean =
        withContext(Dispatchers.IO) {
            require(token.length in 1..8192 && token.matches(Regex("[A-Za-z0-9_.-]+")))
            require(key.matches(Regex("[A-Za-z0-9_.:-]{1,128}")))
            require(kind.matches(Regex("[a-z][a-z0-9_-]{0,63}")))
            require(payload.toByteArray(Charsets.UTF_8).size <= 196608)
            val body =
                JSONObject()
                    .put("project", "secure-payment-pos-core")
                    .put("kind", kind)
                    .put("clientKey", key)
                    .put("result", JSONObject(payload))
                    .toString()
                    .toByteArray(Charsets.UTF_8)
            require(body.size <= 262144)
            val connection =
                (destination.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    instanceFollowRedirects = false
                    doOutput = true
                    connectTimeout = 10000
                    readTimeout = 10000
                    setFixedLengthStreamingMode(body.size)
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Authorization", "Bearer $token")
                }
            try {
                connection.outputStream.use { it.write(body) }
                if (connection.responseCode !in 200..299) return@withContext false
                val response =
                    connection.inputStream.use {
                        val bytes = ByteArray(4096)
                        var offset = 0
                        while (offset < bytes.size) {
                            val count = it.read(bytes, offset, bytes.size - offset)
                            if (count < 0) break
                            offset += count
                        }
                        bytes.copyOf(offset).toString(Charsets.UTF_8)
                    }
                val receipt = JSONObject(response)
                receipt.optBoolean("persisted", false) && receipt.optString("clientKey") == key
            } finally {
                connection.disconnect()
            }
        }
}
