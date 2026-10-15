package com.siosin.bluecon.bluetooth

import kotlin.test.Test
import kotlin.test.assertEquals

class AdvertisingPayloadTest {
    @Test
    fun payloadHexPreservesUnsignedByteValuesAndLeadingZeroes() {
        assertEquals("00 0F 7F 80 FF", byteArrayOf(0, 15, 127, -128, -1).payloadHex())
        assertEquals("", byteArrayOf().payloadHex())
    }
}
