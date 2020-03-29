package org.tvheadend.tvhclient.util.extensions

fun CharSequence?.isEqualTo(s: String?): Boolean {
    return if (this == null && s == null) {
        true
    } else if (this != null && s != null) {
        this == s
    } else {
        false
    }
}

fun String.subStringUntilOrLess(endIndex: Int): String {
    val maxEndIndex = this.length - 1
    val end = if (maxEndIndex > endIndex) endIndex else maxEndIndex
    return this.substring(0, end)
}