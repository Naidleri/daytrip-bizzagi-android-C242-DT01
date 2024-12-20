package com.bizzagi.daytrip.ui.Maps.AddPlan

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Geocoder
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bizzagi.daytrip.R
import com.bizzagi.daytrip.databinding.FragmentStartLocationMapsBinding
import com.bizzagi.daytrip.ui.Maps.MapsViewModel
import com.bizzagi.daytrip.utils.ViewModelFactory
import com.google.android.gms.common.api.Status
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@Suppress("DEPRECATION")
class StartLocationMapsFragment : Fragment(), OnMapReadyCallback {
    private var currentMarker: Marker? = null
    private lateinit var mMap: GoogleMap
    private var _binding: FragmentStartLocationMapsBinding? = null
    private val binding get() = _binding!!
    private lateinit var placesClient: PlacesClient
    private lateinit var autocompleteFragment: AutocompleteSupportFragment

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val viewModel: MapsViewModel by viewModels {
        ViewModelFactory.getInstance(requireActivity())
    }

    private val indonesiaBounds = LatLngBounds(
        LatLng(-11.007375, 95.007307),
        LatLng(6.076912, 141.019454)
    )

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentStartLocationMapsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Places.initialize(requireContext(), getString(R.string.google_maps_api_key))
        placesClient = Places.createClient(requireContext())

        setupAutocomplete()

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapStartPoint) as SupportMapFragment
        mapFragment.getMapAsync(this)
    }

    @SuppressLint("MissingPermission")
    private fun focusOnUserLocation() {
        if (::mMap.isInitialized) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                location?.let {
                    val userLatLng = LatLng(it.latitude, it.longitude)
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 15f))
                }
            }
        }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        getMyLocation()

        mMap.setLatLngBoundsForCameraTarget(indonesiaBounds)
        mMap.setMinZoomPreference(5f)
        mMap.setMaxZoomPreference(20f)

        mMap.setOnMapLoadedCallback {
            val padding = 200
            mMap.setPadding(0, 0, 0, padding)
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    android.Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                focusOnUserLocation()
            }
        }

        mMap.setOnMapClickListener { latLng ->
            Log.d("StartLocationMapsFragment", "User memilih lokasi: $latLng")

            currentMarker?.remove()
            currentMarker = mMap.addMarker(
                MarkerOptions()
                    .position(latLng)
                    .title("Lokasi Awal")
                    .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED))
            )

            viewModel.setSelectedLocation(latLng.latitude, latLng.longitude)
            updateLocationDetails(latLng)
        }

        binding.btnKonfirmasi.setOnClickListener {
            currentMarker?.let { marker ->
                (activity as? PickRegionActivity)?.onLocationSelected(
                    marker.position.latitude,
                    marker.position.longitude
                )
                parentFragmentManager.popBackStack()
            } ?: run {
                Log.e("StartLocationMapsFragment", "No marker selected.")
            }
        }
    }


    private fun setupAutocomplete() {
        autocompleteFragment = childFragmentManager
            .findFragmentById(R.id.autocomplete_fragment) as AutocompleteSupportFragment

        autocompleteFragment.setPlaceFields(
            listOf(
                Place.Field.ID,
                Place.Field.NAME,
                Place.Field.ADDRESS,
                Place.Field.LAT_LNG,
                Place.Field.TYPES
            )
        )
        autocompleteFragment.setHint("Cari lokasi")
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                place.latLng?.let { latLng ->
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                }
            }

            override fun onError(status: Status) {
                Log.e("StartLocationMapsFragment", "Autocomplete error: $status")
            }
        })
    }

    private fun getMyLocation() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            mMap.isMyLocationEnabled = true
        } else {
            requestPermissionLauncher.launch(android.Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                getMyLocation()
            }
        }

    @SuppressLint("SetTextI18n")
    private fun updateLocationDetails(latLng: LatLng) {
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                val geocoder = Geocoder(requireContext(), Locale.getDefault())
                val addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1)
                val addressText = if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    buildString {
                        append(address.getAddressLine(0))
                        if (address.locality != null) append(", ${address.locality}")
                        if (address.adminArea != null) append(", ${address.adminArea}")
                    }
                } else {
                    "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
                }
                withContext(Dispatchers.Main) {
                    binding.tvDetailLokasi.text = addressText
                }
            } catch (e: Exception) {
                Log.e("StartLocationMapsFragment", "Error fetching location details", e)
                withContext(Dispatchers.Main) {
                    binding.tvDetailLokasi.text = "Lat: ${latLng.latitude}, Lng: ${latLng.longitude}"
                }
            }
        }
    }
}
