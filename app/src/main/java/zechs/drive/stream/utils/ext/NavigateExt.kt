package zechs.drive.stream.utils.ext

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import androidx.navigation.Navigator


fun NavController.navigateSafe(directions: NavDirections) {
    val resId = directions.actionId
    val args = directions.arguments
    navigateSafe(resId, args)
}

fun NavController.navigateSafe(
    @IdRes resId: Int,
    args: Bundle? = null,
    navOptions: NavOptions? = null,
    navExtras: Navigator.Extras? = null
) {
    val action = currentDestination?.getAction(resId) ?: graph.getAction(resId)
    if (action != null && currentDestination?.id != action.destinationId) {
        navigate(resId, args, navOptions, navExtras)
    }
}
