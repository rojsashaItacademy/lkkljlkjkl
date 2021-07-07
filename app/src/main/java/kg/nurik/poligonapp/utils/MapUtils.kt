package kg.nurik.poligonapp.utils

import android.util.Log
import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.geojson.Point
import com.mapbox.mapboxsdk.geometry.LatLng
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

object MapUtils {

    fun getDirections(
        startDestination: LatLng, destination: LatLng,
        result: ((item: DirectionsRoute?) -> Unit)? = null  //колбэк возвращаяемая функ
    ) {
        val client = MapboxDirections.builder()
            .accessToken("pk.eyJ1IjoianVtYWJla292IiwiYSI6ImNrZm5uc3JkOTA5b3gycnBjeGNjYWRldWwifQ.yoHumQyfeeIEdAXl0dtyKw")
            .origin(Point.fromLngLat(startDestination.longitude ?: 0.0, startDestination.latitude ?: 0.0))
            .overview(DirectionsCriteria.OVERVIEW_FULL)
            .destination(Point.fromLngLat(destination.longitude, destination.latitude))
            .profile(DirectionsCriteria.PROFILE_WALKING) // какой путь нужен?:пешком , на машине и т.д
            .build()

        client?.enqueueCall(object : Callback<DirectionsResponse> {
            override fun onResponse(
                call: Call<DirectionsResponse>,
                response: Response<DirectionsResponse>
            ) {
                val currentRoute = response.body()?.routes()?.first() // для последней гео
                result?.invoke(currentRoute)
            }

            override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                Log.d("onFailure", "Error")
            }
        })
    }
}