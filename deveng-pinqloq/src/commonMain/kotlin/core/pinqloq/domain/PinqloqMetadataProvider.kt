package core.pinqloq.domain

fun interface PinqloqMetadataProvider {
    fun provideMetadata(): Map<String, String>

    companion object {
        val None = PinqloqMetadataProvider { emptyMap() }
    }
}
