package kg.nurik.poligonapp.ui

import android.os.Bundle
import com.mapbox.mapboxsdk.maps.Style
import kg.nurik.poligonapp.R
import kg.nurik.poligonapp.base.BaseMapActivity
import kg.nurik.poligonapp.databinding.ActivityMainBinding
import kg.nurik.poligonapp.utils.viewBinding


class MainActivity : BaseMapActivity() {

    override fun getResId() = R.layout.activity_main
    override fun getMapViewId() = R.id.mapView
    private val binding by viewBinding(ActivityMainBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupListeners()
    }

    private fun setupListeners() {
        binding.undoFab.setOnClickListener {
            map?.setStyle(Style.DARK)
        }
    }
}