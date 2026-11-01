package com.recall.app.core.ml.tokenizer

import android.content.Context
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.InputStream
import java.io.InputStreamReader
import java.util.regex.Pattern
import java.util.zip.GZIPInputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.Locale

data class ClipTokenizationDiagnostics(
    val normalizedText: String,
    val tokenIds: IntArray,
    val tokenPieces: List<String>,
    val originalTokenCount: Int,
    val truncated: Boolean,
)

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
    private val bpeRanks: Map<Pair<String, String>, Int>,
    private val byteEncoder: Map<Byte, Char>,
    private val contextLength: Int,
) {
    private val cache = ConcurrentHashMap<String, String>()
    private val decoder by lazy { encoder.entries.associate { (piece, id) -> id to piece } }
    private val pattern: Pattern = Pattern.compile(
        "'s|'t|'re|'ve|'m|'ll|'d|[\\p{L}]+|[\\p{N}]|[^\\s\\p{L}\\p{N}]+",
        Pattern.CASE_INSENSITIVE,
    )

    fun encode(text: String): IntArray = pack(tokenize(text).tokenIds)

    fun encodeWithDiagnostics(text: String): ClipTokenizationDiagnostics {
        val tokenization = tokenize(text)
        val tokenIds = pack(tokenization.tokenIds)
        val endIndex = minOf(tokenization.tokenIds.size, contextLength - 2) + 1
        val pieces = tokenIds.mapIndexed { position, id ->
            when {
                position == 0 -> "<SOT>"
                position == endIndex -> "<EOT>"
                position > endIndex -> "<PAD>"
                else -> decoder[id] ?: "<UNKNOWN:$id>"
            }
        }
        return ClipTokenizationDiagnostics(
            normalizedText = tokenization.normalizedText,
            tokenIds = tokenIds,
            tokenPieces = pieces,
            originalTokenCount = tokenization.tokenIds.size,
            truncated = tokenization.tokenIds.size > contextLength - 2,
        )
    }

    private data class Tokenization(val normalizedText: String, val tokenIds: List<Int>)

    private fun tokenize(text: String): Tokenization {
        val bpeTokens = mutableListOf<Int>()
        val cleanedText = text.lowercase(Locale.ROOT).trim()
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

        return Tokenization(cleanedText, bpeTokens)
    }

    private fun pack(bpeTokens: List<Int>): IntArray {
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
        internal const val VOCAB_ASSET_GZIP = "tokenizer/bpe_simple_vocab_16e6.txt.gz"
        internal const val VOCAB_ASSET_PLAIN = "tokenizer/bpe_simple_vocab_16e6.txt"
        private const val DEFAULT_CONTEXT_LENGTH = 77
        internal const val MERGE_COUNT = 48894

        fun create(context: Context, contextLength: Int = DEFAULT_CONTEXT_LENGTH): ClipBpeTokenizer {
            val lines = openVocabulary(context).use { stream ->
                BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readLines()
            }

            return fromVocabularyLines(lines, contextLength)
        }

        internal fun openVocabulary(context: Context): InputStream =
            openVocabulary { name -> context.assets.open(name) }

        internal fun openVocabulary(openAsset: (String) -> InputStream): InputStream {
            // AAPT can unpack .gz assets and package the same vocabulary as .txt.
            val compressed = try {
                openAsset(VOCAB_ASSET_GZIP)
            } catch (_: FileNotFoundException) {
                return openAsset(VOCAB_ASSET_PLAIN)
            }
            return try {
                GZIPInputStream(compressed)
            } catch (error: Throwable) {
                runCatching { compressed.close() }
                throw error
            }
        }

        internal fun fromVocabularyLines(
            lines: List<String>,
            contextLength: Int = DEFAULT_CONTEXT_LENGTH,
        ): ClipBpeTokenizer {
            require(lines.size >= MERGE_COUNT + 1) { "CLIP BPE vocabulary is incomplete" }
            return fromMerges(lines.subList(1, MERGE_COUNT + 1), contextLength)
        }

        internal fun fromMerges(
            mergeLines: List<String>,
            contextLength: Int = DEFAULT_CONTEXT_LENGTH,
        ): ClipBpeTokenizer {
            require(contextLength >= 2) { "CLIP context length must fit start and end tokens" }
            require(mergeLines.size <= MERGE_COUNT) { "Too many CLIP BPE merges" }
            val byteEncoder = buildByteEncoder()

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
                require(parts.size == 2 && parts.all { it.isNotEmpty() }) {
                    "Invalid CLIP BPE merge at line ${rank + 2}"
                }
                vocabTokens.add(parts[0] + parts[1])
                bpeRanks[parts[0] to parts[1]] = rank
            }

            for ((idx, token) in vocabTokens.withIndex()) {
                encoder[token] = idx
            }

            encoder["<|startoftext|>"] = SOT_TOKEN
            encoder["<|endoftext|>"] = EOT_TOKEN
            return ClipBpeTokenizer(encoder, bpeRanks, byteEncoder, contextLength)
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

            val result = LinkedHashMap<Byte, Char>(256)
            for (i in bs.indices) {
                result[bs[i].toByte()] = cs[i].toChar()
            }
            return result
        }
    }
}
