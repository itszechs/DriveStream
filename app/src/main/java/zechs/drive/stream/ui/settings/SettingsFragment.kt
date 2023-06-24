package zechs.drive.stream.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import zechs.drive.stream.R
import zechs.drive.stream.databinding.FragmentSettingsBinding
import zechs.drive.stream.ui.BaseFragment
import zechs.drive.stream.ui.main.MainViewModel
import zechs.drive.stream.utils.AppTheme


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

    override fun onDestroy() {
        super.onDestroy()
        _binding = null
    }

}