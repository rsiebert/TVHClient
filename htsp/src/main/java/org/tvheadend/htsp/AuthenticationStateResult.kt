package org.tvheadend.htsp

sealed class AuthenticationStateResult {
    data class Idle(val message: String = "") : AuthenticationStateResult()
    data class Authenticating(val message: String = "") : AuthenticationStateResult()
    data class Authenticated(val message: String = "") : AuthenticationStateResult()
    data class Failed(val reason: AuthenticationFailureReason) : AuthenticationStateResult()
}

sealed class AuthenticationFailureReason {
    data class BadCredentials(val message: String = ""): AuthenticationFailureReason()
    data class Other(val message: String = ""): AuthenticationFailureReason()
}