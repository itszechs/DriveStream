package zechs.drive.stream.ui.files

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
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
    private lateinit var startForResult: ActivityResultLauncher<Intent>

    private var isLoading = false
    private var isScrolling = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK
                && result.data != null
            ) {
                val intent = result.data!!
                viewModel.handleSignInResult(intent)
            }
        }
    }

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

        viewModel.atLastItem.observe(viewLifecycleOwner) {
            if (it.isLoading && it.isAtLast) {
                binding.pagingLoading.isVisible = true
            } else {
                binding.pagingLoading.isGone = true
            }
        }

        viewModel.userAuth.observe(viewLifecycleOwner) { intent ->
            Log.d(TAG, intent?.data.toString())
            intent?.let { startForResult.launch(it) }
        }
    }

    private fun handleFilesList(response: Resource<List<DriveFile>>) {
        when (response) {
            is Resource.Success -> response.data?.let { files ->
                onSuccess(files)
            }
            is Resource.Error -> {
                showSnackBar(response.message)
                showError(response.message)
            }
            is Resource.Loading -> {
                isLoading = true
                if (!viewModel.hasLoaded) {
                    isLoading(true)
                }
                if (viewModel.hasLoaded && !viewModel.isLastPage) {
                    binding.pagingLoading.isVisible = true
                }
            }
        }
    }

    private fun onSuccess(files: List<DriveFile>) {
        if (!viewModel.hasLoaded) {
            doTransition(MaterialFadeThrough())
        }

        isLoading(false)
        isLoading = false
        viewModel.hasLoaded = true

        if (viewModel.isLastPage) {
            doTransition(
                transition = AutoTransition().apply {
                    duration = 200
                }
            )
            binding.rvList.setPadding(0, 0, 0, 0)
        }

        if (files.isEmpty()) {
            binding.error.apply {
                root.isVisible = true
                errorTxt.text = "No files found"
            }
        } else {
            binding.error.root.apply {
                if (isVisible) {
                    isGone = true
                }
            }
        }

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

    private fun showError(msg: String?) {
        binding.apply {
            rvList.isInvisible = true
            loading.isInvisible = true
            binding.pagingLoading.isGone = true
            error.apply {
                root.isVisible = true
                errorTxt.text = msg ?: getString(R.string.something_went_wrong)
            }
        }
        isLoading = false
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

    private val scrollListener = object : RecyclerView.OnScrollListener() {
        override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
            super.onScrolled(recyclerView, dx, dy)

            val layoutManager = recyclerView.layoutManager as LinearLayoutManager
            val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
            val visibleItemCount = layoutManager.childCount
            val totalItemCount = layoutManager.itemCount

            val isAtLastItem = firstVisibleItemPosition + visibleItemCount >= totalItemCount
            val isLastPage = viewModel.isLastPage

            if (isAtLastItem && !isLoading && !isLastPage && isScrolling) {
                Log.d(TAG, "Paginating...")
                viewModel.queryFiles(args.query)
                isScrolling = false
            }
            viewModel.setLoadingState(isAtLastItem, isLoading)
        }

        override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
            super.onScrollStateChanged(recyclerView, newState)
            if (newState == AbsListView.OnScrollListener.SCROLL_STATE_TOUCH_SCROLL) {
                isScrolling = true
            }
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
            addOnScrollListener(this@FilesFragment.scrollListener)
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