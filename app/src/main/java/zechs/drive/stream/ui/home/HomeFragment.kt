package zechs.drive.stream.ui.home

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import com.google.android.material.button.MaterialButton
import zechs.drive.stream.R
import zechs.drive.stream.databinding.FragmentHomeBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.utils.ext.navigateSafe

class HomeFragment : BaseFragment() {

    companion object {
        const val TAG = "HomeFragment"
    }

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(
            inflater, container, /* attachToParent */false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentHomeBinding.bind(view)

        binding.apply {

            // My drive
            navigateToFiles(
                button = btnMyDrive,
                name = getString(R.string.my_drive),
                query = "'root' in parents and trashed = false"
            )

            // Shared drives
            navigateToFiles(
                button = btnSharedDrives,
                name = getString(R.string.shared_drives),
                query = null
            )

            // Shared with me
            navigateToFiles(
                button = btnSharedWithMe,
                name = getString(R.string.shared_with_me),
                query = "sharedWithMe=true"
            )

            // Starred
            navigateToFiles(
                button = btnStarred,
                name = getString(R.string.starred),
                query = "starred=true"
            )

            // Trashed
            navigateToFiles(
                button = btnTrashed,
                name = getString(R.string.trashed),
                query = "'root' in parents and trashed=true"
            )

        }

    }

    private fun <T : MaterialButton> navigateToFiles(
        button: T, name: String, query: String?
    ) {
        button.setOnClickListener {
            val action = HomeFragmentDirections.actionHomeFragmentToFilesFragment(
                name = name,
                query = query
            )
            findNavController().navigateSafe(action)
            Log.d(TAG, "navigateToFiles(name=$name, query=$query)")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

}