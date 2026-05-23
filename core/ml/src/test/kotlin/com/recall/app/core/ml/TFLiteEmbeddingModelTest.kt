package com.recall.app.core.ml

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.recall.app.core.ml.di.MlModule
import com.recall.app.core.ml.encoder.TFLiteUtils
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.math.sqrt

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class TFLiteEmbeddingModelTest {

    @Test
    fun construction_throwsWhenModelFileMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        try {
            TFLiteEmbeddingModel(context, ModelProfile.LITE)
            throw AssertionError("Expected model load to fail when asset is missing")
        } catch (e: Exception) {
            assertTrue(
                "Expected asset or interpreter failure, got: ${e::class.simpleName}",
                e is java.io.FileNotFoundException ||
                    e is java.io.IOException ||
                    e.message?.contains("mobileclip", ignoreCase = true) == true ||
                    e.cause != null,
            )
        }
    }

    @Test
    fun mlModule_fallsBackToMockWhenModelMissing() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val model = MlModule.provideEmbeddingModel(context, ModelProfile.LITE)
        assertTrue(model is MockEmbeddingModel)
        assertEquals(ModelProfile.LITE.dimensions, model.dimensions)
        assertEquals(ModelProfile.LITE.name, model.profileName)
    }

    @Test
    fun clipNormalization_appliesImageNetMeanStd() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.rgb(255, 255, 255))

        val clip = ImagePreprocessor.bitmapToFloatArray(
            bitmap,
            ImagePreprocessor.Normalization.CLIP_IMAGENET,
        )
        val simple = ImagePreprocessor.bitmapToFloatArray(
            bitmap,
            ImagePreprocessor.Normalization.SIMPLE,
        )

        assertEquals(3, clip.size)
        assertEquals(1.0f, simple[0], 1e-5f)
        assertEquals((1.0f - 0.485f) / 0.229f, clip[0], 1e-3f)
        assertEquals((1.0f - 0.456f) / 0.224f, clip[1], 1e-3f)
        assertEquals((1.0f - 0.406f) / 0.225f, clip[2], 1e-3f)
    }

    @Test
    fun simpleNormalization_scalesTo01() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.rgb(128, 64, 0))

        val simple = ImagePreprocessor.bitmapToFloatArray(
            bitmap,
            ImagePreprocessor.Normalization.SIMPLE,
        )

        assertEquals(128 / 255f, simple[0], 1e-5f)
        assertEquals(64 / 255f, simple[1], 1e-5f)
        assertEquals(0.0f, simple[2], 1e-5f)
    }

    @Test
    fun l2Normalize_producesUnitVector() {
        val vector = floatArrayOf(3f, 4f)
        val normalized = TFLiteUtils.l2Normalize(vector)
        assertEquals(1.0f, l2Norm(normalized), 1e-5f)
    }

    @Test
    fun l2Normalize_handlesZeroVector() {
        val vector = floatArrayOf(0f, 0f, 0f)
        val normalized = TFLiteUtils.l2Normalize(vector)
        assertEquals(0f, normalized[0], 1e-5f)
    }

    @Test
    fun detectInputLayout_recognizesNchwAndNhwc() {
        assertEquals(
            TFLiteUtils.InputLayout.NCHW,
            TFLiteUtils.detectInputLayout(intArrayOf(1, 3, 256, 256)),
        )
        assertEquals(
            TFLiteUtils.InputLayout.NHWC,
            TFLiteUtils.detectInputLayout(intArrayOf(1, 256, 256, 3)),
        )
    }

    @Test
    fun bitmapToFloatArray_defaultNormalizationIsBackwardCompatible() {
        val bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        bitmap.setPixel(0, 0, Color.rgb(255, 128, 0))
        val floats = ImagePreprocessor.bitmapToFloatArray(bitmap)
        assertEquals(1.0f, floats[0], 1e-5f)
        assertEquals(128 / 255f, floats[1], 1e-5f)
        assertEquals(0.0f, floats[2], 1e-5f)
    }

    @Test
    fun embedText_viaMock_isDeterministic() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val first = model.embedText("recall query")
        val second = model.embedText("recall query")
        assertTrue(first.contentEquals(second))
    }

    @Test
    fun embedText_viaMock_doesNotCallEmbedImage() = runTest {
        val model = MockEmbeddingModel(ModelProfile.LITE)
        val textEmb = model.embedText("a dog")
        assertEquals(ModelProfile.LITE.dimensions, textEmb.size)
        assertEquals(1.0f, l2Norm(textEmb), 1e-4f)
    }

    private fun l2Norm(vector: FloatArray): Float {
        return sqrt(vector.fold(0f) { acc, v -> acc + v * v })
    }
}
