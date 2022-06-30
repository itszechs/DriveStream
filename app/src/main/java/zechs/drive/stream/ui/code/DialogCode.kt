package zechs.drive.stream.ui.code

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import android.widget.Toast
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputLayout
import zechs.drive.stream.R

class DialogCode(
    context: Context,
    val onSubmitClickListener: (String) -> Unit
) : Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setContentView(R.layout.dialog_code)

        val codeText = findViewById<TextInputLayout>(R.id.tf_code)
        val submitButton = findViewById<MaterialButton>(R.id.btn_submit)

        submitButton.setOnClickListener {
            val authCode = codeText.editText!!.text.toString()

            if (authCode.isEmpty()) {
                showToast(context.getString(R.string.please_enter_auth_code))
            } else {
                onSubmitClickListener.invoke(authCode)
            }

        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(
            context, msg, Toast.LENGTH_SHORT
        ).show()
    }

}