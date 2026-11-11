package org.tvheadend.tvhclient.util

import kotlin.math.max

/**
 * Returns the `inSampleSize` to use when decoding an icon whose intrinsic size is
 * [outWidth] x [outHeight], so that the decoded bitmap stays close to [targetWidth] x
 * [targetHeight] without becoming smaller than it.
 *
 * `BitmapFactory` requires `inSampleSize` to be a power of two >= 1: any other value is rounded
 * down to the nearest power of two, and 0 is invalid. Both HTSP icon downloaders used to compute
 * this inline and had drifted apart (only one of them guarded against a zero-sized or unreadable
 * image), so the shared version always guards.
 */
internal fun calculateInSampleSize(
    outWidth: Int,
    outHeight: Int,
    targetWidth: Int,
    targetHeight: Int,
): Int {
    if (outWidth <= 0 || outHeight <= 0 || targetWidth <= 0 || targetHeight <= 0) {
        return 1
    }
    val ratio = max(outWidth / targetWidth, outHeight / targetHeight)
    return Integer.highestOneBit(ratio).coerceAtLeast(1)
}
