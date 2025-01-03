package com.example.mapkithw

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.mapkithw.Utils.Companion.ZOOM_BOUNDARY
import com.example.mapkithw.databinding.FragmentMapBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.yandex.mapkit.Animation
import com.yandex.mapkit.MapKit
import com.yandex.mapkit.MapKitFactory
import com.yandex.mapkit.geometry.Point
import com.yandex.mapkit.map.CameraListener
import com.yandex.mapkit.map.CameraPosition
import com.yandex.mapkit.map.CameraUpdateReason
import com.yandex.mapkit.map.Map
import com.yandex.mapkit.map.MapObjectCollection
import com.yandex.mapkit.map.PlacemarkMapObject
import com.yandex.mapkit.traffic.TrafficLayer
import com.yandex.runtime.image.ImageProvider
import java.util.Locale

class MapFragment : Fragment(), CameraListener {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!
    private val startLocation = Point(53.2122, 50.1438)
    private var zoomValue: Float = 16.5f
    private var trafficEnable = false

    private lateinit var mFusedLocationClient: FusedLocationProviderClient
    private val permissionId = 2

    private lateinit var mapObjectCollection: MapObjectCollection
    private lateinit var placemarkMapObject: PlacemarkMapObject

    private lateinit var mapKit: MapKit
    private lateinit var traffic: TrafficLayer

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        setApiKey(savedInstanceState)
        MapKitFactory.initialize(requireContext())
        _binding = FragmentMapBinding.inflate(inflater, container, false)

        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        mapKit = MapKitFactory.getInstance()
        traffic = mapKit.createTrafficLayer(binding.mapView.mapWindow)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        getLocation()
        binding.floatingActionButton.setOnClickListener {
            getTrafficInfo()
        }

    }

    private fun getTrafficInfo() {
        if (!trafficEnable){
            trafficEnable = true
            traffic.isTrafficVisible = true
        } else {
            trafficEnable = false
            traffic.isTrafficVisible = false
        }

    }

    private fun setMarkerInStartLocation(point: Point) {
        val marker = createBitmapFromVector(R.drawable.baseline_location_pin_24)
        mapObjectCollection = binding.mapView.map.mapObjects
        placemarkMapObject = mapObjectCollection.addPlacemark(
            point,
            ImageProvider.fromBitmap(marker)
        )
    }

    private fun createBitmapFromVector(art: Int): Bitmap? {

        val drawable = ContextCompat.getDrawable(requireContext(), art) ?: return null
        val bitmap = Bitmap.createBitmap(
            drawable.intrinsicWidth,
            drawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        ) ?: return null
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    override fun onStart() {
        super.onStart()
        mapKit.onStart()
        binding.mapView.onStart()
    }

    override fun onStop() {
        super.onStop()
        binding.mapView.onStop()
        mapKit.onStop()
    }

    private fun setApiKey(savedInstanceState: Bundle?) {
        val haveApiKey = savedInstanceState?.getBoolean("haveApiKey") ?: false
        if (!haveApiKey) MapKitFactory.setApiKey(Utils.MAPKIT_API_KEY)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("haveApiKey", true)

    }


    private fun moveToStartLocation() {
        binding.mapView.map.move(
            CameraPosition(startLocation, zoomValue, 0.0f, 0.0f)
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @SuppressLint("MissingPermission")
    private fun getLocation() {

        if (checkPermissions()) {
            if (isLocationEnabled()) {
                mFusedLocationClient.lastLocation.addOnCompleteListener(requireActivity()) { task ->
                    val location: Location? = task.result
                    if (location != null) {
                        val geocoder = Geocoder(requireContext(), Locale.getDefault())
                        val list: MutableList<Address>? =
                            geocoder.getFromLocation(location.latitude, location.longitude, 1)
                        val latitude = list?.get(0)!!.latitude
                        val longitude = list[0].longitude

                        val myPosition = Point(latitude, longitude)

                        binding.mapView.map.move(
                            CameraPosition(myPosition, zoomValue, 0.0f, 0.0f),
                            Animation(Animation.Type.SMOOTH, 5f),
                            null
                        )
                        setMarkerInStartLocation(myPosition)
                        binding.mapView.map.addCameraListener(this)


                        Log.d(
                            "@@@", "List: Longitude ${list?.get(0)?.longitude}," +
                                    " Latitude ${list?.get(0)?.latitude}"
                        )
                    }
                }
            } else {
                Toast.makeText(requireContext(), "Please turn on location", Toast.LENGTH_LONG)
                    .show()
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        } else {
            requestPermissions()
        }
        return
    }


    private fun isLocationEnabled(): Boolean {
        val locationManager: LocationManager =
            requireActivity().getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager.isProviderEnabled(
            LocationManager.NETWORK_PROVIDER
        )
    }

    private fun checkPermissions(): Boolean {
        if (ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED

        ) {
            Log.d("@@@", "Разрешение на определение местоположения - дано")
            return true
        }
        Log.d("@@@", "Разрешение на определение местоположения - дано")
        return false
    }

    private fun requestPermissions() {
        ActivityCompat.requestPermissions(
            requireActivity(),
            arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION
            ),
            permissionId
        )
    }

    @SuppressLint("MissingSuperCall")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        if (requestCode == permissionId) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                getLocation()
            }
        }
    }

    override fun onCameraPositionChanged(
        p0: Map,
        p1: CameraPosition,
        p2: CameraUpdateReason,
        p3: Boolean
    ) {
        if (p3) {
            when {
                p1.zoom >= ZOOM_BOUNDARY && zoomValue <= ZOOM_BOUNDARY -> {
                    placemarkMapObject.setIcon(ImageProvider.fromBitmap(createBitmapFromVector(R.drawable.local_icon)))

                }

                p1.zoom <= ZOOM_BOUNDARY && zoomValue >= ZOOM_BOUNDARY -> {
                    placemarkMapObject.setIcon(ImageProvider.fromBitmap(createBitmapFromVector(R.drawable.baseline_location_pin_24)))
                }
            }
            zoomValue = p1.zoom
        }
    }


}