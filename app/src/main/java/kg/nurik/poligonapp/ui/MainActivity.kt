package kg.nurik.poligonapp.ui

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.mapbox.geojson.*
import com.mapbox.mapboxsdk.Mapbox
import com.mapbox.mapboxsdk.camera.CameraPosition
import com.mapbox.mapboxsdk.camera.CameraUpdateFactory
import com.mapbox.mapboxsdk.maps.MapView
import com.mapbox.mapboxsdk.maps.MapboxMap
import com.mapbox.mapboxsdk.maps.OnMapReadyCallback
import com.mapbox.mapboxsdk.maps.Style
import com.mapbox.mapboxsdk.plugins.annotation.SymbolManager
import com.mapbox.mapboxsdk.plugins.annotation.SymbolOptions
import com.mapbox.mapboxsdk.style.layers.CircleLayer
import com.mapbox.mapboxsdk.style.layers.FillLayer
import com.mapbox.mapboxsdk.style.layers.LineLayer
import com.mapbox.mapboxsdk.style.layers.PropertyFactory.*
import com.mapbox.mapboxsdk.style.sources.GeoJsonSource
import kg.nurik.poligonapp.R


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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Mapbox access token is configured here. This needs to be called either in your application
// object or in the same activity which contains the mapview.
        Mapbox.getInstance(this, getString(R.string.access_token))

// This contains the MapView in XML and needs to be called after the access token is configured.
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
            mapboxMap.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.Builder()
                        .zoom(16.0)
                        .build()
                ), 4000
            )

            mapView.let { symbolManager = SymbolManager(it!!, mapboxMap, style) }
            symbolManager?.iconAllowOverlap = true
            symbolManager?.textAllowOverlap = true
            // Add sources to the map
            circleSource = initCircleSource(style)
            fillSource = initFillSource(style)
            lineSource = initLineSource(style)

            // Add layers to the map
            initCircleLayer(style)
            initLineLayer(style)
            initFillLayer(style)
            initFloatingActionButtonClickListeners()
        }
    }

    /**
     * Set the button click listeners
     */
    private fun initFloatingActionButtonClickListeners() {

        val clearBoundariesFab: Button = findViewById(R.id.clear_button)
        clearBoundariesFab.setOnClickListener { clearEntireMap() }

        val dropPinFab = findViewById<FloatingActionButton>(R.id.fab)

        mapboxMap?.addOnMapClickListener {

            val iconSize = if (circleLayerFeatureList.size == 0) 2.0f else 1.0f
            val symbol = symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(it)
                    .withIconImage("MARKER_IMAGE")
                    .withIconSize(iconSize)
                    .withTextAnchor("Person First")
                    .withTextSize(23f)
                    .withDraggable(true)
            )
//            OUTER_POINTS.add(Point.fromLngLat(LatLng.longitude, LatLng.latitude))
//            POINTS.add(OUTER_POINTS)


//            dropPinFab.setOnClickListener { // Use the map click location to create a Point object
//            val mapTargetPoint: Point = Point.fromLngLat(
//                mapboxMap!!.cameraPosition.target.longitude,
//                mapboxMap!!.cameraPosition.target.latitude
//            )

            // Make note of the first map click location so that it can be used to create a closed polygon later on
            if (circleLayerFeatureList.size == 0) {
                firstPointOfPolygon = symbol.geometry
            }

            // Add the click point to the circle layer and update the display of the circle layer data
            circleLayerFeatureList.add(Feature.fromGeometry(symbol.geometry))
            if (circleSource != null) {
                circleSource!!.setGeoJson(FeatureCollection.fromFeatures(circleLayerFeatureList))
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
            lineSource?.setGeoJson(
                FeatureCollection.fromFeatures(
                    arrayOf<Feature>(
                        Feature.fromGeometry(
                            LineString.fromLngLats(lineLayerPointList)
                        )
                    )
                )
            )

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
            val finalFeatureList: MutableList<Feature> = ArrayList()
            finalFeatureList.add(Feature.fromGeometry(Polygon.fromLngLats(listOfList as ArrayList<List<Point?>>)))
            val newFeatureCollection = FeatureCollection.fromFeatures(finalFeatureList)
            if (fillSource != null) {
                fillSource!!.setGeoJson(newFeatureCollection)
            }
            return@addOnMapClickListener false
        }
    }


    /**
     * Remove the drawn area from the map by resetting the FeatureCollections used by the layers' sources
     */
    private fun clearEntireMap() {
        fillLayerPointList = ArrayList()
        circleLayerFeatureList = ArrayList()
        lineLayerPointList = ArrayList()
        circleSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        lineSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
        fillSource?.setGeoJson(FeatureCollection.fromFeatures(arrayOf<Feature>()))
    }

    /**
     * Set up the CircleLayer source for showing map click points
     */
    private fun initCircleSource(loadedMapStyle: Style): GeoJsonSource {
        val circleFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val circleGeoJsonSource = GeoJsonSource(CIRCLE_SOURCE_ID, circleFeatureCollection)
        loadedMapStyle.addSource(circleGeoJsonSource)
        return circleGeoJsonSource
    }

    /**
     * Set up the CircleLayer for showing polygon click points
     */
    private fun initCircleLayer(loadedMapStyle: Style) {
        val circleLayer = CircleLayer(
            CIRCLE_LAYER_ID,
            CIRCLE_SOURCE_ID
        )
        circleLayer.setProperties(
            circleRadius(7f),
            circleColor(Color.parseColor("#d004d3"))
        )
        loadedMapStyle.addLayer(circleLayer)
    }

    /**
     * Set up the FillLayer source for showing map click points
     */
    private fun initFillSource(loadedMapStyle: Style): GeoJsonSource {
        val fillFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val fillGeoJsonSource = GeoJsonSource(FILL_SOURCE_ID, fillFeatureCollection)
        loadedMapStyle.addSource(fillGeoJsonSource)
        return fillGeoJsonSource
    }

    /**
     * Set up the FillLayer for showing the set boundaries' polygons
     */
    private fun initFillLayer(loadedMapStyle: Style) {
        val fillLayer = FillLayer(
            FILL_LAYER_ID,
            FILL_SOURCE_ID
        )
        fillLayer.setProperties(
            fillOpacity(.6f),
            fillColor(Color.parseColor("#00e9ff"))
        )
        loadedMapStyle.addLayerBelow(fillLayer, LINE_LAYER_ID)
    }

    /**
     * Set up the LineLayer source for showing map click points
     */
    private fun initLineSource(loadedMapStyle: Style): GeoJsonSource {
        val lineFeatureCollection: FeatureCollection =
            FeatureCollection.fromFeatures(arrayOf<Feature>())
        val lineGeoJsonSource = GeoJsonSource(LINE_SOURCE_ID, lineFeatureCollection)
        loadedMapStyle.addSource(lineGeoJsonSource)
        return lineGeoJsonSource
    }

    /**
     * Set up the LineLayer for showing the set boundaries' polygons
     */
    private fun initLineLayer(loadedMapStyle: Style) {
        val lineLayer = LineLayer(
            LINE_LAYER_ID,
            LINE_SOURCE_ID
        )
        lineLayer.setProperties(
            lineColor(Color.WHITE),
            lineWidth(5f)
        )
        loadedMapStyle.addLayerBelow(lineLayer, CIRCLE_LAYER_ID)
    }

    // Add the mapView lifecycle to the activity's lifecycle methods
    override fun onResume() {
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