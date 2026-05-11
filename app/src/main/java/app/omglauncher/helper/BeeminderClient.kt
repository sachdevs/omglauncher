package app.omglauncher.helper

import app.omglauncher.data.BeeminderGoalSummary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Scanner

object BeeminderClient {
    private const val BASE_URL = "https://www.beeminder.com/api/v1/users/me.json"

    suspend fun fetchActiveGoals(accessToken: String): List<BeeminderGoalSummary> = withContext(Dispatchers.IO) {
        val url = URL("$BASE_URL?diff_since=0&skinny=true&emaciated=true")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "GET"
        connection.connectTimeout = 10000
        connection.readTimeout = 10000
        connection.useCaches = false
        connection.doInput = true
        connection.setRequestProperty("Authorization", "Bearer $accessToken")

        try {
            val responseCode = connection.responseCode
            val stream = if (responseCode in 200..299) connection.inputStream else connection.errorStream
            val response = stream?.use { input ->
                Scanner(input).use { scanner ->
                    scanner.useDelimiter("\\A")
                    if (scanner.hasNext()) scanner.next() else ""
                }
            }.orEmpty()

            if (responseCode !in 200..299) {
                throw IllegalStateException(parseError(response).ifBlank { "Unable to load Beeminder goals" })
            }

            parseGoals(JSONObject(response).optJSONArray("goals") ?: JSONArray())
        } finally {
            connection.disconnect()
        }
    }

    fun encodeGoals(goals: List<BeeminderGoalSummary>): String {
        val array = JSONArray()
        goals.forEach { goal ->
            array.put(
                JSONObject()
                    .put("slug", goal.slug)
                    .put("title", goal.title)
                    .put("urgencykey", goal.urgencyKey)
                    .put("losedate", goal.loseDate)
                    .put("safebuf", goal.safeBuffer)
                    .put("colorkey", goal.colorKey)
                    .put("limsum", goal.limitSummary)
                    .put("delta", goal.delta)
                    .put("todayta", goal.today)
                    .put("pledge", goal.pledge)
            )
        }
        return array.toString()
    }

    fun decodeGoals(json: String): List<BeeminderGoalSummary> {
        if (json.isBlank()) return emptyList()
        return parseGoals(JSONArray(json), includeInactive = true)
    }

    private fun parseGoals(goals: JSONArray, includeInactive: Boolean = false): List<BeeminderGoalSummary> {
        val parsedGoals = mutableListOf<BeeminderGoalSummary>()
        for (i in 0 until goals.length()) {
            val goal = goals.optJSONObject(i) ?: continue
            if (!includeInactive && (goal.optBoolean("won") || goal.optBoolean("lost") || goal.optBoolean("frozen"))) {
                continue
            }

            parsedGoals.add(
                BeeminderGoalSummary(
                    slug = goal.optString("slug"),
                    title = goal.optString("title"),
                    urgencyKey = goal.optString("urgencykey"),
                    loseDate = goal.optLong("losedate", 0L),
                    safeBuffer = goal.optInt("safebuf", 0),
                    colorKey = goal.optString("colorkey"),
                    limitSummary = goal.optString("limsum"),
                    delta = if (goal.isNull("delta")) null else goal.optDouble("delta"),
                    today = goal.optBoolean("todayta"),
                    pledge = parsePledge(goal)
                )
            )
        }
        return parsedGoals.sortedWith(compareBy<BeeminderGoalSummary> { it.urgencyKey }.thenBy { it.safeBuffer })
    }

    private fun parsePledge(goal: JSONObject): Double {
        val contract = goal.optJSONObject("contract")
        if (contract != null && !contract.isNull("amount")) return contract.optDouble("amount", 0.0)
        return goal.optDouble("pledge", 0.0)
    }

    private fun parseError(response: String): String {
        return try {
            val json = JSONObject(response)
            when {
                json.has("errors") -> json.optString("errors")
                json.has("error") -> json.optString("error")
                else -> ""
            }
        } catch (_: Exception) {
            ""
        }
    }
}
