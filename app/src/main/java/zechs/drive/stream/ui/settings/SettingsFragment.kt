package zechs.drive.stream.ui.settings

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isInvisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.transition.TransitionManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.databinding.FragmentSettingsBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.ui.main.MainViewModel
import zechs.drive.stream.utils.AppTheme
import zechs.drive.stream.utils.VideoPlayer
import zechs.drive.stream.utils.state.Resource


class SettingsFragment : BaseFragment() {

    companion object {
        const val TAG = "SettingsFragment"
    }

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val mainViewModel by activityViewModels<MainViewModel>()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(
            inflater, container, /* attachToParent */false
        )
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentSettingsBinding.bind(view)

        binding.toolbar.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        setupThemeMenu()
        setupDefaultPlayerMenu()
        setupCheckForUpdates()
        setupAdsSetting()
    }

    private fun setupThemeMenu() {
        val themes = listOf(
            getString(R.string.theme_dark),
            getString(R.string.theme_light),
            getString(R.string.theme_system)
        )
        binding.settingSelectTheme.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(getString(R.string.select_theme))
                setSingleChoiceItems(
                    themes.toTypedArray(),
                    mainViewModel.currentThemeIndex
                ) { dialog, item ->
                    val theme = when (item) {
                        AppTheme.DARK.value -> AppTheme.DARK
                        AppTheme.LIGHT.value -> AppTheme.LIGHT
                        AppTheme.SYSTEM.value -> AppTheme.SYSTEM
                        else -> throw IllegalArgumentException("Unknown theme value")
                    }
                    mainViewModel.setTheme(theme)
                    dialog.dismiss()
                }
            }.also { it.show() }
        }
    }

    private fun setupDefaultPlayerMenu() {
        val players = listOf(
            getString(R.string.exoplayer),
            getString(R.string.mpv)
        )
        binding.settingDefaultPlayer.setOnClickListener {
            MaterialAlertDialogBuilder(requireContext()).apply {
                setTitle(getString(R.string.default_player))
                setSingleChoiceItems(
                    players.toTypedArray(),
                    mainViewModel.currentPlayerIndex.value
                ) { dialog, item ->
                    val player = when (item) {
                        VideoPlayer.EXO_PLAYER.value -> VideoPlayer.EXO_PLAYER
                        VideoPlayer.MPV.value -> VideoPlayer.MPV
                        else -> throw IllegalArgumentException("Unknown default player")
                    }
                    mainViewModel.setPlayer(player)
                    dialog.dismiss()
                }
            }.also { it.show() }
        }
    }

    private fun setupCheckForUpdates() {

        var isUserClick = false
        binding.settingCheckForUpdate.setOnClickListener {
            if (!mainViewModel.isChecking) {
                mainViewModel.getLatestRelease()
                isUserClick = true
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                mainViewModel.lastUpdated.collect {
                    if (it != null) {
                        val last = "Last checked: $it"
                        TransitionManager.beginDelayedTransition(
                            binding.settingCheckForUpdate,
                        )
                        binding.lastCheckedLabel.text = last
                    }
                }
            }
        }

        fun isChecking(bool: Boolean) {
            binding.progressBarChecking.isInvisible = !bool
        }

        mainViewModel.latest.observe(viewLifecycleOwner) { state ->
            when (state) {
                is Resource.Loading -> isChecking(true)

                is Resource.Error -> {
                    isChecking(false)
                    showSnackBar("Unable to check for updates")
                }

                is Resource.Success -> {
                    isChecking(false)
                    val release = state.data!!
                    if (release.isLatest() && isUserClick) {
                        showSnackBar("You are already on the latest version")
                    }
                }
            }
        }

    }

    private fun setupAdsSetting() {
        binding.switchAdsSupport.isChecked = mainViewModel.adsEnabled
        binding.settingAdsSupport.setOnClickListener {
            if (binding.switchAdsSupport.isChecked) {
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle(getString(R.string.disable_ads))
                    .setMessage(getString(R.string.disable_ads_message))
                    .setNegativeButton(getString(R.string.no)) { dialog, _ ->
                        dialog.dismiss()
                    }
                    .setPositiveButton(getString(R.string.yes)) { dialog, _ ->
                        dialog.dismiss()
                        Log.d(TAG, "Disabling ads")
                        binding.switchAdsSupport.isChecked = false
                        mainViewModel.setEnableAds(false)
                    }
                    .show()
            } else {
                binding.switchAdsSupport.isChecked = true
                mainViewModel.setEnableAds(true)
                showSnackBar(getString(R.string.thank_you_for_supporting_the_app))
            }
        }
    }

    private fun showSnackBar(message: String) {
        Snackbar.make(
            binding.root,
            message,
            Snackbar.LENGTH_SHORT
        ).show()
    }

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}