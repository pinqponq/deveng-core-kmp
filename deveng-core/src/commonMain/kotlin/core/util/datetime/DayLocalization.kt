package core.util.datetime

private val TURKISH_DAYS = listOf(
    "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi", "Pazar"
)

private val ENGLISH_DAYS = listOf(
    "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"
)

private val SPANISH_DAYS = listOf(
    "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo"
)

private val FRENCH_DAYS = listOf(
    "Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche"
)

private val GERMAN_DAYS = listOf(
    "Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"
)

private val ITALIAN_DAYS = listOf(
    "Lunedì", "Martedì", "Mercoledì", "Giovedì", "Venerdì", "Sabato", "Domenica"
)

private val PORTUGUESE_DAYS = listOf(
    "Segunda-feira", "Terça-feira", "Quarta-feira", "Quinta-feira",
    "Sexta-feira", "Sábado", "Domingo"
)

private val ARABIC_DAYS = listOf(
    "الاثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت", "الأحد"
)

fun resolveDayNames(systemLanguage: String): List<String> {
    val normalizedLanguage = systemLanguage
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
    return when (normalizedLanguage) {
        "tr" -> TURKISH_DAYS
        "es" -> SPANISH_DAYS
        "fr" -> FRENCH_DAYS
        "de" -> GERMAN_DAYS
        "it" -> ITALIAN_DAYS
        "pt" -> PORTUGUESE_DAYS
        "ar" -> ARABIC_DAYS
        else -> ENGLISH_DAYS
    }
}
