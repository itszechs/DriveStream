package zechs.drive.stream.utils.util

import android.content.Context.INPUT_METHOD_SERVICE
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.view.inputmethod.InputMethodManager.HIDE_NOT_ALWAYS
import android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT

object Keyboard {

    fun show(v: View) {
        if (v.requestFocus()) {
            (v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
                .showSoftInput(v, SHOW_IMPLICIT)
        }
    }

    fun hide(v: View) {
        (v.context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(v.windowToken, HIDE_NOT_ALWAYS)
    }

}