package com.recall.app.core.ml.tokenizer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipBpeTokenizerTest {
    private val tokenizer = ClipBpeTokenizer.fromMerges(emptyList())

    @Test
    fun encode_matchesCanonicalBaseVocabulary() {
        assertContent("a", 320)
        assertContent("1", 272)
        assertContent("!", 256)
        assertContent("кот", 140, 118, 140, 122, 141, 480)
        assertContent("Я", 141, 493)
    }

    @Test
    fun encode_usesRankedMergesWithCanonicalIds() {
        val merged = ClipBpeTokenizer.fromMerges(listOf("c a", "ca t</w>", "d o", "do g</w>"))
        assertArrayEquals(intArrayOf(49406, 320, 513, 49407), merged.encode("A cat").take(4).toIntArray())
        assertArrayEquals(intArrayOf(49406, 515, 49407), merged.encode("dog").take(3).toIntArray())
    }

    @Test
    fun encode_alwaysIncludesSpecialTokensAndZeroPadding() {
        val result = tokenizer.encode("")
        assertEquals(77, result.size)
        assertArrayEquals(intArrayOf(49406, 49407), result.take(2).toIntArray())
        assertArrayEquals(IntArray(75), result.drop(2).toIntArray())
    }

    @Test
    fun encode_truncatesContentAndKeepsEndToken() {
        val short = ClipBpeTokenizer.fromMerges(emptyList(), contextLength = 4)
        assertArrayEquals(intArrayOf(49406, 320, 272, 49407), short.encode("a 1 !"))
    }

    @Test
    fun vocabulary_usesExactlyCanonicalMergeCount() {
        val lines = listOf("#version: 0.2") + List(48894) { "a b" } + "x y</w>"
        val full = ClipBpeTokenizer.fromVocabularyLines(lines)
        // The extra merge would consume the reserved start token ID 49406.
        assertArrayEquals(intArrayOf(49406, 87, 344, 49407), full.encode("xy").take(4).toIntArray())
    }

    @Test(expected = IllegalArgumentException::class)
    fun vocabulary_rejectsIncompleteAsset() {
        ClipBpeTokenizer.fromVocabularyLines(listOf("#version: 0.2", "a b"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun encode_rejectsContextWithoutRoomForSpecialTokens() {
        ClipBpeTokenizer.fromMerges(emptyList(), contextLength = 1)
    }

    private fun assertContent(text: String, vararg content: Int) {
        val expected = intArrayOf(49406) + content + intArrayOf(49407)
        assertArrayEquals(text, expected, tokenizer.encode(text).take(expected.size).toIntArray())
    }
}
