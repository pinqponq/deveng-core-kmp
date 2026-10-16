package core.domain.camera.analysis.plate.manager

/** Defines when the analyzer's detector and OCR model sessions are released. */
enum class ModelSessionReleasePolicy {
    /** Keeps sessions loaded until the consumer closes the returned recognition handle. */
    MANUAL,

    /** Releases sessions after a validated result has been delivered to the consumer callback. */
    AFTER_SUCCESS
}