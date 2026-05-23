package com.recall.app.core.ml

import android.graphics.Bitmap
import android.graphics.Color
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Visually-aware mock embedding model for demo purposes.
 *
 * Images: extracts a compact visual descriptor from actual pixel data (dominant colors,
 * brightness, hue histogram, saturation, sky-likelihood).
 *
 * Text: maps known concept keywords to expected visual descriptors so that,
 * e.g., "sunset" is close to warm-toned photos in cosine space.
 *
 * Both descriptors are projected through a fixed random matrix into the full
 * embedding space and L2-normalised, giving plausible similarity rankings
 * without a real neural network.
 */
class MockEmbeddingModel(
    private val profile: ModelProfile = ModelProfile.LITE,
) : EmbeddingModel {

    override val dimensions: Int = profile.dimensions
    override val profileName: String = profile.name

    private val projectionMatrix: Array<FloatArray> by lazy { buildProjection() }

    override suspend fun embedImage(bitmap: Bitmap): FloatArray {
        val features = extractVisualFeatures(bitmap)
        return projectAndNormalize(features)
    }

    override suspend fun embedText(text: String): FloatArray {
        val features = textToVisualFeatures(text.lowercase().trim())
        return projectAndNormalize(features)
    }

    override fun close() { /* no-op */ }

    // ── Image feature extraction ────────────────────────────────────────

    private fun extractVisualFeatures(bitmap: Bitmap): FloatArray {
        val features = FloatArray(FEATURE_DIMS)
        val w = bitmap.width
        val h = bitmap.height
        val step = maxOf(1, minOf(w, h) / SAMPLE_GRID)

        var sumR = 0.0; var sumG = 0.0; var sumB = 0.0
        var count = 0
        val hueHist = DoubleArray(HUE_BUCKETS)
        var sumSat = 0.0
        var topBright = 0.0; var botBright = 0.0
        var topCount = 0; var botCount = 0
        var topBlue = 0.0

        for (y in 0 until h step step) {
            for (x in 0 until w step step) {
                val px = bitmap.getPixel(x, y)
                val a = Color.alpha(px)
                if (a < 128) continue

                val r = Color.red(px) / 255.0
                val g = Color.green(px) / 255.0
                val b = Color.blue(px) / 255.0

                sumR += r; sumG += g; sumB += b; count++

                val hsv = floatArrayOf(0f, 0f, 0f)
                Color.RGBToHSV(Color.red(px), Color.green(px), Color.blue(px), hsv)
                hueHist[((hsv[0] / 30.0).toInt()).coerceIn(0, HUE_BUCKETS - 1)] += 1.0
                sumSat += hsv[1]

                val brightness = (r + g + b) / 3.0
                if (y < h / 2) { topBright += brightness; topBlue += b; topCount++ }
                else { botBright += brightness; botCount++ }
            }
        }

        if (count == 0) return features

        val avgR = (sumR / count).toFloat()
        val avgG = (sumG / count).toFloat()
        val avgB = (sumB / count).toFloat()

        var idx = 0
        features[idx++] = avgR                                          // 0
        features[idx++] = avgG                                          // 1
        features[idx++] = avgB                                          // 2
        features[idx++] = (sumR + sumG + sumB).toFloat() / (3f * count) // 3  brightness
        features[idx++] = avgR - avgB                                   // 4  warmth
        features[idx++] = avgG - (avgR + avgB) / 2f                     // 5  greenness
        features[idx++] = (sumSat / count).toFloat()                    // 6  saturation
        features[idx++] = if (topCount > 0) (topBlue / topCount).toFloat() else 0f  // 7  sky
        features[idx++] = run {                                         // 8  vertical gradient
            val t = if (topCount > 0) topBright / topCount else 0.0
            val b2 = if (botCount > 0) botBright / botCount else 0.0
            (t - b2).toFloat()
        }

        val hueTotal = hueHist.sum().coerceAtLeast(1.0)
        for (i in 0 until HUE_BUCKETS) {
            features[idx++] = (hueHist[i] / hueTotal).toFloat()        // 9..20
        }

        return features
    }

    // ── Text → visual features ──────────────────────────────────────────

    private fun textToVisualFeatures(text: String): FloatArray {
        val accum = FloatArray(FEATURE_DIMS)
        var hits = 0

        for ((keywords, proto) in CONCEPTS) {
            if (keywords.any { it in text }) {
                for (i in accum.indices) accum[i] += proto[i]
                hits++
            }
        }

        if (hits > 0) {
            for (i in accum.indices) accum[i] /= hits
            return accum
        }

        val rng = Random(text.hashCode())
        return FloatArray(FEATURE_DIMS) { rng.nextFloat() * 0.6f - 0.3f }
    }

    // ── Projection & normalisation ──────────────────────────────────────

    private fun buildProjection(): Array<FloatArray> {
        val rng = Random(PROJECTION_SEED)
        val scale = 1f / sqrt(FEATURE_DIMS.toFloat())
        return Array(FEATURE_DIMS) {
            FloatArray(dimensions) { (rng.nextFloat() * 2f - 1f) * scale }
        }
    }

    private fun projectAndNormalize(features: FloatArray): FloatArray {
        val out = FloatArray(dimensions)
        for (j in 0 until dimensions) {
            var s = 0f
            for (i in 0 until FEATURE_DIMS) s += features[i] * projectionMatrix[i][j]
            out[j] = s
        }
        val norm = sqrt(out.fold(0f) { acc, v -> acc + v * v })
        if (norm > 0f) for (i in out.indices) out[i] /= norm
        return out
    }

    // ── Constants ───────────────────────────────────────────────────────

    companion object {
        private const val HUE_BUCKETS = 12
        private const val FEATURE_DIMS = 9 + HUE_BUCKETS   // 21
        private const val SAMPLE_GRID = 16
        private const val PROJECTION_SEED = 42

        //  Feature layout:
        //  0  avgR      1  avgG      2  avgB       3  brightness
        //  4  warmth    5  greenness 6  saturation  7  skyScore
        //  8  vertGrad  9..20  hue histogram (12 × 30°)

        @Suppress("LongMethod")
        private val CONCEPTS: List<Pair<List<String>, FloatArray>> = listOf(
            // ── Sunset / sunrise / golden hour ──
            listOf("sunset", "sunrise", "golden", "закат", "рассвет", "dawn", "dusk") to
                floatArrayOf(
                    0.80f, 0.40f, 0.20f, 0.47f, 0.70f, -0.10f, 0.85f, 0.25f, 0.25f,
                    0.30f, 0.35f, 0.20f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.05f, 0.05f,
                ),
            // ── Sky / clouds ──
            listOf("sky", "cloud", "clouds", "небо", "облака") to
                floatArrayOf(
                    0.40f, 0.50f, 0.85f, 0.58f, -0.45f, 0.00f, 0.50f, 0.85f, 0.30f,
                    0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.15f, 0.50f, 0.30f, 0.00f, 0.00f, 0.05f,
                ),
            // ── Ocean / water ──
            listOf("ocean", "sea", "water", "lake", "river", "океан", "море", "вода", "озеро", "река") to
                floatArrayOf(
                    0.15f, 0.35f, 0.75f, 0.42f, -0.60f, 0.00f, 0.55f, 0.25f, -0.05f,
                    0.00f, 0.00f, 0.00f, 0.05f, 0.10f, 0.15f, 0.30f, 0.30f, 0.10f, 0.00f, 0.00f, 0.00f,
                ),
            // ── Forest / nature / plants ──
            listOf("forest", "tree", "trees", "nature", "plant", "garden", "park", "grass",
                "лес", "природа", "дерево", "зелень", "парк", "трава", "green") to
                floatArrayOf(
                    0.25f, 0.55f, 0.20f, 0.33f, 0.05f, 0.35f, 0.65f, 0.20f, -0.10f,
                    0.00f, 0.05f, 0.10f, 0.35f, 0.30f, 0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.05f,
                ),
            // ── Food / drink ──
            listOf("food", "meal", "coffee", "cake", "pizza", "sushi", "dish", "lunch", "dinner",
                "breakfast", "еда", "кофе", "торт", "пицца", "завтрак", "обед", "ужин") to
                floatArrayOf(
                    0.55f, 0.40f, 0.30f, 0.42f, 0.25f, 0.00f, 0.65f, 0.05f, 0.00f,
                    0.15f, 0.25f, 0.25f, 0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.10f, 0.10f,
                ),
            // ── Night / dark ──
            listOf("night", "dark", "evening", "ночь", "темно", "вечер", "тёмный") to
                floatArrayOf(
                    0.10f, 0.08f, 0.15f, 0.11f, -0.05f, -0.02f, 0.30f, 0.05f, 0.00f,
                    0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.05f, 0.15f, 0.35f, 0.15f, 0.10f, 0.05f, 0.10f,
                ),
            // ── Snow / winter ──
            listOf("snow", "winter", "ice", "frost", "снег", "зима", "лёд", "мороз") to
                floatArrayOf(
                    0.90f, 0.90f, 0.92f, 0.91f, -0.02f, -0.02f, 0.05f, 0.40f, 0.00f,
                    0.00f, 0.00f, 0.00f, 0.00f, 0.10f, 0.20f, 0.30f, 0.30f, 0.10f, 0.00f, 0.00f, 0.00f,
                ),
            // ── Flowers ──
            listOf("flower", "flowers", "bloom", "rose", "tulip",
                "цветы", "цветок", "роза", "тюльпан") to
                floatArrayOf(
                    0.60f, 0.35f, 0.50f, 0.48f, 0.10f, -0.10f, 0.85f, 0.15f, -0.10f,
                    0.15f, 0.10f, 0.05f, 0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.10f, 0.20f, 0.15f, 0.10f,
                ),
            // ── Cat / kitten ──
            listOf("cat", "kitten", "кот", "кошка", "котёнок", "котик") to
                floatArrayOf(
                    0.55f, 0.45f, 0.35f, 0.45f, 0.20f, 0.02f, 0.40f, 0.10f, 0.00f,
                    0.10f, 0.20f, 0.20f, 0.10f, 0.05f, 0.05f, 0.05f, 0.05f, 0.00f, 0.00f, 0.10f, 0.10f,
                ),
            // ── Dog / puppy ──
            listOf("dog", "puppy", "собака", "щенок", "пёс") to
                floatArrayOf(
                    0.50f, 0.40f, 0.30f, 0.40f, 0.20f, 0.02f, 0.45f, 0.15f, 0.00f,
                    0.10f, 0.15f, 0.25f, 0.15f, 0.10f, 0.05f, 0.05f, 0.05f, 0.00f, 0.00f, 0.05f, 0.05f,
                ),
            // ── City / architecture ──
            listOf("city", "building", "street", "architecture", "urban", "skyscraper",
                "город", "здание", "улица", "архитектура") to
                floatArrayOf(
                    0.45f, 0.42f, 0.45f, 0.44f, 0.00f, -0.02f, 0.25f, 0.30f, 0.05f,
                    0.05f, 0.10f, 0.10f, 0.05f, 0.05f, 0.10f, 0.15f, 0.20f, 0.10f, 0.05f, 0.03f, 0.02f,
                ),
            // ── Car / transport ──
            listOf("car", "vehicle", "road", "highway",
                "машина", "автомобиль", "дорога", "транспорт") to
                floatArrayOf(
                    0.40f, 0.40f, 0.40f, 0.40f, 0.00f, 0.00f, 0.35f, 0.25f, 0.05f,
                    0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.10f, 0.05f, 0.03f, 0.05f, 0.07f,
                ),
            // ── Beach / sand ──
            listOf("beach", "sand", "tropical", "пляж", "песок", "тропики") to
                floatArrayOf(
                    0.70f, 0.60f, 0.45f, 0.58f, 0.25f, 0.05f, 0.45f, 0.45f, 0.10f,
                    0.05f, 0.15f, 0.30f, 0.15f, 0.10f, 0.05f, 0.05f, 0.10f, 0.05f, 0.00f, 0.00f, 0.00f,
                ),
            // ── Mountain ──
            listOf("mountain", "mountains", "hill", "peak", "summit",
                "горы", "гора", "холм", "вершина") to
                floatArrayOf(
                    0.40f, 0.45f, 0.55f, 0.47f, -0.15f, 0.08f, 0.40f, 0.50f, 0.20f,
                    0.00f, 0.05f, 0.10f, 0.15f, 0.20f, 0.15f, 0.15f, 0.15f, 0.05f, 0.00f, 0.00f, 0.00f,
                ),
            // ── People / portrait ──
            listOf("person", "people", "face", "portrait", "selfie", "smile",
                "человек", "люди", "лицо", "портрет", "селфи", "улыбка") to
                floatArrayOf(
                    0.60f, 0.45f, 0.40f, 0.48f, 0.20f, 0.00f, 0.45f, 0.10f, 0.00f,
                    0.10f, 0.20f, 0.15f, 0.10f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.05f, 0.10f, 0.05f,
                ),
            // ── Autumn / fall ──
            listOf("autumn", "fall", "leaves", "осень", "листья", "листопад") to
                floatArrayOf(
                    0.70f, 0.45f, 0.15f, 0.43f, 0.55f, 0.10f, 0.75f, 0.15f, 0.05f,
                    0.20f, 0.30f, 0.25f, 0.10f, 0.05f, 0.00f, 0.00f, 0.00f, 0.00f, 0.00f, 0.05f, 0.05f,
                ),
        )
    }
}
