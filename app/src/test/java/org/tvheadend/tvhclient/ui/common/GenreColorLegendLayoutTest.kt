package org.tvheadend.tvhclient.ui.common

import android.app.Application
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.tvheadend.tvhclient.R
import org.tvheadend.tvhclient.databinding.GenreColorListAdapterBinding

/**
 * The genre color legend is assembled from two layouts: the dialog body
 * (`genre_color_legend`) and the row that draws one color next to one genre name
 * (`genre_color_list_adapter`). Both are inflated here, because compiling the data binding
 * expressions alone does not prove that the ids the dialog and the adapter look up resolve.
 */
@RunWith(RobolectricTestRunner::class)
// A plain Application: MainApplication runs the database migration in onCreate.
@Config(sdk = [34], application = Application::class)
class GenreColorLegendLayoutTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test
    fun theDialogBodyResolvesTheExplanationAndTheList() {
        val legend = LayoutInflater.from(context).inflate(R.layout.genre_color_legend, null)

        assertThat(legend.findViewById<View>(R.id.genre_color_explanation)).isNotNull()
        assertThat(legend.findViewById<View>(R.id.genre_color_list)).isNotNull()
    }

    @Test
    fun theRowResolvesTheSwatchAndTheGenreLabel() {
        val row = inflateRow()

        assertThat(row.findViewById<View>(R.id.color)).isNotNull()
        assertThat(row.findViewById<View>(R.id.genre)).isNotNull()
    }

    @Test
    fun theRowIsInformationalAndNotClickable() {
        // A legend explains the colors, it selects nothing. A clickable row would draw a ripple
        // and promise an action that does not exist.
        assertThat(inflateRow().isClickable).isFalse()
    }

    @Test
    fun theSwatchIsHiddenFromAccessibilityAndTheLabelIsNot() {
        val row = inflateRow()
        val swatch = row.findViewById<View>(R.id.color)
        val label = row.findViewById<View>(R.id.genre)

        // The swatch carries no text, so it must not become a separate, meaningless TalkBack stop.
        assertThat(swatch.importantForAccessibility).isEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
        assertThat(label.importantForAccessibility).isNotEqualTo(View.IMPORTANT_FOR_ACCESSIBILITY_NO)
    }

    private fun inflateRow(): View =
            GenreColorListAdapterBinding.inflate(LayoutInflater.from(context), null, false).root
}
