package com.brunnodev.pos

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.net.HttpURLConnection
import java.net.URL

class OutboxSyncWorker(context: Context, parameters: WorkerParameters, private val repository: PosRepository) : CoroutineWorker(context, parameters) {
    override suspend fun doWork(): Result {
        val batch = repository.outboxBatch()
        if (batch.isEmpty()) return Result.success()
        return try {
            val endpoint = inputData.getString("endpoint") ?: return Result.failure()
            val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
                requestMethod = "POST"
                doOutput = true
                connectTimeout = 10000
                readTimeout = 10000
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty("Idempotency-Key", batch.first().id)
            }
            connection.outputStream.use { it.write(batch.joinToString(prefix = "[", postfix = "]") { row -> row.payload }.toByteArray()) }
            val code = connection.responseCode
            connection.disconnect()
            if (code in 200..299) { repository.acknowledge(batch.map { it.id }); Result.success() }
            else { repository.retry(batch.map { it.id }); Result.retry() }
        } catch (_: Exception) {
            repository.retry(batch.map { it.id })
            Result.retry()
        }
    }
}
