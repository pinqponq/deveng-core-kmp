package core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class PercentEncodingTest {
    @Test
    fun percentEncodeUriComponent_withUnreservedCharacters_keepsThemAsIs() {
        assertEquals("AZaz09-_.~", "AZaz09-_.~".percentEncodeUriComponent())
    }

    @Test
    fun percentEncodeUriComponent_withSpace_writesPercentTwentyNotPlus() {
        assertEquals("a%20b", "a b".percentEncodeUriComponent())
    }

    @Test
    fun percentEncodeUriComponent_withReservedCharacters_encodesThemUppercase() {
        assertEquals("%3F%26%3D%2F%3A%40", "?&=/:@".percentEncodeUriComponent())
    }

    @Test
    fun percentEncodeUriComponent_withNonAsciiCharacter_encodesUtf8Bytes() {
        assertEquals("%C3%87a%C4%9F%C4%B1", "Çağı".percentEncodeUriComponent())
    }

    @Test
    fun percentEncodeUriComponent_withBoundaryCharacters_encodesOnlyOutsideRanges() {
        assertEquals("%40AZ%5B%60az%7B%2F09%3A", "@AZ[`az{/09:".percentEncodeUriComponent())
    }
}
