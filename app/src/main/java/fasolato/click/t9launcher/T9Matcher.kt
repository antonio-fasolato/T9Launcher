package fasolato.click.t9launcher

object T9Matcher {

    val T9_MAP = mapOf(
        '2' to "abc",
        '3' to "def",
        '4' to "ghi",
        '5' to "jkl",
        '6' to "mno",
        '7' to "pqrs",
        '8' to "tuv",
        '9' to "wxyz"
    )

    fun wordMatches(word: String, digits: String): Boolean {
        if (word.length < digits.length) return false
        for (i in digits.indices) {
            if (!charMatchesDigit(word[i], digits[i])) return false
        }
        return true
    }

    fun matchesName(name: String, digits: String): Boolean =
        matchPositions(name, digits) != null

    fun matchesDescription(description: String, digits: String): Boolean {
        if (description.isEmpty()) return false
        return matchPositions(description, digits) != null
    }

    // Returns the indices (in `text`) of a contiguous substring of non-delimiter chars
    // that T9-matches `digits`. Delimiters (whitespace, dash, underscore, dot) are
    // transparent: e.g. "Play Store" + "97" matches 'y' (idx 3) and 'S' (idx 5),
    // skipping the space at idx 4. Returns the leftmost match. Empty digits return
    // an empty list. Returns null when no match exists.
    fun matchPositions(text: String, digits: String): List<Int>? {
        if (digits.isEmpty()) return emptyList()
        val lower = text.lowercase()
        val nonDelim = mutableListOf<Int>()
        for (i in lower.indices) {
            if (!isDelimiter(lower[i])) nonDelim.add(i)
        }
        if (nonDelim.size < digits.length) return null
        for (start in 0..(nonDelim.size - digits.length)) {
            var matches = true
            for (j in digits.indices) {
                if (!charMatchesDigit(lower[nonDelim[start + j]], digits[j])) {
                    matches = false
                    break
                }
            }
            if (matches) return List(digits.length) { nonDelim[start + it] }
        }
        return null
    }

    private fun isDelimiter(c: Char): Boolean =
        c.isWhitespace() || c == '-' || c == '_' || c == '.'

    private fun charMatchesDigit(ch: Char, digit: Char): Boolean {
        val letters = T9_MAP[digit]
        return (letters != null && ch in letters) || ch == digit
    }
}
