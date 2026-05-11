package app.omglauncher.helper

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class BeeminderPostWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val body = JSONObject().apply {
                put("goals", JSONArray().apply {
                    put(JSONObject().apply { put("meditation", 1) })
                })
            }.toString().toByteArray()

            var currentUrl = "https://michellesaksham.com/api/beeminder"
            var redirects = 0
            while (redirects < 5) {
                val connection = URL(currentUrl).openConnection() as HttpURLConnection
                connection.instanceFollowRedirects = false
                connection.requestMethod = "POST"
                connection.connectTimeout = 15000
                connection.readTimeout = 15000
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.outputStream.use { it.write(body) }

                val code = connection.responseCode
                if (code in 300..399) {
                    currentUrl = connection.getHeaderField("Location") ?: return@withContext Result.retry()
                    connection.disconnect()
                    redirects++
                } else {
                    connection.disconnect()
                    return@withContext if (code in 200..299) Result.success() else Result.retry()
                }
            }
            Result.retry()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
