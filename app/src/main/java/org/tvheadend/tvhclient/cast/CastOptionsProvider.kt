package org.tvheadend.tvhclient.cast

import android.content.Context
import com.google.android.gms.cast.framework.CastOptions
import com.google.android.gms.cast.framework.OptionsProvider
import com.google.android.gms.cast.framework.SessionProvider
import org.tvheadend.tvhclient.BuildConfig

/**
 * Cast configuration consumed by the GMS Cast framework that Media3 Cast is built on.
 *
 * Media3's CastPlayer still reads its options through the GMS [OptionsProvider] interface, and the
 * app uses the project-specific receiver application id ([BuildConfig.CAST_ID]) rather than the
 * default one, so a custom provider is required instead of `DefaultCastOptionsProvider`.
 *
 * Remote-to-local transfer is disabled on purpose: the service's local player is a plain ExoPlayer
 * that cannot play the app's HTSP streams, so moving playback back from the TV to the phone would
 * not work. Stopping the cast is the supported way to end remote playback.
 */
class CastOptionsProvider : OptionsProvider {

    override fun getCastOptions(context: Context): CastOptions {
        return CastOptions.Builder()
                .setReceiverApplicationId(BuildConfig.CAST_ID)
                .setRemoteToLocalEnabled(false)
                .build()
    }

    override fun getAdditionalSessionProviders(context: Context): List<SessionProvider>? = null
}
