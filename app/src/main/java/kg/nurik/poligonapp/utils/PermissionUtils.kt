package kg.nurik.poligonapp.utils

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity

object PermissionUtils {

    private val locationPermission = arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    const val LOCATION_REQUEST_CODE = 101

    fun requestLocationPermission(activity: AppCompatActivity): Boolean {
        if (checkLocationPermission(activity))
            return true

        activity.requestPermissions(locationPermission, LOCATION_REQUEST_CODE)
        return false
    }

    private fun checkLocationPermission(activity: AppCompatActivity) =
        activity.checkSelfPermission(locationPermission[0]) == PackageManager.PERMISSION_GRANTED
}

