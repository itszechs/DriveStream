package zechs.drive.stream.ui.files

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import zechs.drive.stream.databinding.FragmentFilesBinding


class FilesFragment : Fragment() {

    companion object {
        const val TAG = "FilesFragment"
    }

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFilesBinding.inflate(
            inflater, container, /* attachToParent */false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFilesBinding.bind(view)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}