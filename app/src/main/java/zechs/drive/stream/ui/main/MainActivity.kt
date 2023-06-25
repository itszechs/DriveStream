package zechs.drive.stream.ui.main

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_DEFAULT
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import zechs.drive.stream.BuildConfig
import zechs.drive.stream.R
import zechs.drive.stream.data.model.LatestRelease
import zechs.drive.stream.databinding.ActivityMainBinding
import zechs.drive.stream.utils.AdUnits
import zechs.drive.stream.utils.AppTheme
import zechs.drive.stream.utils.ext.navigateSafe
import zechs.drive.stream.utils.state.Resource
import zechs.drive.stream.utils.util.NotificationKeys.Companion.UPDATE_CHANNEL_CODE
import zechs.drive.stream.utils.util.NotificationKeys.Companion.UPDATE_CHANNEL_ID
import zechs.drive.stream.utils.util.NotificationKeys.Companion.UPDATE_CHANNEL_NAME
import java.util.*

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    companion object {
        const val TAG = "MainActivity"
        const val NOTIFICATION_PERMISSION_CODE = 2000
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            createUpdateNotificationChannel()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ActivityCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) handlePermission()
        }

        val navHostFragment = supportFragmentManager.findFragmentById(
            R.id.mainNavHostFragment
        ) as NavHostFragment
        navController = navHostFragment.navController

        themeObserver()
        updateObserver()
        redirectOnLogin()
        setupAds()
    }

    private fun themeObserver() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.theme.collect { theme ->
                    Log.d(TAG, "theme=${theme}")
                    changeTheme(theme)
                }
            }
        }
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

    private fun redirectOnLogin() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.hasLoggedIn.collect { hasLoggedIn ->
                    Log.d(TAG, "hasLoggedIn=${hasLoggedIn}")
                    if (hasLoggedIn) handleLogin()
                }
            }
        }
    }

    private fun handleLogin() {
        val currentFragment = navController.currentDestination?.id
        if (currentFragment != null && currentFragment == R.id.signInFragment) {
            navController.navigateSafe(R.id.action_signInFragment_to_homeFragment)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    private fun updateObserver() {
        viewModel.latest.observe(this) {
            when (it) {
                is Resource.Success -> {
                    val release = it.data!!
                    if (release.isLatest()) {
                        Log.d(TAG, "Already on latest version")
                    } else {
                        Log.d(TAG, "Newer version of app is available (latest=${release.tagName})")
                        sendUpdateNotification(release)
                    }
                }

                is Resource.Error -> Log.d(TAG, it.message!!)
                else -> {}
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun createUpdateNotificationChannel() {
        val channel = NotificationChannel(
            UPDATE_CHANNEL_ID,
            UPDATE_CHANNEL_NAME,
            IMPORTANCE_DEFAULT
        )

        val notificationManager = getSystemService(
            NOTIFICATION_SERVICE
        ) as NotificationManager

        notificationManager.createNotificationChannel(channel)
    }

    private fun sendUpdateNotification(release: LatestRelease) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(release.htmlUrl)
        }

        val pendingIntent = PendingIntent.getActivity(
            /* context */ this,
            /* requestCode */ Random().nextInt(),
            /* intent */ intent,
            /* flags */
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_IMMUTABLE
            else PendingIntent.FLAG_UPDATE_CURRENT
        )


        val defaultSoundUri = RingtoneManager.getDefaultUri(
            RingtoneManager.TYPE_NOTIFICATION
        )

        val notificationBuilder = NotificationCompat.Builder(
            /* context */this,
            /* channelId */ UPDATE_CHANNEL_ID
        ).apply {
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setSmallIcon(R.drawable.ic_update_24)
            setContentTitle(release.name)
            setContentText(getString(R.string.new_version_available))
            setContentIntent(pendingIntent)
            setAutoCancel(true)
            setSound(defaultSoundUri)
        }

        with(NotificationManagerCompat.from(this)) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ActivityCompat.checkSelfPermission(
                        applicationContext,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) == PackageManager.PERMISSION_GRANTED
                ) {
                    notify(
                        /* requestCode */ UPDATE_CHANNEL_CODE,
                        /* notification */ notificationBuilder.build()
                    )
                }
            } else {
                notify(
                    /* requestCode */ UPDATE_CHANNEL_CODE,
                    /* notification */ notificationBuilder.build()
                )
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun handlePermission() {
        if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
            Snackbar.make(
                binding.root,
                getString(R.string.notification_permission_rationale),
                Snackbar.LENGTH_LONG
            ).setAction(getString(R.string.grant)) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    NOTIFICATION_PERMISSION_CODE
                )
            }.show()
        } else {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                NOTIFICATION_PERMISSION_CODE
            )
        }
    }

    private fun changeTheme(appTheme: AppTheme) {
        val mode = when (appTheme) {
            AppTheme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            AppTheme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
            AppTheme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun setupAds() {
        val linearLayout = binding.linearLayout
        var adView: AdView? = null
        val adId = if (BuildConfig.DEBUG) AdUnits.DEBUG_BANNER else AdUnits.RELEASE_BANNER

        viewModel.showAds.observe(this) { enabled ->
            if (enabled) {
                Log.d(TAG, "setupAds(), Loading AdView...")
                adView = AdView(this@MainActivity)
                adView!!.apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = adId
                    loadAd(AdRequest.Builder().build())
                }
                linearLayout.addView(adView)
            } else {
                if (adView == null) return@observe
                Log.d(TAG, "setupAds(), Removing AdView...")
                linearLayout.removeView(adView)
                adView!!.destroy()
                adView = null
            }
        }
    }

}