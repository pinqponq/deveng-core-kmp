package core.util.polling

/** [FINISHED] ends polling; [CONTINUE] waits for the schedule's next delay and makes the next attempt. */
enum class PollAttemptOutcome {
    FINISHED,
    CONTINUE,
}
