package kg.nurik.poligonapp.ui

import android.annotation.SuppressLint
import android.graphics.Color
import android.location.Location
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.geometry.LatLng
import com.mapbox.mapboxsdk.location.LocationComponentActivationOptions
import com.mapbox.mapboxsdk.location.modes.CameraMode
import com.mapbox.mapboxsdk.location.modes.RenderMode
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.OnSymbolDragListener
import com.mapbox.mapboxsdk.plugins.annotation.Symbol
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.*
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import com.mapbox.mapboxsdk.utils.BitmapUtils
import kg.nurik.poligonapp.R
import kg.nurik.poligonapp.utils.PermissionUtils

class MainActivity : AppCompatActivity(), OnMapReadyCallback {

    private var fillLayerPointList: MutableList<Point?> = ArrayList()
    private var lineLayerPointList: MutableList<Point?> = ArrayList()
    private var circleLayerFeatureList: MutableList<Feature> = ArrayList()
    private var listOfList: MutableList<List<Point?>>? = null
    private var mapView: MapView? = null
    private var mapboxMap: MapboxMap? = null
    private var circleSource: GeoJsonSource? = null
    private var fillSource: GeoJsonSource? = null
    private var lineSource: GeoJsonSource? = null
    private var firstPointOfPolygon: Point? = null
    private var symbolManager: SymbolManager? = null
    private var symbol: Symbol? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Mapbox.getInstance(this, getString(R.string.access_token))
        setContentView(R.layout.activity_main)
        mapView = findViewById(R.id.mapView)
        mapView?.onCreate(savedInstanceState)
        mapView?.getMapAsync(this)
    }

    override fun onMapReady(mapboxMap: MapboxMap) {
        this.mapboxMap = mapboxMap
        mapboxMap.setStyle(
            Style.OUTDOORS
        ) { style ->
            if (PermissionUtils.requestLocationPermission(this)) //проверка на гео
                showUserLocation()

            mapView.let { symbolManager = SymbolManager(it!!, mapboxMap, style) }
            symbolManager?.iconAllowOverlap = true
            symbolManager?.textAllowOverlap = true

            circleSource = initCircleSource(style)
            fillSource = initFillSource(style)
            lineSource = initLineSource(style)

            initCircleLayer(style)
            initLineLayer(style)
            initFillLayer(style)
            initFloatingActionButtonClickListeners()
        }
    }

    @SuppressLint("MissingPermission", "Range")
    private fun showUserLocation() {
        mapboxMap?.style?.let {
            val locationComponent = mapboxMap?.locationComponent
            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(applicationContext, it).build()
            )

            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.COMPASS
            val location = locationComponent?.lastKnownLocation
            val latLng = locationToLatLng(location)
            animateCamera(latLng)
        }
    }

    private fun animateCamera(latLng: LatLng) {
        val cm = CameraPosition.Builder()
            .target(latLng)
            .zoom(15.5)
            .build()
        mapboxMap?.animateCamera(CameraUpdateFactory.newCameraPosition(cm), 5000)
    }

    private fun locationToLatLng(location: Location?) =
        LatLng(location?.latitude ?: 0.0, location?.longitude ?: 0.0)


    private fun initFloatingActionButtonClickListeners() {

        val clearBoundariesFab: Button = findViewById(R.id.clear_button)
        clearBoundariesFab.setOnClickListener { clearEntireMap() }

        val saveBoundariesFab: Button = findViewById(R.id.save_button)
        saveBoundariesFab.setOnClickListener { saveEntireMap() }

        mapboxMap?.addOnMapClickListener {

            val symbol = symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(it)
                    .withIconImage("MARKER_IMAGE")
                    .withIconSize(1.0f)
                    .withDraggable(true)
            )

            // Make note of the first map click location so that it can be used to create aclosed polygon later on
            if (circleLayerFeatureList.size == 0) {
                firstPointOfPolygon = symbol.geometry
            }

            // Add the click point to the circle layer and update the display of the circle layer data
            circleLayerFeatureList.add(Feature.fromGeometry(symbol.geometry))
            if (circleSource != null) {
                circleSource!!.setGeoJson(
                    FeatureCollection.fromFeatures
                        (circleLayerFeatureList)
                )
            }

            // Add the click point to the line layer and update the display of the line layer data
            if (circleLayerFeatureList.size < 3) {
                lineLayerPointList.add(symbol.geometry)
            } else if (circleLayerFeatureList.size == 3) {
                lineLayerPointList.add(symbol.geometry)
                lineLayerPointList.add(firstPointOfPolygon)
            } else {
                lineLayerPointList.removeAt(circleLayerFeatureList.size - 1)
                lineLayerPointList.add(symbol.geometry)
                lineLayerPointList.add(firstPointOfPolygon)
            }

            // Add the click point to the fill layer and update the display of the fill layer data
            if (circleLayerFeatureList.size < 3) {
                fillLayerPointList.add(symbol.geometry)
            } else if (circleLayerFeatureList.size == 3) {
                fillLayerPointList.add(symbol.geometry)
                fillLayerPointList.add(firstPointOfPolygon)
            } else {
                fillLayerPointList.removeAt(fillLayerPointList.size - 1)
                fillLayerPointList.add(symbol.geometry)
                fillLayerPointList.add(firstPointOfPolygon)
            }
            listOfList = ArrayList()
            (listOfList as ArrayList<List<Point?>>).add(fillLayerPointList)

            lineSource?.setGeoJson(
                FeatureCollection.fromFeatures(
                    arrayOf<Feature>(
                        Feature.fromGeometry(
                            LineString.fromLngLats(lineLayerPointList)
                        )
                    )
                )
            )

            drawPolygon()
            return@addOnMapClickListener false
        }

        symbolManager?.addDragListener(object : OnSymbolDragListener {
            override fun onAnnotationDragStarted(annotation: Symbol?) {}
            override fun onAnnotationDrag(annotation: Symbol?) {}

            @SuppressLint("LongLogTag")
            override fun onAnnotationDragFinished(annotation: Symbol?) {
                annotation?.id?.let { itemId ->
                    val id = itemId.toInt()
                    fillLayerPointList[id] = annotation.geometry
                    lineLayerPointList.addAll(fillLayerPointList)
                    drawPolygon()
                }
            }
        })
    }

    private fun drawPolygon() {
        val finalFeatureList: MutableList<Feature> = ArrayList()
        finalFeatureList.add(Feature.fromGeometry(Polygon.fromLngLats(listOfList as ArrayList<List<Point?>>)))
        val newFeatureCollection = FeatureCollection.fromFeatures(finalFeatureList)
        if (fillSource != null) {
            fillSource!!.setGeoJson(newFeatureCollection)
        }
    }


    private fun saveEntireMap() {
        Toast.makeText(
            this,
            "size point = " + fillLayerPointList.size.toString(),
            Toast.LENGTH_LONG
        ).show()
    }

    private fun clearEntireMap() {
        fillLayerPointList = ArrayList()
        circleLayerFeatureList = ArrayList()
        lineLayerPointList = ArrayList()
        circleSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        lineSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        fillSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        symbolManager?.deleteAll()
    }

    private fun initCircleSource(loadedMapStyle: Style): GeoJsonSource {
        val circleFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val circleGeoJsonSource = GeoJsonSource(CIRCLE_SOURCE_ID, circleFeatureCollection)
        loadedMapStyle.addSource(circleGeoJsonSource)
        return circleGeoJsonSource
    }

    private fun initCircleLayer(loadedMapStyle: Style) {
        loadedMapStyle.addImage(
            "MARKER_IMAGE", BitmapUtils.getBitmapFromDrawable(
                resources.getDrawable(R.drawable.ic_baseline_radio_button_checked_24)
            )!!
        )
    }

    private fun initFillSource(loadedMapStyle: Style): GeoJsonSource {
        val fillFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val fillGeoJsonSource = GeoJsonSource(FILL_SOURCE_ID, fillFeatureCollection)
        loadedMapStyle.addSource(fillGeoJsonSource)
        return fillGeoJsonSource
    }

    private fun initFillLayer(loadedMapStyle: Style) {
        val fillLayer = FillLayer(FILL_LAYER_ID, FILL_SOURCE_ID)
        fillLayer.setProperties(
            fillOpacity(.6f),
            fillColor(Color.parseColor("#6FA6B8"))
        )
        loadedMapStyle.addLayerBelow(fillLayer, LINE_LAYER_ID)
    }

    private fun initLineSource(loadedMapStyle: Style): GeoJsonSource {
        val lineFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val lineGeoJsonSource = GeoJsonSource(LINE_SOURCE_ID, lineFeatureCollection)
        loadedMapStyle.addSource(lineGeoJsonSource)
        return lineGeoJsonSource
    }

    private fun initLineLayer(loadedMapStyle: Style) {
        val lineLayer = LineLayer(LINE_LAYER_ID, LINE_SOURCE_ID)
        lineLayer.setProperties(
            lineCap(Property.LINE_CAP_ROUND),
            lineJoin(Property.LINE_JOIN_ROUND),
            lineWidth(2f), //ширина линии
            lineColor(Color.parseColor("#f7100a"))
        )
        loadedMapStyle.addLayerBelow(
            lineLayer, CIRCLE_LAYER_ID
        )
    }

    public override fun onResume() {
        super.onResume()
        mapView?.onResume()
    }

    override fun onStart() {
        super.onStart()
        mapView?.onStart()
    }

    override fun onStop() {
        super.onStop()
        mapView?.onStop()
    }

    public override fun onPause() {
        super.onPause()
        mapView?.onPause()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView?.onLowMemory()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView?.onDestroy()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mapView?.onSaveInstanceState(outState)
    }

    companion object {
        private const val CIRCLE_SOURCE_ID = "circle-source-id"
        private const val FILL_SOURCE_ID = "fill-source-id"
        private const val LINE_SOURCE_ID = "line-source-id"
        private const val CIRCLE_LAYER_ID = "circle-layer-id"
        private const val FILL_LAYER_ID = "fill-layer-polygon-id"
        private const val LINE_LAYER_ID = "line-layer-id"
    }
}