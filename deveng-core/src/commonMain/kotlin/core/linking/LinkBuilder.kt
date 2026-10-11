package core.linking

class LinkBuilder(
    private val host: String,
    private val scheme: String = "https"
) {
    fun build(vararg pathSegments: Any): String =
        "$scheme://$host" + pathSegments.joinToString(separator = "") { "/$it" }
}
