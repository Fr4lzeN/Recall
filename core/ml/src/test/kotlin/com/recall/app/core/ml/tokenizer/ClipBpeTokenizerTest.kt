package com.recall.app.core.ml.tokenizer

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ClipBpeTokenizerTest {

    private var tokenizer: ClipBpeTokenizer? = null

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        tokenizer = try {
            ClipBpeTokenizer.create(context)
        } catch (_: Exception) {
            null
        }
    }

    @Test
    fun encode_outputLength_isContextLength() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val result = tok.encode("a dog")
        assertEquals(77, result.size)
    }

    @Test
    fun encode_startsWithSotToken() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val result = tok.encode("a dog")
        assertEquals(ClipBpeTokenizer.SOT_TOKEN, result[0])
    }

    @Test
    fun encode_hasEotAfterTokens() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val result = tok.encode("a dog")
        val eotIndex = result.indexOfFirst { it == ClipBpeTokenizer.EOT_TOKEN }
        assert(eotIndex > 0) { "EOT token should be present after SOT + content" }
    }

    @Test
    fun encode_padsWithZeros() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val result = tok.encode("a")
        val eotIndex = result.indexOf(ClipBpeTokenizer.EOT_TOKEN)
        for (i in eotIndex + 1 until 77) {
            assertEquals("Position $i should be padded with 0", 0, result[i])
        }
    }

    @Test
    fun encode_emptyString_hasSotAndEot() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val result = tok.encode("")
        assertEquals(ClipBpeTokenizer.SOT_TOKEN, result[0])
        assertEquals(ClipBpeTokenizer.EOT_TOKEN, result[1])
        for (i in 2 until 77) {
            assertEquals(0, result[i])
        }
    }

    @Test
    fun encode_isDeterministic() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val first = tok.encode("a photo of a sunset over the ocean")
        val second = tok.encode("a photo of a sunset over the ocean")
        assertArrayEquals(first, second)
    }

    @Test
    fun encode_differentTexts_produceDifferentTokens() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val dog = tok.encode("a dog")
        val cat = tok.encode("a cat")
        assert(!dog.contentEquals(cat)) { "Different texts should produce different tokens" }
    }

    @Test
    fun encode_longText_truncatesTo77() {
        val tok = tokenizer ?: run {
            assumeTrue("BPE vocab asset not available, skipping", false)
            return
        }
        val longText = "word ".repeat(200)
        val result = tok.encode(longText)
        assertEquals(77, result.size)
        assertEquals(ClipBpeTokenizer.SOT_TOKEN, result[0])
    }
}
