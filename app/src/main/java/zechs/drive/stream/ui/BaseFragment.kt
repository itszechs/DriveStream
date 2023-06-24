package zechs.drive.stream.ui

import android.os.Bundle
import android.view.animation.LinearInterpolator
import androidx.fragment.app.Fragment
import com.google.android.material.transition.MaterialSharedAxis

abstract class BaseFragment : Fragment() {

    /**
     * Base fragment class currently only for
     * transition animation
     */

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enterTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.X,
            /* forward */ true
        ).apply {
            interpolator = LinearInterpolator()
            duration = 300
        }

        returnTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.X,
            /* forward */ false
        ).apply {
            interpolator = LinearInterpolator()
            duration = 300
        }

        exitTransition = MaterialSharedAxis(
            /* axis */ MaterialSharedAxis.X,
            /* forward */ true
        ).apply {
            interpolator = LinearInterpolator()
            duration = 250
        }

    }

}