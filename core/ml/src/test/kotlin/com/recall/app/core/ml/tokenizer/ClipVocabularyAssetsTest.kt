package com.recall.app.core.ml.tokenizer

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.FileNotFoundException
import java.util.zip.GZIPOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipVocabularyAssetsTest {
    private val vocabulary = (listOf("#version: 0.2") + List(48894) { "a b" })
        .joinToString("\n").toByteArray(Charsets.UTF_8)

    @Test
    fun plainAndGzipAssetsProduceIdenticalTokensAndFingerprintInput() {
        val gzip = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(vocabulary) }
        }.toByteArray()
        val plainBytes = readVocabulary(mapOf(ClipBpeTokenizer.VOCAB_ASSET_PLAIN to vocabulary))
        val gzipBytes = readVocabulary(mapOf(ClipBpeTokenizer.VOCAB_ASSET_GZIP to gzip))
        assertArrayEquals(vocabulary, plainBytes)
        assertArrayEquals(plainBytes, gzipBytes)
        val plain = ClipBpeTokenizer.fromVocabularyLines(plainBytes.toString(Charsets.UTF_8).lines())
        val compressed = ClipBpeTokenizer.fromVocabularyLines(gzipBytes.toString(Charsets.UTF_8).lines())
        for (text in listOf("a 1 !", "кот", "семья на отдыхе")) {
            assertArrayEquals(text, plain.encode(text), compressed.encode(text))
        }
    }

    @Test
    fun prefersGzipWhenBothPackagedNamesExist() {
        val gzip = ByteArrayOutputStream().also { output ->
            GZIPOutputStream(output).use { it.write(vocabulary) }
        }.toByteArray()
        val opened = mutableListOf<String>()
        ClipBpeTokenizer.openVocabulary { name ->
            opened.add(name)
            ByteArrayInputStream(if (name == ClipBpeTokenizer.VOCAB_ASSET_GZIP) gzip else byteArrayOf())
        }.use { assertArrayEquals(vocabulary, it.readBytes()) }
        assertEquals(listOf(ClipBpeTokenizer.VOCAB_ASSET_GZIP), opened)
    }

    @Test
    fun corruptGzipIsClosedAndReportedInsteadOfSilentlyUsingPlain() {
        var closed = false
        var plainOpened = false
        val corrupt = object : ByteArrayInputStream(byteArrayOf(1, 2, 3)) {
            override fun close() {
                closed = true
                super.close()
            }
        }
        try {
            ClipBpeTokenizer.openVocabulary { name ->
                if (name == ClipBpeTokenizer.VOCAB_ASSET_GZIP) corrupt else {
                    plainOpened = true
                    ByteArrayInputStream(vocabulary)
                }
            }
            throw AssertionError("Corrupt gzip asset must fail")
        } catch (expected: java.io.IOException) {
            assertTrue(closed)
            assertTrue(!plainOpened)
        }
    }

    private fun readVocabulary(assets: Map<String, ByteArray>): ByteArray =
        ClipBpeTokenizer.openVocabulary { name ->
            ByteArrayInputStream(assets[name] ?: throw FileNotFoundException(name))
        }.use { it.readBytes() }
}
