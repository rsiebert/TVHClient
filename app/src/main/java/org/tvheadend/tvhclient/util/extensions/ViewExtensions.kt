package org.tvheadend.tvhclient.util.extensions

import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding

fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
    this.addTextChangedListener(object : TextWatcher {
        override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
        }

        override fun afterTextChanged(editable: Editable?) {
            afterTextChanged.invoke(editable.toString())
        }
    })
}

fun View.gone() {
    this.visibility = View.GONE
}

fun View.visible() {
    this.visibility = View.VISIBLE
}

fun View.invisible() {
    this.visibility = View.INVISIBLE
}

fun View.visibleOrInvisible(show: Boolean) {
    this.visibility = if (show) View.VISIBLE else View.INVISIBLE
}

fun View.visibleOrGone(show: Boolean) {
    this.visibility = if (show) View.VISIBLE else View.GONE
}

/**
 * Applies the system bar and display cutout insets as padding to this view.
 *
 * The insets are not consumed, so sibling and descendant views still receive them. The padding the
 * view already has is captured once, so repeated inset dispatches do not accumulate it.
 *
 * Only the sides the view is responsible for should be requested: a container that sits below an
 * app bar usually only needs the bottom inset, while the app bar itself needs the top inset.
 */
fun View.applySystemBarInsets(
        top: Boolean = false,
        bottom: Boolean = true,
        horizontal: Boolean = false,
) {
    val initialLeft = paddingLeft
    val initialTop = paddingTop
    val initialRight = paddingRight
    val initialBottom = paddingBottom

    ViewCompat.setOnApplyWindowInsetsListener(this) { view, windowInsets ->
        val insets = windowInsets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.displayCutout())

        view.updatePadding(
                left = if (horizontal) initialLeft + insets.left else initialLeft,
                top = if (top) initialTop + insets.top else initialTop,
                right = if (horizontal) initialRight + insets.right else initialRight,
                bottom = if (bottom) initialBottom + insets.bottom else initialBottom,
        )
        windowInsets
    }
}
