/*
 * Copyright 2002-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package app.core

import java.util.*

/**
 * Miscellaneous [String] utility methods.
 *
 *
 * Mainly for internal use within the framework; consider
 * [Apache's Commons Lang](https://commons.apache.org/proper/commons-lang/)
 * for a more comprehensive suite of `String` utilities.
 *
 *
 * This class delivers some simple functionality that should really be
 * provided by the core Java [String] and [StringBuilder]
 * classes. It also provides easy-to-use methods to convert between
 * delimited strings, such as CSV strings, and collections and arrays.
 *
 * @author Rod Johnson
 * @author Juergen Hoeller
 * @author Keith Donald
 * @author Rob Harrop
 * @author Rick Evans
 * @author Arjen Poutsma
 * @author Sam Brannen
 * @author Brian Clozel
 * @since 16 April 2001
 */
object StringUtils {
    private val EMPTY_STRING_ARRAY = arrayOf<String>()

    //---------------------------------------------------------------------
    // General convenience methods for working with Strings
    //---------------------------------------------------------------------

    /**
     * Check that the given `String` is neither `null` nor of length 0.
     *
     * Note: this method returns `true` for a `String` that
     * purely consists of whitespace.
     * @param str the `String` to check (may be `null`)
     * @return `true` if the `String` is not `null` and has length
     * @see .hasLength
     * @see .hasText
     */
    fun hasLength(str: String?): Boolean {
        return str != null && !str.isEmpty()
    }

    /**
     * Check whether the given `String` contains actual *text*.
     *
     * More specifically, this method returns `true` if the
     * `String` is not `null`, its length is greater than 0,
     * and it contains at least one non-whitespace character.
     * @param str the `String` to check (may be `null`)
     * @return `true` if the `String` is not `null`, its
     * length is greater than 0, and it does not contain whitespace only
     * @see .hasText
     * @see .hasLength
     * @see Character.isWhitespace
     */
    fun hasText(str: String?): Boolean {
        return str != null && !str.isEmpty() && containsText(str)
    }

    private fun containsText(str: CharSequence): Boolean {
        val strLen = str.length
        for (i in 0 until strLen) {
            if (!Character.isWhitespace(str[i])) {
                return true
            }
        }
        return false
    }


    /**
     * Replace all occurrences of a substring within a string with another string.
     * @param inString `String` to examine
     * @param oldPattern `String` to replace
     * @param newPattern `String` to insert
     * @return a `String` with the replacements
     */
    fun replace(
        inString: String,
        oldPattern: String,
        newPattern: String?
    ): String {
        if (!hasLength(inString) || !hasLength(
                oldPattern
            ) || newPattern == null
        ) {
            return inString
        }
        var index = inString.indexOf(oldPattern)
        if (index == -1) {
            // no occurrence -> can return input as-is
            return inString
        }
        var capacity = inString.length
        if (newPattern.length > oldPattern.length) {
            capacity += 16
        }
        val sb = StringBuilder(capacity)
        var pos = 0 // our position in the old string
        val patLen = oldPattern.length
        while (index >= 0) {
            sb.append(inString, pos, index)
            sb.append(newPattern)
            pos = index + patLen
            index = inString.indexOf(oldPattern, pos)
        }

        // append any characters to the right of a match
        sb.append(inString, pos, inString.length)
        return sb.toString()
    }

    /**
     * Delete any character in a given `String`.
     * @param inString the original `String`
     * @param charsToDelete a set of characters to delete.
     * E.g. "az\n" will delete 'a's, 'z's and new lines.
     * @return the resulting `String`
     */
    fun deleteAny(inString: String, charsToDelete: String?): String {
        if (!hasLength(inString) || !hasLength(
                charsToDelete
            )
        ) {
            return inString
        }
        val sb = StringBuilder(inString.length)
        for (i in 0 until inString.length) {
            val c = inString[i]
            if (charsToDelete!!.indexOf(c) == -1) {
                sb.append(c)
            }
        }
        return sb.toString()
    }

    /**
     * Determine the RFC 3066 compliant language tag,
     * as used for the HTTP "Accept-Language" header.
     * @param locale the Locale to transform to a language tag
     * @return the RFC 3066 compliant language tag as `String`
     */
    @Deprecated("as of 5.0.4, in favor of {@link Locale#toLanguageTag()}")
    fun toLanguageTag(locale: Locale): String {
        return locale.language + if (hasText(locale.country)) "-" + locale.country else ""
    }

    //---------------------------------------------------------------------
    // Convenience methods for working with String arrays
    //---------------------------------------------------------------------
    /**
     * Copy the given [Collection] into a `String` array.
     *
     * The `Collection` must contain `String` elements only.
     * @param collection the `Collection` to copy
     * (potentially `null` or empty)
     * @return the resulting `String` array
     */
    fun toStringArray(collection: Collection<String>): Array<String> {
        return if (!collection.isEmpty()) collection.toTypedArray() else EMPTY_STRING_ARRAY
    }

    /**
     * Merge the given `String` arrays into one, with overlapping
     * array elements only included once.
     *
     * The order of elements in the original arrays is preserved
     * (with the exception of overlapping elements, which are only
     * included on their first occurrence).
     * @param array1 the first array (can be `null`)
     * @param array2 the second array (can be `null`)
     * @return the new array (`null` if both given arrays were `null`)
     */

    @Deprecated(
        """as of 4.3.15, in favor of manual merging via {@link LinkedHashSet}
	  (with every entry included at most once, even entries within the first array)"""
    )
    fun mergeStringArrays(
        array1: Array<String>,
        array2: Array<String>
    ): Array<String> {
        if (array1.isEmpty()) {
            return array2
        }
        if (array2.isEmpty()) {
            return array1
        }
        val result: MutableList<String> =
            ArrayList(Arrays.asList(*array1))
        for (str in array2) {
            if (!result.contains(str)) {
                result.add(str)
            }
        }
        return toStringArray(result)
    }

    /**
     * Tokenize the given `String` into a `String` array via a
     * [StringTokenizer].
     *
     * The given `delimiters` string can consist of any number of
     * delimiter characters. Each of those characters can be used to separate
     * tokens. A delimiter is always a single character; for multi-character
     * delimiters, consider using [.delimitedListToStringArray].
     * @param str the `String` to tokenize (potentially `null` or empty)
     * @param delimiters the delimiter characters, assembled as a `String`
     * (each of the characters is individually considered as a delimiter)
     * @param trimTokens trim the tokens via [String.trim]
     * @param ignoreEmptyTokens omit empty tokens from the result array
     * (only applies to tokens that are empty after trimming; StringTokenizer
     * will not consider subsequent delimiters as token in the first place).
     * @return an array of the tokens
     * @see java.util.StringTokenizer
     *
     * @see String.trim
     * @see .delimitedListToStringArray
     */
    /**
     * Tokenize the given `String` into a `String` array via a
     * [StringTokenizer].
     *
     * Trims tokens and omits empty tokens.
     *
     * The given `delimiters` string can consist of any number of
     * delimiter characters. Each of those characters can be used to separate
     * tokens. A delimiter is always a single character; for multi-character
     * delimiters, consider using [.delimitedListToStringArray].
     * @param str the `String` to tokenize (potentially `null` or empty)
     * @param delimiters the delimiter characters, assembled as a `String`
     * (each of the characters is individually considered as a delimiter)
     * @return an array of the tokens
     * @see java.util.StringTokenizer
     *
     * @see String.trim
     * @see .delimitedListToStringArray
     */
    @JvmOverloads
    fun tokenizeToStringArray(
        str: String?,
        delimiters: String?,
        trimTokens: Boolean = true,
        ignoreEmptyTokens: Boolean = true
    ): Array<String> {
        if (str == null) {
            return EMPTY_STRING_ARRAY
        }
        val st = StringTokenizer(str, delimiters)
        val tokens: MutableList<String> = ArrayList()
        while (st.hasMoreTokens()) {
            var token = st.nextToken()
            if (trimTokens) {
                token = token.trim { it <= ' ' }
            }
            if (!ignoreEmptyTokens || token.length > 0) {
                tokens.add(token)
            }
        }
        return toStringArray(tokens)
    }
    /**
     * Take a `String` that is a delimited list and convert it into
     * a `String` array.
     *
     * A single `delimiter` may consist of more than one character,
     * but it will still be considered as a single delimiter string, rather
     * than as bunch of potential delimiter characters, in contrast to
     * [.tokenizeToStringArray].
     * @param str the input `String` (potentially `null` or empty)
     * @param delimiter the delimiter between elements (this is a single delimiter,
     * rather than a bunch individual delimiter characters)
     * @param charsToDelete a set of characters to delete; useful for deleting unwanted
     * line breaks: e.g. "\r\n\f" will delete all new lines and line feeds in a `String`
     * @return an array of the tokens in the list
     * @see .tokenizeToStringArray
     */
    /**
     * Take a `String` that is a delimited list and convert it into a
     * `String` array.
     *
     * A single `delimiter` may consist of more than one character,
     * but it will still be considered as a single delimiter string, rather
     * than as bunch of potential delimiter characters, in contrast to
     * [.tokenizeToStringArray].
     * @param str the input `String` (potentially `null` or empty)
     * @param delimiter the delimiter between elements (this is a single delimiter,
     * rather than a bunch individual delimiter characters)
     * @return an array of the tokens in the list
     * @see .tokenizeToStringArray
     */
    @JvmOverloads
    fun delimitedListToStringArray(
        str: String?,
        delimiter: String?,
        charsToDelete: String? = null
    ): Array<String> {
        if (str == null) {
            return EMPTY_STRING_ARRAY
        }
        if (delimiter == null) {
            return arrayOf(str)
        }
        val result: MutableList<String> = ArrayList()
        if (delimiter.isEmpty()) {
            for (i in 0 until str.length) {
                result.add(deleteAny(str.substring(i, i + 1), charsToDelete))
            }
        } else {
            var pos = 0
            var delPos: Int
            while (str.indexOf(delimiter, pos).also { delPos = it } != -1) {
                result.add(deleteAny(str.substring(pos, delPos), charsToDelete))
                pos = delPos + delimiter.length
            }
            if (str.length > 0 && pos <= str.length) {
                // Add rest of String, but not in case of empty input.
                result.add(deleteAny(str.substring(pos), charsToDelete))
            }
        }
        return toStringArray(result)
    }

    /**
     * Convert a comma delimited list (e.g., a row from a CSV file) into an
     * array of strings.
     * @param str the input `String` (potentially `null` or empty)
     * @return an array of strings, or the empty array in case of empty input
     */
    fun commaDelimitedListToStringArray(str: String?): Array<String> {
        return delimitedListToStringArray(str, ",")
    }

    /**
     * Convert a [Collection] to a delimited `String` (e.g. CSV).
     *
     * Useful for `toString()` implementations.
     * @param coll the `Collection` to convert (potentially `null` or empty)
     * @param delim the delimiter to use (typically a ",")
     * @param prefix the `String` to start each element with
     * @param suffix the `String` to end each element with
     * @return the delimited `String`
     */

}