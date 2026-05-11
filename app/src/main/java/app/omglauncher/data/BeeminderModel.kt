package app.omglauncher.data

data class BeeminderGoalSummary(
    val slug: String,
    val title: String,
    val urgencyKey: String,
    val loseDate: Long,
    val safeBuffer: Int,
    val colorKey: String,
    val limitSummary: String,
    val delta: Double?,
    val today: Boolean,
    val pledge: Double,
) {
    val displayName: String
        get() = title.ifBlank { slug }
}

enum class BeeminderPostStatus { IDLE, POSTING, POSTED }

data class BeeminderTimerState(
    val durationMs: Long,
    val remainingMs: Long,
    val isRunning: Boolean,
    val postStatus: BeeminderPostStatus = BeeminderPostStatus.IDLE,
)

sealed class BeeminderDashboardState {
    data object NotConfigured : BeeminderDashboardState()
    data object Loading : BeeminderDashboardState()
    data class Loaded(
        val goals: List<BeeminderGoalSummary>,
        val totalActiveGoals: Int,
        val updatedAtMs: Long,
        val stale: Boolean = false,
    ) : BeeminderDashboardState()

    data class Error(
        val message: String,
        val cached: Loaded? = null,
    ) : BeeminderDashboardState()
}
