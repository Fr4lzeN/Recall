package com.recall.app.core.ml.tokenizer

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipTokenizationDiagnosticsTest {
    @Test
    fun diagnosticsUseExactlyTheSameIdsAsOrdinaryEnglishAndRussianEncoding() {
        val tokenizer = ClipBpeTokenizer.fromMerges(listOf("c a", "ca t</w>"))
        for (query in listOf("A cat", "Кот на пляже", "семья на отдыхе", "a 1 !")) {
            val diagnostic = tokenizer.encodeWithDiagnostics(query)
            assertArrayEquals(query, tokenizer.encode(query), diagnostic.tokenIds)
            assertEquals(77, diagnostic.tokenPieces.size)
            assertFalse(diagnostic.truncated)
        }
        val cat = tokenizer.encodeWithDiagnostics("A cat")
        assertEquals("a cat", cat.normalizedText)
        assertEquals(listOf("<SOT>", "a</w>", "cat</w>", "<EOT>"), cat.tokenPieces.take(4))
        assertEquals(2, cat.originalTokenCount)
    }

    @Test
    fun russianDiagnosticPreservesTheActualNormalizedQueryAndByteTokenCount() {
        val tokenizer = ClipBpeTokenizer.fromMerges(emptyList())
        val diagnostic = tokenizer.encodeWithDiagnostics("  КОТ  ")
        assertEquals("кот", diagnostic.normalizedText)
        assertArrayEquals(intArrayOf(49406, 140, 118, 140, 122, 141, 480, 49407), diagnostic.tokenIds.take(8).toIntArray())
        assertEquals(6, diagnostic.originalTokenCount)
        assertEquals("<EOT>", diagnostic.tokenPieces[7])
    }

    @Test
    fun zeroIdInContentIsNotMislabelledAsPadding() {
        val tokenizer = ClipBpeTokenizer.fromMerges(emptyList())
        val diagnostic = tokenizer.encodeWithDiagnostics("!?")
        assertEquals(0, diagnostic.tokenIds[1])
        assertEquals("!", diagnostic.tokenPieces[1])
        assertEquals("<SOT>", diagnostic.tokenPieces.first())
        assertEquals("<EOT>", diagnostic.tokenPieces[3])
        assertTrue(diagnostic.tokenPieces.drop(4).all { it == "<PAD>" })
    }

    @Test
    fun truncationReportsOriginalContentLengthAndKeepsEndToken() {
        val tokenizer = ClipBpeTokenizer.fromMerges(emptyList(), contextLength = 4)
        val diagnostic = tokenizer.encodeWithDiagnostics("a 1 !")
        assertArrayEquals(intArrayOf(49406, 320, 272, 49407), diagnostic.tokenIds)
        assertEquals(listOf("<SOT>", "a</w>", "1</w>", "<EOT>"), diagnostic.tokenPieces)
        assertEquals(3, diagnostic.originalTokenCount)
        assertTrue(diagnostic.truncated)
    }

    @Test
    fun contentThatExactlyFitsIsNotTruncatedAndEmptyInputIsClearlyPadded() {
        val tokenizer = ClipBpeTokenizer.fromMerges(emptyList(), contextLength = 4)
        assertFalse(tokenizer.encodeWithDiagnostics("a 1").truncated)
        val empty = tokenizer.encodeWithDiagnostics("   ")
        assertEquals("", empty.normalizedText)
        assertEquals(0, empty.originalTokenCount)
        assertEquals(listOf("<SOT>", "<EOT>", "<PAD>", "<PAD>"), empty.tokenPieces)
    }

    @Test
    fun returnedSnapshotsAreNotReusedByLaterQueries() {
        val tokenizer = ClipBpeTokenizer.fromMerges(emptyList())
        val first = tokenizer.encodeWithDiagnostics("a")
        first.tokenIds[1] = -123
        val next = tokenizer.encodeWithDiagnostics("a")
        assertEquals(320, next.tokenIds[1])
        assertEquals("a</w>", next.tokenPieces[1])
    }
}
