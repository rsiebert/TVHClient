package org.tvheadend.api

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

sealed class AuthenticationStateResult : Parcelable {
    @Parcelize
    data class Idle(val message: String = "") : AuthenticationStateResult(), Parcelable
    @Parcelize
    data class Authenticating(val message: String = "") : AuthenticationStateResult(), Parcelable
    @Parcelize
    data class Authenticated(val message: String = "") : AuthenticationStateResult(), Parcelable
    @Parcelize
    data class Failed(val reason: AuthenticationFailureReason) : AuthenticationStateResult(), Parcelable
}

sealed class AuthenticationFailureReason : Parcelable {
    @Parcelize
    data class BadCredentials(val message: String = ""): AuthenticationFailureReason(), Parcelable
    @Parcelize
    data class Other(val message: String = ""): AuthenticationFailureReason(), Parcelable
}