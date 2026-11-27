package core.data.keyvalue

interface LanguageStorage {
    fun getSelectedLanguageCode(): String?

    fun setSelectedLanguageCode(code: String)
}
