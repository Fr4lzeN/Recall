package com.recall.app.core.ml.tokenizer

import android.content.Context
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream

/**
 * Kotlin port of OpenAI CLIP's simple BPE tokenizer.
 *
 * Loads the standard `bpe_simple_vocab_16e6.txt.gz` vocabulary and encodes
 * text into a fixed-length IntArray of token IDs suitable for CLIP text encoders.
 *
 * Reference: https://github.com/openai/CLIP/blob/main/clip/simple_tokenizer.py
 */
class ClipBpeTokenizer private constructor(
    private val encoder: Map<String, Int>,
    private val decoder: Map<Int, String>,
    private val bpeRanks: Map<Pair<String, String>, Int>,
    private val byteEncoder: Map<Byte, Char>,
    private val contextLength: Int,
) {
    private val cache = HashMap<String, String>()
    private val pattern: Pattern = Pattern.compile(
        "'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}]+",
        Pattern.CASE_INSENSITIVE,
    )

    fun encode(text: String): IntArray {
        val bpeTokens = mutableListOf<Int>()
        val cleanedText = text.lowercase().trim()
        val matcher = pattern.matcher(cleanedText)

        while (matcher.find()) {
            val token = matcher.group()
            val encoded = buildString {
                for (b in token.toByteArray(Charsets.UTF_8)) {
                    append(byteEncoder[b] ?: '?')
                }
            }
            for (bpeToken in bpe(encoded).split(" ")) {
                val id = encoder[bpeToken]
                if (id != null) {
                    bpeTokens.add(id)
                }
            }
        }

        val result = IntArray(contextLength)
        result[0] = SOT_TOKEN
        val maxBpeTokens = contextLength - 2
        val tokenCount = minOf(bpeTokens.size, maxBpeTokens)
        for (i in 0 until tokenCount) {
            result[i + 1] = bpeTokens[i]
        }
        result[tokenCount + 1] = EOT_TOKEN
        return result
    }

    private fun bpe(token: String): String {
        cache[token]?.let { return it }

        var word = token.toList().mapIndexed { i, c ->
            if (i == token.length - 1) c.toString() + END_WORD else c.toString()
        }

        if (word.size == 1) {
            val result = word[0]
            cache[token] = result
            return result
        }

        while (true) {
            val pairs = getPairs(word)
            if (pairs.isEmpty()) break

            val bigram = pairs.minByOrNull { (a, b) ->
                bpeRanks[a to b] ?: Int.MAX_VALUE
            } ?: break

            if (bpeRanks[bigram.first to bigram.second] == null) break

            val (first, second) = bigram
            val newWord = mutableListOf<String>()
            var i = 0
            while (i < word.size) {
                val j = indexOf(word, first, i)
                if (j == -1) {
                    newWord.addAll(word.subList(i, word.size))
                    break
                }
                newWord.addAll(word.subList(i, j))
                i = j

                if (i < word.size - 1 && word[i] == first && word[i + 1] == second) {
                    newWord.add(first + second)
                    i += 2
                } else {
                    newWord.add(word[i])
                    i += 1
                }
            }
            word = newWord
            if (word.size == 1) break
        }

        val result = word.joinToString(" ")
        cache[token] = result
        return result
    }

    private fun indexOf(word: List<String>, target: String, start: Int): Int {
        for (i in start until word.size) {
            if (word[i] == target) return i
        }
        return -1
    }

    private fun getPairs(word: List<String>): Set<Pair<String, String>> {
        val pairs = LinkedHashSet<Pair<String, String>>()
        for (i in 0 until word.size - 1) {
            pairs.add(word[i] to word[i + 1])
        }
        return pairs
    }

    companion object {
        const val SOT_TOKEN = 49406
        const val EOT_TOKEN = 49407
        private const val END_WORD = "</w>"
        private const val VOCAB_ASSET = "tokenizer/bpe_simple_vocab_16e6.txt.gz"
        private const val DEFAULT_CONTEXT_LENGTH = 77

        fun create(context: Context, contextLength: Int = DEFAULT_CONTEXT_LENGTH): ClipBpeTokenizer {
            val byteEncoder = buildByteEncoder()
            val (encoder, bpeRanks) = loadVocab(context, byteEncoder)
            val decoder = encoder.entries.associate { (k, v) -> v to k }
            return ClipBpeTokenizer(encoder, decoder, bpeRanks, byteEncoder, contextLength)
        }

        private fun loadVocab(
            context: Context,
            byteEncoder: Map<Byte, Char>,
        ): Pair<Map<String, Int>, Map<Pair<String, String>, Int>> {
            val lines = context.assets.open(VOCAB_ASSET).use { raw ->
                GZIPInputStream(raw).use { gzip ->
                    BufferedReader(InputStreamReader(gzip, Charsets.UTF_8)).readLines()
                }
            }

            val mergeLines = lines.subList(1, 48895 + 1)

            val encoder = HashMap<String, Int>(49408)

            val vocabTokens = mutableListOf<String>()
            for ((_, c) in byteEncoder) {
                vocabTokens.add(c.toString())
            }
            for ((_, c) in byteEncoder) {
                vocabTokens.add(c.toString() + END_WORD)
            }

            val bpeRanks = HashMap<Pair<String, String>, Int>(mergeLines.size)
            for ((rank, line) in mergeLines.withIndex()) {
                val parts = line.split(" ")
                if (parts.size == 2) {
                    vocabTokens.add(parts[0] + parts[1])
                    bpeRanks[parts[0] to parts[1]] = rank
                }
            }

            vocabTokens.add("<|startoftext|>")
            vocabTokens.add("<|endoftext|>")

            for ((idx, token) in vocabTokens.withIndex()) {
                encoder[token] = idx
            }

            return encoder to bpeRanks
        }

        private fun buildByteEncoder(): Map<Byte, Char> {
            val bs = mutableListOf<Int>()
            val cs = mutableListOf<Int>()

            for (b in '!'.code..'~'.code) { bs.add(b); cs.add(b) }
            for (b in '¡'.code..'¬'.code) { bs.add(b); cs.add(b) }
            for (b in '®'.code..'ÿ'.code) { bs.add(b); cs.add(b) }

            var n = 0
            for (b in 0..255) {
                if (b !in bs) {
                    bs.add(b)
                    cs.add(256 + n)
                    n++
                }
            }

            val result = HashMap<Byte, Char>(256)
            for (i in bs.indices) {
                result[bs[i].toByte()] = cs[i].toChar()
            }
            return result
        }
    }
}
