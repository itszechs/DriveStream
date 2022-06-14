package zechs.drive.stream.ui.files

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.view.isInvisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.FragmentFilesBinding
import zechs.drive.stream.ui.files.adapter.FilesAdapter
import zechs.drive.stream.utils.state.Resource


@AndroidEntryPoint
class FilesFragment : Fragment() {

    companion object {
        const val TAG = "FilesFragment"
    }

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private val viewModel by lazy {
        ViewModelProvider(this)[FilesViewModel::class.java]
    }
    private val args by navArgs<FilesFragmentArgs>()

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

        binding.toolbar.apply {
            title = args.name
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        Log.d(TAG, "FilesFragment(name=${args.name}, query=${args.query})")

        setupRecyclerView()
        setupFilesObserver()
    }

    private fun setupFilesObserver() {
        if (!viewModel.hasLoaded) {
            viewModel.queryFiles(args.query)
        }

        viewModel.filesList.observe(viewLifecycleOwner) { response ->
            handleFilesList(response)
        }
    }

    private fun handleFilesList(response: Resource<List<DriveFile>>) {
        when (response) {
            is Resource.Success -> response.data?.let { files ->
                onSuccess(files)
            }
            is Resource.Error -> {
                showSnackBar(response.message)
            }
            is Resource.Loading -> {
                if (!viewModel.hasLoaded) {
                    isLoading(true)
                }
            }
        }
    }

    private fun onSuccess(files: List<DriveFile>) {
        if (!viewModel.hasLoaded) {
            doTransition(MaterialFadeThrough())
        }

        isLoading(false)
        viewModel.hasLoaded = true

        lifecycleScope.launch {
            filesAdapter.submitList(files.toMutableList())
        }
    }

    private fun doTransition(transition: Transition) {
        TransitionManager.beginDelayedTransition(
            binding.root, transition
        )
    }

    private fun isLoading(hide: Boolean) {
        binding.apply {
            loading.isInvisible = !hide
            rvList.isInvisible = hide
        }
    }

    private val filesAdapter by lazy {
        FilesAdapter(onClickListener = { handleFileOnClick(it) })
    }

    private fun handleFileOnClick(file: DriveFile) {
        Log.d(TAG, file.toString())
        if (file.isFolder && !file.isShortcut) {
            val action = FilesFragmentDirections.actionFilesFragmentSelf(
                name = file.name,
                query = "'${file.id}' in parents and trashed=false"
            )
            findNavController().navigate(action)
        } else {
            // Just showing a SnackBar for the time being...
            showSnackBar("fileId=${file.id}\nfileName:${file.name}")
        }
    }

    private fun setupRecyclerView() {
        val linearLayoutManager = LinearLayoutManager(
            /* context */ context,
            /* orientation */ LinearLayout.VERTICAL,
            /* reverseLayout */ false
        )
        binding.rvList.apply {
            adapter = filesAdapter
            layoutManager = linearLayoutManager
            addItemDecoration(
                DividerItemDecoration(context, linearLayoutManager.orientation)
            )
        }
    }

    private fun showSnackBar(message: String?) {
        val snackBar = Snackbar.make(
            binding.root,
            message ?: getString(R.string.something_went_wrong),
            Snackbar.LENGTH_SHORT
        )
        val snackBarView = snackBar.view
        val textView = snackBarView.findViewById<View>(
            com.google.android.material.R.id.snackbar_text
        ) as TextView
        textView.maxLines = 5
        snackBar.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding.rvList.adapter = null
        _binding = null
    }

}