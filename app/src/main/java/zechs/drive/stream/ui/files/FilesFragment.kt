package zechs.drive.stream.ui.files

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AbsListView
import android.widget.TextView
import androidx.core.view.isGone
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.google.android.material.color.MaterialColors
import com.google.android.material.snackbar.Snackbar
import com.google.android.material.transition.MaterialFadeThrough
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.data.model.DriveFile
import zechs.drive.stream.databinding.FragmentFilesBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.ui.files.adapter.FilesAdapter
import zechs.drive.stream.ui.files.adapter.FilesDataModel
import zechs.drive.stream.ui.main.MainViewModel
import zechs.drive.stream.ui.player.PlayerActivity
import zechs.drive.stream.ui.player2.MPVActivity
import zechs.drive.stream.utils.VideoPlayer
import zechs.drive.stream.utils.state.Resource


@AndroidEntryPoint
class FilesFragment : BaseFragment() {

    companion object {
        const val TAG = "FilesFragment"
    }

    private var _binding: FragmentFilesBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel by activityViewModels<MainViewModel>()
    private val viewModel by lazy {
        ViewModelProvider(this)[FilesViewModel::class.java]
    }

    private val args by navArgs<FilesFragmentArgs>()

    private var isLoading = false
    private var isScrolling = false

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

        // Workaround for transition animation
        // https://github.com/material-components/material-components-android/issues/1984
        val colorBackground = MaterialColors.getColor(view, android.R.attr.colorBackground)
        view.setBackgroundColor(colorBackground)

        binding.toolbar.apply {
            title = args.name
            setNavigationOnClickListener {
                findNavController().navigateUp()
            }
        }

        Log.d(TAG, "FilesFragment(name=${args.name}, query=${args.query})")

        setupRecyclerView()
        setupFilesObserver()
        mpvObserver()

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.fileUpdate.collect { status ->
                    showSnackBar(status)
                }
            }
        }


    }

    private fun setupFilesObserver() {
        if (!viewModel.hasLoaded) {
            viewModel.queryFiles(args.query)
        }

        viewModel.filesList.observe(viewLifecycleOwner) { response ->
            handleFilesList(response)
        }
    }

    private fun handleFilesList(response: Resource<List<FilesDataModel>>) {
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
                if (!viewModel.hasLoaded || viewModel.hasFailed) {
                    isLoading(true)
                }
                binding.error.root.apply {
                    if (isVisible) {
                        isGone = true
                    }
                }
            }
        }
    }

    private fun onSuccess(files: List<FilesDataModel>) {
        Log.d(TAG, "onSuccess(files=${files.size})")
        if (!viewModel.hasLoaded) {
            doTransition(MaterialFadeThrough())
        }

        isLoading(false)
        isLoading = false
        viewModel.hasLoaded = true

        if (files.isEmpty()) {
            binding.error.apply {
                root.isVisible = true
                errorTxt.text = getString(R.string.no_files_found)
            }
        } else {
            binding.error.root.apply {
                if (isVisible) {
                    isGone = true
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
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
            error.apply {
                root.isVisible = true
                errorTxt.text = msg ?: getString(R.string.something_went_wrong)
                btnRetry.apply {
                    isVisible = true
                    setOnClickListener {
                        viewModel.queryFiles(args.query)
                    }
                }
            }
        }
        isLoading = false
    }

    private val filesAdapter by lazy {
        FilesAdapter(
            onClickListener = { handleFileOnClick(it) },
            onLongClickListener = { handleFileOnLongPress(it) },
            onStarClickListener = { file, isStarred ->
                viewModel.starFile(file, isStarred)
            }
        )
    }

    private fun handleFileOnLongPress(file: DriveFile) {
        Log.d(TAG, file.toString())
        if (file.isVideoFile) {
            launchVideoPlayer(file)
        } else if (file.isShortcut) {
            if (file.isShortcutVideo) {
                val videoShortcutFile = file.copy(id = file.shortcutDetails.targetId!!)
                launchVideoPlayer(videoShortcutFile)
            }
        }
    }

    private fun handleFileOnClick(file: DriveFile) {
        Log.d(TAG, file.toString())
        if (file.isFolder && !file.isShortcut) {
            val action = FilesFragmentDirections.actionFilesFragmentSelf(
                name = file.name,
                query = "'${file.id}' in parents and trashed=false"
            )
            findNavController().navigate(action)
        } else if (file.isVideoFile) {
            viewModel.fetchToken(file)
        } else if (file.isShortcut) {
            if (file.isShortcutFolder) {
                val action = FilesFragmentDirections.actionFilesFragmentSelf(
                    name = file.name,
                    query = "'${file.shortcutDetails.targetId}' in parents and trashed=false"
                )
                findNavController().navigate(action)
            } else if (file.isShortcutVideo) {
                val videoShortcutFile = file.copy(id = file.shortcutDetails.targetId!!)
                viewModel.fetchToken(videoShortcutFile)
            }
        }

    }

    private fun launchVideoPlayer(file: DriveFile) {
        when (mainViewModel.currentPlayerIndex) {
            VideoPlayer.EXO_PLAYER -> launchExo(file)
            VideoPlayer.MPV -> viewModel.fetchToken(file)
        }
    }

    private fun mpvObserver() {
        viewModel.mpvFile.observe(viewLifecycleOwner) { event ->
            event.getContentIfNotHandled()?.let { res ->
                when (res) {
                    is Resource.Success -> {
                        launchMpv(res.data!!)
                    }

                    is Resource.Error -> {
                        showSnackBar(res.message!!)
                    }

                    else -> {}
                }
            }
        }
    }

    private fun launchExo(file: DriveFile) {
        Intent(
            context, PlayerActivity::class.java
        ).apply {
            putExtra("fileId", file.id)
            putExtra("title", file.name)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { startActivity(it) }
    }

    private fun launchMpv(fileToken: FilesViewModel.FileToken) {
        Intent(
            context, MPVActivity::class.java
        ).apply {
            putExtra("fileId", fileToken.fileId)
            putExtra("title", fileToken.fileName)
            putExtra("accessToken", fileToken.accessToken)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }.also { startActivity(it) }
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
            /* orientation */ RecyclerView.VERTICAL,
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