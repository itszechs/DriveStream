package zechs.drive.stream.ui.signin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import zechs.drive.stream.databinding.FragmentSignInBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.ui.main.MainViewModel

class SignInFragment : BaseFragment() {

    private var _binding: FragmentSignInBinding? = null
    private val binding get() = _binding!!

    private val viewModel by activityViewModels<MainViewModel>()

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

        viewModel.loginStatus.observe(viewLifecycleOwner) {
            binding.signInText.text = it
        }
    }

}