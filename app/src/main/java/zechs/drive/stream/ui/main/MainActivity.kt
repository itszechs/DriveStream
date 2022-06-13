package zechs.drive.stream.ui.main

import android.os.Bundle
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.Scopes
import com.google.android.gms.common.api.Scope
import dagger.hilt.android.AndroidEntryPoint
import zechs.drive.stream.R
import zechs.drive.stream.databinding.ActivityMainBinding

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

        val signInOptions = GoogleSignInOptions
            .Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope(Scopes.DRIVE_FULL))
            .requestEmail()
            .build()

        val client = GoogleSignIn.getClient(this, signInOptions)

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

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }
}