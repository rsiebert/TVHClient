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