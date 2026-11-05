package core.util.polling

/** [FINISHED] ends polling; [CONTINUE] waits for the schedule's next delay and checks again. */
enum class PollAttemptOutcome {
    FINISHED,
    CONTINUE,
}
