package zechs.drive.stream.ui.signin

import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import zechs.drive.stream.R
import zechs.drive.stream.databinding.FragmentSignInBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.ui.code.DialogCode
import zechs.drive.stream.utils.ext.navigateSafe
import zechs.drive.stream.utils.state.Resource
import zechs.drive.stream.utils.util.Constants.Companion.AUTH_URL


class SignInFragment : BaseFragment() {

    companion object {
        const val TAG = "SignInFragment"
    }

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private var _codeDialog: DialogCode? = null
    private val codeDialog get() = _codeDialog!!

    private val viewModel by activityViewModels<SignInViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSignInBinding.inflate(
            inflater, container, /* attachToParent */false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSignInBinding.bind(view)

        binding.signInText.setOnClickListener {
            Intent().setAction(Intent.ACTION_VIEW)
                .setData(Uri.parse(AUTH_URL))
                .also { startActivity(it) }
        }

        binding.enterCode.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext())
                .setTitle("Please note")
                .setMessage(getString(R.string.important_note_message))
                .setPositiveButton("Continue") { dialog, _ ->
                    dialog.dismiss()
                    showCodeDialog()
                }.show()
        }

        loginObserver()
    }

    private fun loginObserver() {
        viewModel.loginStatus.observe(viewLifecycleOwner) { response ->
            when (response) {
                is Resource.Success -> {
                    findNavController().navigateSafe(
                        R.id.action_signInFragment_to_homeFragment
                    )
                }
                is Resource.Error -> {
                    isLoading(false)
                    Snackbar.make(
                        binding.root,
                        response.message!!,
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                is Resource.Loading -> isLoading(true)
            }
        }
    }

    private fun showCodeDialog() {
        if (_codeDialog == null) {
            _codeDialog = DialogCode(
                context = requireContext(),
                onSubmitClickListener = { codeUri ->
                    viewModel.requestRefreshToken(codeUri)
                    codeDialog.dismiss()
                }
            )
        }

        codeDialog.also {
            it.show()
            it.window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(MATCH_PARENT, WRAP_CONTENT)
            }
            it.setOnDismissListener {
                _codeDialog = null
            }
        }
    }

    private fun isLoading(loading: Boolean) {
        binding.apply {
            this.loading.isVisible = loading
            signInText.isVisible = !loading
            enterCode.isVisible = !loading
        }
    }

}