package org.tvheadend.tvhclient.ui.features.playback.internal

import androidx.annotation.StringRes
import org.tvheadend.htsp.HtspMessage
import org.tvheadend.tvhclient.R

/**
 * Signal quality as reported by the server in a `signalStatus` message.
 *
 * The server sends either a relative (`feSNR`, `feSignal`) or an absolute
 * (`feAbsoluteSNR`, `feAbsoluteSignal`) value, depending on what the tuner supports.
 */
data class HtspSignalStatus(
    val status: String? = null,
    val snr: Long? = null,
    val absoluteSnr: Long? = null,
    val signal: Long? = null,
    val absoluteSignal: Long? = null,
    val ber: Long? = null,
    val unc: Long? = null,
)

/**
 * Descrambler (CAM) information as reported in a `descrambleInfo` message.
 * Only sent by servers running HTSP 24 or newer.
 */
data class HtspDescrambleInfo(
    val pid: Int = 0,
    val caid: Int = 0,
    val provid: Int = 0,
    val ecmTime: Int = 0,
    val hops: Int = 0,
    val cardSystem: String? = null,
    val reader: String? = null,
    val from: String? = null,
    val protocol: String? = null,
)

/**
 * Queue statistics as reported in a `queueStatus` message. The server sends these
 * roughly once a second while a subscription is streaming.
 */
data class HtspQueueStatus(
    val packets: Int = 0,
    val bytes: Long = 0,
    val errors: Int = 0,
)

/**
 * Everything the server tells us about an active subscription, apart from the
 * stream data itself. Instances are immutable; [HtspSubscriptionStateReader] returns
 * an updated copy for every relevant server message.
 */
data class HtspSubscriptionState(
    val signalStatus: HtspSignalStatus? = null,
    val descrambleInfo: HtspDescrambleInfo? = null,
    val queueStatus: HtspQueueStatus? = null,
    val speed: Int? = null,
    val graceTimeout: Int? = null,
    val status: String? = null,
    val subscriptionError: String? = null,
)

/** Receives an update for every server message that describes a subscription's state. */
interface HtspSubscriptionStateListener {
    fun onSubscriptionStateChanged(state: HtspSubscriptionState)
}

/**
 * Turns a single HTSP server message into an updated [HtspSubscriptionState].
 * Messages that do not describe the subscription state are returned unchanged.
 */
object HtspSubscriptionStateReader {

    @JvmStatic
    fun read(previous: HtspSubscriptionState, message: HtspMessage, method: String): HtspSubscriptionState {
        return when (method) {
            "signalStatus" -> previous.copy(
                signalStatus = HtspSignalStatus(
                    status = message.getString("feStatus", null),
                    snr = optionalLong(message, "feSNR"),
                    absoluteSnr = optionalLong(message, "feAbsoluteSNR"),
                    signal = optionalLong(message, "feSignal"),
                    absoluteSignal = optionalLong(message, "feAbsoluteSignal"),
                    ber = optionalLong(message, "feBER"),
                    unc = optionalLong(message, "feUNC"),
                )
            )

            "descrambleInfo" -> previous.copy(
                descrambleInfo = HtspDescrambleInfo(
                    pid = message.getInteger("pid", 0),
                    caid = message.getInteger("caid", 0),
                    provid = message.getInteger("provid", 0),
                    ecmTime = message.getInteger("ecmtime", 0),
                    hops = message.getInteger("hops", 0),
                    cardSystem = message.getString("cardsystem", null),
                    reader = message.getString("reader", null),
                    from = message.getString("from", null),
                    protocol = message.getString("protocol", null),
                )
            )

            "queueStatus" -> previous.copy(
                queueStatus = HtspQueueStatus(
                    packets = message.getInteger("packets", 0),
                    bytes = message.getLong("bytes", 0L),
                    errors = message.getInteger("errors", 0),
                )
            )

            // The server grants the subscription a grace period when no stream data
            // can be delivered yet, for example while waiting for a free adapter.
            "subscriptionGrace" -> previous.copy(
                graceTimeout = message.getInteger("graceTimeout", 0),
                status = null,
                subscriptionError = null,
            )

            "subscriptionSpeed" -> previous.copy(
                speed = message.getInteger("speed", 0),
            )

            // A status message with an absent "status" field only means that packets are
            // flowing again, so any previously reported error no longer applies.
            "subscriptionStop", "subscriptionStatus" -> previous.copy(
                graceTimeout = null,
                status = message.getString("status", null),
                subscriptionError = message.getString("subscriptionError", null),
            )

            else -> previous
        }
    }

    private fun optionalLong(message: HtspMessage, key: String): Long? {
        return if (message.containsKey(key)) message.getLong(key) else null
    }
}

/**
 * Maps the status code of a `subscriptionStop` / `subscriptionStatus` message to a
 * string resource, or `null` when the code is not one we know about. Unknown codes are
 * plain text from the server (for example "Input/output error") and can be shown as is.
 */
@StringRes
fun htspStatusToStringRes(status: String?): Int? = when (status) {
    "noFreeAdapter" -> R.string.htsp_status_no_free_adapter
    "scrambled" -> R.string.htsp_status_scrambled
    "badSignal" -> R.string.htsp_status_bad_signal
    "tuningFailed" -> R.string.htsp_status_tuning_failed
    "subscriptionOverridden" -> R.string.htsp_status_subscription_overridden
    "muxNotEnabled" -> R.string.htsp_status_mux_not_enabled
    "invalidTarget" -> R.string.htsp_status_invalid_target
    "userAccess" -> R.string.htsp_status_user_access
    "userLimit" -> R.string.htsp_status_user_limit
    "weakStream" -> R.string.htsp_status_weak_stream
    "noDiskSpace" -> R.string.htsp_status_no_disk_space
    else -> null
}
