package kg.nurik.poligonapp.base

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Color
import android.util.Log
import com.mapbox.core.constants.Constants.PRECISION_6
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
import com.mapbox.mapboxsdk.plugins.annotation.*
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

    //    private val routeCoordinates: List<Point>? = null
    private val polygon: Polygon? = null
    private val latLngList = arrayListOf<Point>()
    private val markerList = arrayListOf<Marker>()

    var polyHashList: HashMap<String, Pair<LinkedHashMap<String, LatLng>, Boolean>> = HashMap()
    private val POINTS: MutableList<List<Point>> = ArrayList()
    private val OUTER_POINTS: MutableList<Point> = ArrayList()

    override fun onMapLoaded(
        mapBoxMap: MapboxMap,
        style: Style
    ) { // после загрузки карты и стиля выз эта функция
        setupListeners(mapBoxMap) // нажимая на карту вставляем маркер
//        setOnMarkerClickListener(mapBoxMap)
//        loadImages(style) // грузим картинку
//        initSource(style) // грузим
//        initLayer(style)
//        drawLopygon(style)
//        drawPolygon(mapBoxMap)
        mapView.let { symbolManager = SymbolManager(it!!, mapBoxMap, style) }
        symbolManager?.iconAllowOverlap = true
        symbolManager?.textAllowOverlap = true
        if (PermissionUtils.requestLocationPermission(this)) //проверка на гео
            showUserLocation()
    }

    private fun drawLopygon(style: Style) {
        style.addSource(GeoJsonSource("source-id", Polygon.fromLngLats(POINTS)))
        style.addLayerBelow(
            FillLayer("layer-id", "source-id").withProperties(
                fillColor(Color.parseColor("#66018786"))
            ), "settlement-label"
        )
    }

    private fun setupListeners(mapBoxMap: MapboxMap) { //клик на карту
        mapBoxMap.addOnMapClickListener {
            symbol = symbolManager!!.create(
                SymbolOptions()
                    .withLatLng(it)
                    .withIconImage(MARKER_IMAGE)
                    .withIconSize(1.0f)
                    .withTextAnchor("Person First")
                    .withTextSize(23f)
                    .withDraggable(true)
            )
            OUTER_POINTS.add(Point.fromLngLat(it.longitude, it.latitude))
            POINTS.add(OUTER_POINTS)

            symbolManager?.addDragListener(object : OnSymbolDragListener {
                override fun onAnnotationDragStarted(annotation: Symbol?) {
                    Log.d("onAnnotationDragStarted", "onAnnotationDragStarted")
                }

                override fun onAnnotationDrag(annotation: Symbol?) {
                    Log.d("onAnnotationDrag", "onAnnotationDrag")
                }

                @SuppressLint("LongLogTag")
                override fun onAnnotationDragFinished(annotation: Symbol?) {
                    Log.d("onAnnotationDragFinished", "onAnnotationDragFinished")
                }

            })

            drawPolygon(mapBoxMap)

//            mapBoxMap.setStyle(Style.OUTDOORS) { style ->
//                style.addImageAsync(
//                    MARKER_IMAGE,
//                    BitmapUtils.getBitmapFromDrawable
//                        (resources.getDrawable(R.drawable.ic_baseline_radio_button_checked_24))!!
//                )
//                style.addSource(
//                    GeoJsonSource(
//                        "line-source", FeatureCollection.fromFeatures(
//                            arrayOf<Feature>(
//                                Feature.fromGeometry(LineString.fromLngLats(OUTER_POINTS))
//                            )
//                        )
//                    )
//                )
//                style.addLayer(
//                    LineLayer("linelayer", "line-source").withProperties(
//                        PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
//                        PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
//                        PropertyFactory.lineWidth(3f),
//                        PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
//                    )
//                )
//                style.addSource(GeoJsonSource("source-id", Polygon.fromLngLats(POINTS)))
//                style.addLayerBelow(
//                    FillLayer("layer-id", "source-id").withProperties(
//                        fillColor(Color.parseColor("#66018786"))
//                    ), "settlement-label"
//
//                )
//            }
            return@addOnMapClickListener true
        }
    }

//    fun highlightSymbol(t: Symbol?) {
//        var symbols = symbolManager?.annotations
//        symbols.forEach {}
//        val lists = symbols?.size()?.let { ArrayList<Symbol>(it) }
//        symbols.forEach { k, v ->
//            lists?.add(v)
//        }
//
//        symbolManager?.update(lists)
//        t?.iconImage = ID_ICON_LOCATION_SELECTED
//        symbolManager?.update(t)
//    }

    private fun drawPolygon(mapBoxMap: MapboxMap) {
        mapBoxMap.setStyle(Style.OUTDOORS) { style ->
            style.addImageAsync(
                MARKER_IMAGE,
                BitmapUtils.getBitmapFromDrawable
                    (resources.getDrawable(R.drawable.ic_baseline_radio_button_checked_24))!!
            )
            style.addSource(
                GeoJsonSource(
                    "line-source", FeatureCollection.fromFeatures(
                        arrayOf<Feature>(
                            Feature.fromGeometry(LineString.fromLngLats(OUTER_POINTS))
                        )
                    )
                )
            )
            style.addLayer(
                LineLayer("linelayer", "line-source").withProperties(
                    PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                    PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                    PropertyFactory.lineWidth(3f),
                    PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
                )
            )
            style.addSource(GeoJsonSource("source-id", Polygon.fromLngLats(POINTS)))
            style.addLayerBelow(
                FillLayer("layer-id", "source-id").withProperties(
                    fillColor(Color.parseColor("#66018786"))
                ), "settlement-label"

            )
        }
    }
//        mapBoxMap.setOnMarkerClickListener { marker ->
//            marker.position = LatLng(42.842206, 74.622755)
//            // Show a toast with the title of the selected marker
//            Toast.makeText(this@BaseMapActivity, marker.title, Toast.LENGTH_LONG).show()
//            true
//        }

    private fun initLayer(style: Style) { // рисует линию
        style.addLayer(
            LineLayer("linelayer", "line-source").withProperties(
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
                PropertyFactory.lineWidth(3f),
                PropertyFactory.lineColor(Color.parseColor("#e55e5e"))
            )
        )
    }

    private fun addMarker(newLatLng: LatLng) { // добавляем маркер

//     symbol?.let { symbolManager?.delete(it) } // при каждом дабвлении удаляется старый маркер и проверка что гео не ноль

        val symbolOptions = SymbolOptions()
            .withLatLng(newLatLng)
            .withIconImage(MARKER_IMAGE)
        symbol = symbolManager?.create(symbolOptions) // для начала создаем symbolManager
        OUTER_POINTS.add(Point.fromLngLat(newLatLng.longitude, newLatLng.latitude))
        POINTS.add(OUTER_POINTS)
    }

    private fun getDirections(
        newLatLng: LatLng,
        oldLatLng: LatLng
    ) { // рисует линию между мест до маркера и считает

        MapUtils.getDirections(oldLatLng, newLatLng) {
            val newLatLng = map?.style?.getSourceAs<GeoJsonSource>(LINE_SOURCE)
            if (newLatLng != null) {
                if (it?.geometry() != null) {
                    newLatLng.setGeoJson(LineString.fromPolyline(it.geometry()!!, PRECISION_6))
                }
            }
        }
    }


    private fun initSource(style: Style) {
        style.addSource(
            GeoJsonSource(
                "line-source", FeatureCollection.fromFeatures(
                    arrayOf<Feature>(
                        Feature.fromGeometry(LineString.fromLngLats(OUTER_POINTS))
                    )
                )
            )
        ) // путь отрисовки пользователя
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private fun loadImages(style: Style) {
        style.addImageAsync(
            MARKER_IMAGE,
            BitmapUtils.getBitmapFromDrawable
                (resources.getDrawable(R.drawable.ic_baseline_radio_button_checked_24))!!
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PermissionUtils.LOCATION_REQUEST_CODE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                showUserLocation()
            }
        }
    }

    @SuppressLint("MissingPermission", "Range")
    private fun showUserLocation() {
        map?.style?.let {
            val locationComponent = map?.locationComponent
            locationComponent?.activateLocationComponent(
                LocationComponentActivationOptions.builder(applicationContext, it)
                    .build()
            )

            locationComponent?.isLocationComponentEnabled = true
            locationComponent?.cameraMode = CameraMode.TRACKING
            locationComponent?.renderMode = RenderMode.COMPASS
            val location = locationComponent?.lastKnownLocation
            val latLng = MapUtils.locationToLatLng(location) // coordinate
            animateCamera(latLng) // камера зуум
        }
    }

    private fun animateCamera(latLng: LatLng) { // камера зуум
        val cm = CameraPosition.Builder()
            .target(latLng)
            .zoom(15.5)
            .build()

        map?.animateCamera(CameraUpdateFactory.newCameraPosition(cm), 5000)
    }

    companion object {
        const val MARKER_IMAGE = "MARKER_IMAGE"
        const val LINE_SOURCE = "LINE_SOURCE"
        const val LINE_LAYER = "LINE_LAYER"
        private val ID_ICON_LOCATION_SELECTED = "location_selected"
    }
}