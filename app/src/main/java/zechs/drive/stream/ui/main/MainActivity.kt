package zechs.drive.stream.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import zechs.drive.stream.R
import zechs.drive.stream.databinding.ActivityMainBinding
import zechs.drive.stream.utils.ext.navigateSafe

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private val viewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        /*
         * Splash screen via the SplashScreenApi
         */
        doSplashScreen()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.mainNavHostFragment
        ) as NavHostFragment
        navController = navHostFragment.navController

        requestSignIn()
        redirectOnLogin()
    }

    private fun doSplashScreen() {
        installSplashScreen().apply {
            setKeepOnScreenCondition { viewModel.isLoading.value }
            setOnExitAnimationListener { viewProvider ->
                viewProvider.view
                    .animate()
                    .setDuration(300L)
                    .alpha(0f)
                    .withEndAction { viewProvider.remove() }
                    .start()
            }
        }
    }

    private fun requestSignIn() {
        Log.d(TAG, "Requesting sign-in")

        val client = viewModel.getClient()

        val startForResult = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == RESULT_OK && result.data != null) {
                val intent = result.data!!
                Log.d(TAG, "intent=$intent")
                viewModel.handleSignInResult(intent)
            }
        }
        startForResult.launch(client.signInIntent)
    }

    private fun redirectOnLogin() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasLoggedIn.collect {
                    handleLogin(hasLoggedIn = it)
                }
            }
        }
    }

    private fun handleLogin(hasLoggedIn: Boolean) {
        Log.d(TAG, "hasLoggedIn=${hasLoggedIn}")
        val currentFragment = navController.currentDestination?.id
        if (hasLoggedIn && currentFragment != null && currentFragment == R.id.signInFragment) {
            navController.navigateSafe(R.id.action_signInFragment_to_homeFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}