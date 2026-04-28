package fasolato.click.t9launcher

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class T9MatcherTest {

    // ---------- matchesName: positive ----------

    @Test
    fun name_matches_singleWord_shortPrefix() {
        assertTrue(T9Matcher.matchesName("WhatsApp", "9"))
    }

    @Test
    fun name_matches_singleWord_longPrefix() {
        // w->9, h->4, a->2
        assertTrue(T9Matcher.matchesName("WhatsApp", "942"))
    }

    @Test
    fun name_matches_caseInsensitive() {
        // s->7, p->7
        assertTrue(T9Matcher.matchesName("SPOTIFY", "77"))
    }

    @Test
    fun name_matches_secondWordSegment() {
        // "Google Maps", digits "62" matcha "Maps" (m->6, a->2)
        assertTrue(T9Matcher.matchesName("Google Maps", "62"))
    }

    @Test
    fun name_matches_dashDelimiter() {
        // "Adobe-Reader", digits "73" matcha "Reader" (r->7, e->3)
        assertTrue(T9Matcher.matchesName("Adobe-Reader", "73"))
    }

    @Test
    fun name_matches_underscoreDelimiter() {
        // "my_app", digits "2" matcha "app" (a->2)
        assertTrue(T9Matcher.matchesName("my_app", "2"))
    }

    @Test
    fun name_matches_dotDelimiter() {
        // "com.google.android", digits "26" matcha "com" (c->2, o->6)
        assertTrue(T9Matcher.matchesName("com.google.android", "26"))
    }

    @Test
    fun name_matches_literalDigitsInWord() {
        // "365 Days", digits "365" matcha la parola "365" (digit letterali)
        assertTrue(T9Matcher.matchesName("365 Days", "365"))
    }

    @Test
    fun name_matches_fdroid_innerWord() {
        // "F-Droid" + 376: matcha "droid" (d->3, r->7, o->6)
        assertTrue(T9Matcher.matchesName("F-Droid", "376"))
    }

    @Test
    fun name_matches_fdroid_crossWord() {
        // "F-Droid" + 337: matcha attraverso il delimitatore consumando tutto "f" (w0)
        // e poi i primi due caratteri di "droid" (w1): f->3, d->3, r->7
        assertTrue(T9Matcher.matchesName("F-Droid", "337"))
    }

    @Test
    fun name_matches_mixedLettersAndLiteralDigits() {
        // "Office365", digits "365" matcha la parola "office365"
        // (3==3, 6==6, 5==5 sui caratteri agli indici 6,7,8 — ma serve prefix:
        // qui digits="365" chiede word[0..2] == 3,6,5 → "office365"[0]='o' non matcha 3.
        // Per il match va testata una parola che inizia con i digit letterali:
        assertTrue(T9Matcher.matchesName("365app", "365"))
    }

    // ---------- matchesName: negative ----------

    @Test
    fun name_doesNotMatch_noCharSatisfiesDigit() {
        // "Hello" + "9": nessun char ('h','e','l','l','o') matcha 9 (->wxyz)
        assertFalse(T9Matcher.matchesName("Hello", "9"))
    }

    @Test
    fun name_doesNotMatch_substringNotContiguous() {
        // "Apple" + "23": 'a' (idx 0) -> 2 ok, ma 'p' (idx 1) non matcha 3.
        // L'unico altro char->3 e' 'e' (idx 4), ma il match deve essere contiguo
        // nel concatenato senza delimitatori.
        assertFalse(T9Matcher.matchesName("Apple", "23"))
    }

    @Test
    fun name_doesNotMatch_digitsLongerThanText() {
        // "OK" + "652": solo 2 char non-delim, digits sono 3.
        assertFalse(T9Matcher.matchesName("OK", "652"))
    }

    @Test
    fun name_doesNotMatch_noCharMatchesDigit() {
        // "Maps", digits "9": nessun char matcha 9 (->wxyz)
        assertFalse(T9Matcher.matchesName("Maps", "9"))
    }

    // ---------- matchesName: edge cases ----------

    @Test
    fun name_emptyDigits_matchesEverything() {
        // Comportamento corrente in produzione: digits vuoti → tutto matcha
        assertTrue(T9Matcher.matchesName("WhatsApp", ""))
    }

    @Test
    fun name_emptyName_doesNotMatchNonEmptyDigits() {
        assertFalse(T9Matcher.matchesName("", "2"))
    }

    @Test
    fun name_emptyName_emptyDigits_matches() {
        // wordMatches("", "") → true (loop non eseguito)
        assertTrue(T9Matcher.matchesName("", ""))
    }

    // ---------- matchesDescription ----------

    @Test
    fun description_empty_doesNotMatch() {
        assertFalse(T9Matcher.matchesDescription("", "2"))
    }

    @Test
    fun description_empty_emptyDigits_doesNotMatch() {
        // La guard isEmpty ha priorita' sul caso digits vuoti
        assertFalse(T9Matcher.matchesDescription("", ""))
    }

    @Test
    fun description_matches_firstWord() {
        // "Photo editor", digits "74": p->7, h->4 → matcha "photo"
        assertTrue(T9Matcher.matchesDescription("Photo editor", "74"))
    }

    @Test
    fun description_matches_innerWord() {
        // "Read books offline", digits "6": o->6 in "offline"
        assertTrue(T9Matcher.matchesDescription("Read books offline", "6"))
    }

    @Test
    fun description_doesNotMatch() {
        // "Photo editor", digits "22": p non in "abc", e non in "abc" → no match
        assertFalse(T9Matcher.matchesDescription("Photo editor", "22"))
    }

    @Test
    fun description_caseInsensitive() {
        // "OFFLINE FIRST", digits "63" → lower = "offline first", "offline" o->6, f->3
        assertTrue(T9Matcher.matchesDescription("OFFLINE FIRST", "63"))
    }

    // ---------- wordMatches: positive ----------

    @Test
    fun wordMatches_exactLength_allMappedLetters() {
        // word "abc", digits "222": tutti in "abc"
        assertTrue(T9Matcher.wordMatches("abc", "222"))
    }

    @Test
    fun wordMatches_prefixOnLongerWord() {
        // word "hello", digits "43": h->4, e->3
        assertTrue(T9Matcher.wordMatches("hello", "43"))
    }

    @Test
    fun wordMatches_literalDigitInWord() {
        // word "r2d2", digits "72": r->7 ok; '2'==digit '2' (letterale) ok
        assertTrue(T9Matcher.wordMatches("r2d2", "72"))
    }

    @Test
    fun wordMatches_emptyWord_emptyDigits() {
        // Loop non eseguito → true
        assertTrue(T9Matcher.wordMatches("", ""))
    }

    // ---------- wordMatches: negative ----------

    @Test
    fun wordMatches_wordShorterThanDigits() {
        assertFalse(T9Matcher.wordMatches("ab", "222"))
    }

    @Test
    fun wordMatches_emptyWord_nonEmptyDigits() {
        assertFalse(T9Matcher.wordMatches("", "2"))
    }

    @Test
    fun wordMatches_letterNotInDigitMap() {
        // word "xyz", digits "222": x non in "abc"
        assertFalse(T9Matcher.wordMatches("xyz", "222"))
    }

    // ---------- matchPositions: verifica indici evidenziati ----------

    @Test
    fun matchPositions_innerWord_returnsContiguousIndices() {
        // "F-Droid" + 376: indici di D, r, o nell'originale (2, 3, 4)
        assertEquals(listOf(2, 3, 4), T9Matcher.matchPositions("F-Droid", "376"))
    }

    @Test
    fun matchPositions_crossWord_skipsDelimiter() {
        // "F-Droid" + 337: F (0), poi salta il '-' (1), poi D (2), r (3)
        assertEquals(listOf(0, 2, 3), T9Matcher.matchPositions("F-Droid", "337"))
    }

    @Test
    fun matchPositions_singleWord_prefix() {
        // "WhatsApp" + 942: indici 0, 1, 2 (W, h, a)
        assertEquals(listOf(0, 1, 2), T9Matcher.matchPositions("WhatsApp", "942"))
    }

    @Test
    fun matchPositions_secondWord() {
        // "Google Maps" + 62: indici di M (7), a (8)
        assertEquals(listOf(7, 8), T9Matcher.matchPositions("Google Maps", "62"))
    }

    @Test
    fun matchPositions_emptyDigits_returnsEmptyList() {
        assertEquals(emptyList<Int>(), T9Matcher.matchPositions("WhatsApp", ""))
    }

    @Test
    fun matchPositions_noMatch_returnsNull() {
        // "Maps" + "9": nessun char matcha 9 -> null
        assertNull(T9Matcher.matchPositions("Maps", "9"))
    }

    @Test
    fun matchPositions_emptyText_nonEmptyDigits_returnsNull() {
        assertNull(T9Matcher.matchPositions("", "2"))
    }

    // ---------- ignore-spaces: match cross-space ----------

    @Test
    fun name_ignoresSpace_spanningAcrossWords() {
        // "Hello World" + 435569: tutto "hello" + 'w' di "world"
        // h->4, e->3, l->5, l->5, o->6, w->9
        assertTrue(T9Matcher.matchesName("Hello World", "435569"))
    }

    @Test
    fun matchPositions_ignoresSpace_skipsDelimiterIndex() {
        // Lo spazio (idx 5 in "Hello World") non riceve highlight; tornano gli
        // indici dei char veri: H(0) e(1) l(2) l(3) o(4), poi w(6).
        assertEquals(
            listOf(0, 1, 2, 3, 4, 6),
            T9Matcher.matchPositions("Hello World", "435569")
        )
    }

    @Test
    fun description_ignoresSpace_spanningAcrossWords() {
        // "Read books" + 73232: r,e,a,d (read) + b (books) -> 7,3,2,3,2
        assertTrue(T9Matcher.matchesDescription("Read books", "73232"))
    }

    @Test
    fun name_ignoresSpace_negative_lettersNotAdjacent() {
        // "Hello World" + "94": w (idx 6) -> 9, ma il char accanto e' 'o' (->6),
        // non matcha 4. Non c'e' nessuna coppia adiacente nel concatenato
        // "helloworld" che soddisfi 9,4.
        assertFalse(T9Matcher.matchesName("Hello World", "94"))
    }

    // ---------- substring search: match in mezzo o attraverso lo spazio ----------

    @Test
    fun name_matches_playStore_acrossSpace() {
        // "Play Store" + 97: y (idx 3) -> 9, S (idx 5, dopo lo spazio) -> 7.
        // Esempio canonico: lo spazio non blocca il match.
        assertTrue(T9Matcher.matchesName("Play Store", "97"))
    }

    @Test
    fun matchPositions_playStore_acrossSpace() {
        // L'highlighting deve coprire y (3) e S (5); lo spazio (4) e' saltato.
        assertEquals(listOf(3, 5), T9Matcher.matchPositions("Play Store", "97"))
    }

    @Test
    fun name_matches_substring_insideSingleWord() {
        // "WhatsApp" + 428: h (idx 1) -> 4, a (idx 2) -> 2, t (idx 3) -> 8.
        // Il match parte da meta' parola, non dall'inizio.
        assertTrue(T9Matcher.matchesName("WhatsApp", "428"))
    }

    @Test
    fun matchPositions_substring_insideSingleWord() {
        assertEquals(listOf(1, 2, 3), T9Matcher.matchPositions("WhatsApp", "428"))
    }
}
