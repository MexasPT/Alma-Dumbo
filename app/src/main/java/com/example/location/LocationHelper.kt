package com.example.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

data class LocationInfo(
    val latitude: Double,
    val longitude: Double,
    val address: String,
    val mapUrl: String,
    val staticMapPreviewUrl: String
)

class LocationHelper(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val coarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
    }

    suspend fun getCurrentLocationInfo(): LocationInfo? = withContext(Dispatchers.IO) {
        if (!hasLocationPermission()) return@withContext null

        try {
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
                ?: return@withContext null

            var bestLocation: Location? = null

            val providers = locationManager.getProviders(true)
            for (provider in providers) {
                try {
                    val l = locationManager.getLastKnownLocation(provider) ?: continue
                    if (bestLocation == null || l.accuracy < bestLocation.accuracy) {
                        bestLocation = l
                    }
                } catch (e: SecurityException) {
                    Log.e("LocationHelper", "SecurityException getting location", e)
                }
            }

            if (bestLocation == null) {
                // Fallback default coordinates if emulator or no GPS fix yet
                return@withContext null
            }

            val lat = bestLocation.latitude
            val lon = bestLocation.longitude
            val address = resolveAddress(lat, lon)
            val mapUrl = "https://www.google.com/maps/search/?api=1&query=$lat,$lon"
            val staticMapUrl = "https://static-maps.yandex.ru/1.x/?ll=$lon,$lat&z=15&l=map&pt=$lon,$lat,pm2rdm"

            return@withContext LocationInfo(
                latitude = lat,
                longitude = lon,
                address = address,
                mapUrl = mapUrl,
                staticMapPreviewUrl = staticMapUrl
            )
        } catch (e: Exception) {
            Log.e("LocationHelper", "Error getting location", e)
            return@withContext null
        }
    }

    suspend fun resolveAddress(latitude: Double, longitude: Double): String = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                var resolvedStr = "$latitude, $longitude"
                try {
                    val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                    if (!addresses.isNullOrEmpty()) {
                        val addr = addresses[0]
                        resolvedStr = formatAddress(addr)
                    }
                } catch (e: Exception) {
                    resolvedStr = "Coordenadas: $latitude, $longitude"
                }
                return@withContext resolvedStr
            } else {
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocation(latitude, longitude, 1)
                if (!addresses.isNullOrEmpty()) {
                    return@withContext formatAddress(addresses[0])
                }
            }
        } catch (e: Exception) {
            Log.e("LocationHelper", "Geocoder failed", e)
        }
        return@withContext "Coordenadas GPS: %.5f, %.5f".format(Locale.US, latitude, longitude)
    }

    private fun formatAddress(addr: Address): String {
        val parts = mutableListOf<String>()
        addr.thoroughfare?.let { street ->
            val num = addr.subThoroughfare ?: ""
            parts.add("$street $num".trim())
        }
        addr.locality?.let { parts.add(it) }
        addr.adminArea?.let { parts.add(it) }
        addr.countryName?.let { parts.add(it) }

        return if (parts.isNotEmpty()) {
            parts.joinToString(", ")
        } else {
            addr.getAddressLine(0) ?: "Localização Detetada"
        }
    }
}
