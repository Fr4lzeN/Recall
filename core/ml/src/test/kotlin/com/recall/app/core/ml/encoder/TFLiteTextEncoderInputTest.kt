package com.recall.app.core.ml.encoder

import org.junit.Assert.assertArrayEquals
import org.junit.Test
import org.tensorflow.lite.DataType

class TFLiteTextEncoderInputTest {
    @Test
    fun attentionMaskIncludesValidZeroTokenBeforeEnd() {
        assertArrayEquals(
            intArrayOf(1, 1, 1, 0),
            TFLiteTextEncoder.attentionMask(intArrayOf(49406, 0, 49407, 0)),
        )
    }

    @Test
    fun tokenInputUsesBatchDimensionAndDeclaredInt64Type() {
        val packed = TFLiteTextEncoder.packTokens(intArrayOf(49406, 320, 49407), DataType.INT64)
        @Suppress("UNCHECKED_CAST")
        assertArrayEquals(longArrayOf(49406, 320, 49407), (packed as Array<LongArray>).single())
    }
}
