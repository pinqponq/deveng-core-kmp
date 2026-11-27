package core.util.datetime

import kotlinx.datetime.format.MonthNames.Companion.ENGLISH_ABBREVIATED
import kotlinx.datetime.format.MonthNames.Companion.ENGLISH_FULL

private val TURKISH_MONTH_ABBREVIATIONS = listOf(
    "Oca", "Şub", "Mar", "Nis", "May", "Haz",
    "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara"
)

private val SPANISH_MONTH_ABBREVIATIONS = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

private val FRENCH_MONTH_ABBREVIATIONS = listOf(
    "Janv.", "Févr.", "Mars", "Avr.", "Mai", "Juin",
    "Juil.", "Août", "Sept.", "Oct.", "Nov.", "Déc."
)

private val GERMAN_MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez"
)

private val ITALIAN_MONTH_ABBREVIATIONS = listOf(
    "Gen", "Feb", "Mar", "Apr", "Mag", "Giu", "Lug", "Ago", "Set", "Ott", "Nov", "Dic"
)

private val PORTUGUESE_MONTH_ABBREVIATIONS = listOf(
    "Jan", "Fev", "Mar", "Abr", "Mai", "Jun", "Jul", "Ago", "Set", "Out", "Nov", "Dez"
)

private val ARABIC_MONTH_ABBREVIATIONS = listOf(
    "ينا", "فبر", "مار", "أبر", "ماي", "يون", "يول", "أغس", "سبت", "أكت", "نوف", "ديس"
)

private val RUSSIAN_MONTH_ABBREVIATIONS = listOf(
    "Янв", "Фев", "Мар", "Апр", "Май", "Июн", "Июл", "Авг", "Сен", "Окт", "Ноя", "Дек"
)

private val UKRAINIAN_MONTH_ABBREVIATIONS = listOf(
    "Січ", "Лют", "Бер", "Кві", "Тра", "Чер", "Лип", "Сер", "Вер", "Жов", "Лис", "Гру"
)

private val DUTCH_MONTH_ABBREVIATIONS = listOf(
    "Jan", "Feb", "Mrt", "Apr", "Mei", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dec"
)

private val POLISH_MONTH_ABBREVIATIONS = listOf(
    "Sty", "Lut", "Mar", "Kwi", "Maj", "Cze", "Lip", "Sie", "Wrz", "Paź", "Lis", "Gru"
)

private val PERSIAN_MONTH_ABBREVIATIONS = listOf(
    "ژان", "فور", "مار", "آور", "مه", "ژوئن", "ژوئیه", "اوت", "سپت", "اکت", "نوا", "دسا"
)

private val TURKISH_MONTH = listOf(
    "Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
    "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık"
)

private val SPANISH_MONTH = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val FRENCH_MONTH = listOf(
    "Janvier", "Février", "Mars", "Avril", "Mai", "Juin",
    "Juillet", "Août", "Septembre", "Octobre", "Novembre", "Décembre"
)

private val GERMAN_MONTH = listOf(
    "Januar", "Februar", "März", "April", "Mai", "Juni",
    "Juli", "August", "September", "Oktober", "November", "Dezember"
)

private val ITALIAN_MONTH = listOf(
    "Gennaio", "Febbraio", "Marzo", "Aprile", "Maggio", "Giugno",
    "Luglio", "Agosto", "Settembre", "Ottobre", "Novembre", "Dicembre"
)

private val PORTUGUESE_MONTH = listOf(
    "Janeiro", "Fevereiro", "Março", "Abril", "Maio", "Junho",
    "Julho", "Agosto", "Setembro", "Outubro", "Novembro", "Dezembro"
)

private val ARABIC_MONTH = listOf(
    "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
    "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
)

private val RUSSIAN_MONTH = listOf(
    "Январь", "Февраль", "Март", "Апрель", "Май", "Июнь",
    "Июль", "Август", "Сентябрь", "Октябрь", "Ноябрь", "Декабрь"
)

private val UKRAINIAN_MONTH = listOf(
    "Січень", "Лютий", "Березень", "Квітень", "Травень", "Червень",
    "Липень", "Серпень", "Вересень", "Жовтень", "Листопад", "Грудень"
)

private val DUTCH_MONTH = listOf(
    "Januari", "Februari", "Maart", "April", "Mei", "Juni",
    "Juli", "Augustus", "September", "Oktober", "November", "December"
)

private val POLISH_MONTH = listOf(
    "Styczeń", "Luty", "Marzec", "Kwiecień", "Maj", "Czerwiec",
    "Lipiec", "Sierpień", "Wrzesień", "Październik", "Listopad", "Grudzień"
)

private val PERSIAN_MONTH = listOf(
    "ژانویه", "فوریه", "مارس", "آوریل", "مه", "ژوئن",
    "ژوئیه", "اوت", "سپتامبر", "اکتبر", "نوامبر", "دسامبر"
)

private fun normalizeLanguage(systemLanguage: String): String {
    return systemLanguage
        .substringBefore('-')
        .substringBefore('_')
        .lowercase()
}

fun resolveMonthAbbreviationNames(systemLanguage: String): List<String> {
    return when (normalizeLanguage(systemLanguage)) {
        "tr" -> TURKISH_MONTH_ABBREVIATIONS
        "es" -> SPANISH_MONTH_ABBREVIATIONS
        "fr" -> FRENCH_MONTH_ABBREVIATIONS
        "de" -> GERMAN_MONTH_ABBREVIATIONS
        "it" -> ITALIAN_MONTH_ABBREVIATIONS
        "pt" -> PORTUGUESE_MONTH_ABBREVIATIONS
        "ar" -> ARABIC_MONTH_ABBREVIATIONS
        "ru" -> RUSSIAN_MONTH_ABBREVIATIONS
        "uk" -> UKRAINIAN_MONTH_ABBREVIATIONS
        "nl" -> DUTCH_MONTH_ABBREVIATIONS
        "pl" -> POLISH_MONTH_ABBREVIATIONS
        "fa" -> PERSIAN_MONTH_ABBREVIATIONS
        else -> ENGLISH_ABBREVIATED.names
    }
}

fun resolveMonthNames(systemLanguage: String): List<String> {
    return when (normalizeLanguage(systemLanguage)) {
        "tr" -> TURKISH_MONTH
        "es" -> SPANISH_MONTH
        "fr" -> FRENCH_MONTH
        "de" -> GERMAN_MONTH
        "it" -> ITALIAN_MONTH
        "pt" -> PORTUGUESE_MONTH
        "ar" -> ARABIC_MONTH
        "ru" -> RUSSIAN_MONTH
        "uk" -> UKRAINIAN_MONTH
        "nl" -> DUTCH_MONTH
        "pl" -> POLISH_MONTH
        "fa" -> PERSIAN_MONTH
        else -> ENGLISH_FULL.names
    }
}
