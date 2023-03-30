package zechs.drive.stream.utils.ext

import android.widget.EditText
import zechs.drive.stream.utils.util.Keyboard

fun EditText.hideKeyboardWhenOffFocus() {
    this.setOnFocusChangeListener { v, hasFocus ->
        if (!hasFocus) Keyboard.hide(v)
    }
}
