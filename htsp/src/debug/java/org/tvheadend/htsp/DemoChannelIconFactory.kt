package org.tvheadend.htsp

import java.io.ByteArrayOutputStream
import java.util.zip.Adler32
import java.util.zip.CRC32

/**
 * Generates tiny, valid PNG images for the demo channel icons served by [FakeTvheadendServer].
 *
 * The encoding is done by hand (uncompressed "stored" deflate blocks plus CRC32/Adler32) instead of
 * through `android.graphics.Bitmap`, so the fake server produces identical, real PNG bytes both on
 * the device and under Robolectric.
 */
internal object DemoChannelIconFactory {

    private const val WHITE = 0xFFFFFF

    private val PNG_SIGNATURE = byteArrayOf(
        0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
    )

    /**
     * Renders a solid [color] badge with a white border and an inner white square, standing in for a
     * real broadcaster logo.
     *
     * @param color the packed (A)RGB colour of the badge background
     * @param size  the edge length of the (square) image in pixels
     */
    fun badge(color: Int, size: Int = 32): ByteArray {
        val pixels = IntArray(size * size)
        val border = size / 8
        val inner = size / 2
        val innerStart = (size - inner) / 2
        val innerEnd = innerStart + inner

        for (y in 0 until size) {
            for (x in 0 until size) {
                val onBorder = x < border || y < border || x >= size - border || y >= size - border
                val inInner = x in innerStart until innerEnd && y in innerStart until innerEnd
                pixels[y * size + x] = if (onBorder || inInner) WHITE else color
            }
        }
        return encodePng(pixels, size, size)
    }

    /** Encodes a packed (A)RGB pixel array as an 8-bit truecolour (RGB) PNG. */
    private fun encodePng(pixels: IntArray, width: Int, height: Int): ByteArray {
        // Raw image data: each scanline is a filter byte (0 = None) followed by the RGB triples.
        val raw = ByteArray(height * (1 + width * 3))
        var pos = 0
        for (y in 0 until height) {
            raw[pos++] = 0
            for (x in 0 until width) {
                val pixel = pixels[y * width + x]
                raw[pos++] = (pixel shr 16 and 0xFF).toByte()
                raw[pos++] = (pixel shr 8 and 0xFF).toByte()
                raw[pos++] = (pixel and 0xFF).toByte()
            }
        }

        val out = ByteArrayOutputStream()
        out.write(PNG_SIGNATURE, 0, PNG_SIGNATURE.size)
        writeChunk(out, "IHDR", header(width, height))
        writeChunk(out, "IDAT", deflateStored(raw))
        writeChunk(out, "IEND", ByteArray(0))
        return out.toByteArray()
    }

    private fun header(width: Int, height: Int): ByteArray = ByteArray(13).also { data ->
        writeInt(data, 0, width)
        writeInt(data, 4, height)
        data[8] = 8   // bit depth
        data[9] = 2   // colour type: truecolour (RGB)
        data[10] = 0  // compression method: deflate
        data[11] = 0  // filter method: adaptive
        data[12] = 0  // interlace: none
    }

    /** Wraps [data] in a zlib stream made of uncompressed ("stored") deflate blocks. */
    private fun deflateStored(data: ByteArray): ByteArray {
        val out = ByteArrayOutputStream(data.size + data.size / 0xFFFF * 5 + 16)
        out.write(0x78) // zlib CMF: deflate, 32K window
        out.write(0x01) // zlib FLG: no preset dict; makes the 2-byte header a multiple of 31
        var offset = 0
        do {
            val len = minOf(data.size - offset, 0xFFFF)
            val isLast = offset + len >= data.size
            out.write(if (isLast) 1 else 0) // BFINAL plus BTYPE=00 (stored)
            out.write(len and 0xFF)
            out.write(len shr 8 and 0xFF)
            out.write(len.inv() and 0xFF)
            out.write(len.inv() shr 8 and 0xFF)
            out.write(data, offset, len)
            offset += len
        } while (offset < data.size)
        writeInt(out, adler32(data))
        return out.toByteArray()
    }

    private fun adler32(data: ByteArray): Int = Adler32().apply { update(data) }.value.toInt()

    private fun writeChunk(out: ByteArrayOutputStream, type: String, data: ByteArray) {
        writeInt(out, data.size)
        val typeBytes = type.toByteArray(Charsets.US_ASCII)
        out.write(typeBytes, 0, typeBytes.size)
        out.write(data, 0, data.size)
        val crc = CRC32().apply {
            update(typeBytes)
            update(data)
        }
        writeInt(out, crc.value.toInt())
    }

    private fun writeInt(out: ByteArrayOutputStream, value: Int) {
        out.write(intToBytes(value), 0, 4)
    }

    private fun writeInt(target: ByteArray, offset: Int, value: Int) {
        target[offset] = (value shr 24 and 0xFF).toByte()
        target[offset + 1] = (value shr 16 and 0xFF).toByte()
        target[offset + 2] = (value shr 8 and 0xFF).toByte()
        target[offset + 3] = (value and 0xFF).toByte()
    }

    private fun intToBytes(value: Int): ByteArray = byteArrayOf(
        (value shr 24 and 0xFF).toByte(),
        (value shr 16 and 0xFF).toByte(),
        (value shr 8 and 0xFF).toByte(),
        (value and 0xFF).toByte(),
    )
}
