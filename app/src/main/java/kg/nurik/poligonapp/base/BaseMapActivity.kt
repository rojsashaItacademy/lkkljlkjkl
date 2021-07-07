package kg.nurik.poligonapp.base

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.annotations.Marker
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.Property
import com.mapbox.mapboxsdk.style.layers.PropertyFactory
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.fillColor
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kg.nurik.poligonapp.R
import kg.nurik.poligonapp.utils.MapUtils
import kg.nurik.poligonapp.utils.PermissionUtils

abstract class BaseMapActivity : SupportMapActivity() {

    private var symbol: Symbol? = null
    private var symbolManager: SymbolManager? = null
    private var mapBoxMap: MapboxMap? = null
    private val POINTS: MutableList<List<Point>> = ArrayList()
    private val OUTER_POINTS: MutableList<Point> = ArrayList()

    override fun onMapLoaded(
        mapBoxMap: MapboxMap,
        style: Style
    ) {
        this.mapBoxMap = mapBoxMap
        mapView.let { symbolManager = SymbolManager(it!!, mapBoxMap, style) }
        symbolManager?.iconAllowOverlap = true
        symbolManager?.textAllowOverlap = true
        setupListeners(mapBoxMap)
        if (PermissionUtils.requestLocationPermission(this)) //проверка на гео
            showUserLocation()
    }

    private fun setupListeners(mapBoxMap: MapboxMap) {

        mapBoxMap.addOnMapClickListener {
            val iconSize = if (POINTS.size == 0) 1.3f else 1.0f
            symbol = symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(it)
                    .withIconImage("MARKER_IMAGE")
                    .withIconSize(iconSize)
                    .withTextAnchor("Person First")
                    .withTextSize(23f)
                    .withDraggable(true)
            )
            OUTER_POINTS.add(Point.fromLngLat(it.longitude, it.latitude))
            POINTS.add(OUTER_POINTS)

            drawPolygon(mapBoxMap)
            return@addOnMapClickListener false
        }

        symbolManager?.addDragListener(object : OnSymbolDragListener {
            override fun onAnnotationDragStarted(annotation: Symbol?) {}
            override fun onAnnotationDrag(annotation: Symbol?) {}

            @SuppressLint("LongLogTag")
            override fun onAnnotationDragFinished(annotation: Symbol?) {
                annotation?.id?.let { itemId ->
                    val id = itemId.toInt()
                    OUTER_POINTS[id] = annotation.geometry
                    POINTS.clear()
                    POINTS.add(OUTER_POINTS)
                    drawPolygon(mapBoxMap)
                }
            }
        })

        symbolManager?.addClickListener {
            false
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun drawPolygon(mapBoxMap: MapboxMap) {
        mapBoxMap.getStyle { style ->
            style.addImageAsync(
                "MARKER_IMAGE",
                BitmapUtils.getBitmapFromDrawable
                    (resources.getDrawable(R.drawable.ic_baseline_radio_button_checked_24))!!
            )
            if (OUTER_POINTS.size < 2)
                addLayer("sourceOne")
            else editLayer("sourceOne")
        }
    }

    private fun editLayer(nameSource: String) {
        mapBoxMap?.getStyle { style ->
            style.getSourceAs<GeoJsonSource>("line-source$nameSource")
                ?.setGeoJson(LineString.fromLngLats(OUTER_POINTS))
            style.getSourceAs<GeoJsonSource>("linelayer$nameSource")
                ?.setGeoJson(LineString.fromLngLats(OUTER_POINTS))
            style.getSourceAs<GeoJsonSource>("source-id$nameSource")
                ?.setGeoJson(Polygon.fromLngLats(POINTS))
        }
    }

    private fun addLayer(nameSource: String) {
        mapBoxMap?.getStyle { style ->
            style.addSource(
                GeoJsonSource(
                    "line-source$nameSource", FeatureCollection.fromFeatures(
                        arrayOf<Feature>(
                            Feature.fromGeometry(LineString.fromLngLats(OUTER_POINTS)))))
            )
            style.addLayer(
                LineLayer("linelayer$nameSource", "line-source$nameSource").withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineColor(Color.parseColor("#e55e5e")))
            )
            style.addSource(GeoJsonSource("source-id$nameSource", Polygon.fromLngLats(POINTS)))
            style.addLayerBelow(
                FillLayer("layer-id$nameSource", "source-id$nameSource").withProperties(
                    fillColor(Color.parseColor("#66018786"))), "settlement-label"
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation() }
        }
    }

    @SuppressLint("MissingPermission", "Range")
    private fun showUserLocation() {
        map?.style?.let {
            val locationComponent = map?.locationComponent
            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(applicationContext, it).build())

            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.COMPASS
            val location = locationComponent?.lastKnownLocation
            val latLng = MapUtils.locationToLatLng(location)
            animateCamera(latLng)
        }
    }

    private fun animateCamera(latLng: LatLng) {
        val cm = CameraPosition.Builder()
            .target(latLng)
            .zoom(15.5)
            .build()
        map?.animateCamera(CameraUpdateFactory.newCameraPosition(cm), 5000)
    }
}