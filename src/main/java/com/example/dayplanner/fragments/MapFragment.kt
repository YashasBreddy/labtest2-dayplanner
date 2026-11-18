package com.example.dayplanner.fragments

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.dayplanner.R
import com.example.dayplanner.databinding.FragmentMapBinding
import com.example.dayplanner.utils.MapsHelper
import com.example.dayplanner.viewmodels.MainViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.launch

class MapFragment : Fragment(), OnMapReadyCallback {

    private var _binding: FragmentMapBinding? = null
    private val binding get() = _binding!!

    private val viewModel: MainViewModel by activityViewModels()
    private var googleMap: GoogleMap? = null

    private val locationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false
        val coarseGranted = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] ?: false

        if (fineGranted || coarseGranted) {
            enableMyLocationOnMap()
            viewModel.fetchLocation()
        } else {
            Toast.makeText(requireContext(), "Location permission denied.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.mapContainer) as? SupportMapFragment
            ?: SupportMapFragment.newInstance().also {
                childFragmentManager.beginTransaction()
                    .replace(R.id.mapContainer, it)
                    .commit()
            }
        mapFragment.getMapAsync(this)

        setupListeners()
    }

    private fun setupListeners() {
        binding.btnMapPlanMyDay.setOnClickListener {
            viewModel.planMyDay()
        }

        binding.btnMapClearRoute.setOnClickListener {
            viewModel.clearRoute()
        }

        binding.fabMyLocation.setOnClickListener {
            checkAndRequestLocationPermission()
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        map.uiSettings.isZoomControlsEnabled = true
        map.uiSettings.isCompassEnabled = true
        map.uiSettings.isMapToolbarEnabled = true

        checkAndEnableMyLocation()
        observeViewModel()
    }

    private fun checkAndRequestLocationPermission() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted || coarseGranted) {
            viewModel.fetchLocation()
            centerOnUserLocation()
        } else {
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    private fun checkAndEnableMyLocation() {
        val fineGranted = ContextCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (fineGranted) {
            enableMyLocationOnMap()
        }
    }

    private fun enableMyLocationOnMap() {
        try {
            googleMap?.isMyLocationEnabled = true
        } catch (e: SecurityException) {
            // Permission denied
        }
    }

    private fun centerOnUserLocation() {
        val loc = viewModel.userLocationState.value
        if (loc != null) {
            googleMap?.animateCamera(CameraUpdateFactory.newLatLngZoom(LatLng(loc.first, loc.second), 14f))
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.placesState.collect { _ ->
                        updateMapUI()
                    }
                }

                launch {
                    viewModel.routeResultState.collect { route ->
                        if (route != null) {
                            binding.tvMapRouteSummary.text = route.summary
                            binding.btnMapClearRoute.visibility = View.VISIBLE
                        } else {
                            binding.tvMapRouteSummary.text = getString(R.string.no_route_yet)
                            binding.btnMapClearRoute.visibility = View.GONE
                        }
                        updateMapUI()
                    }
                }

                launch {
                    viewModel.userLocationState.collect { _ ->
                        updateMapUI()
                    }
                }
            }
        }
    }

    private fun updateMapUI() {
        val map = googleMap ?: return
        val places = viewModel.placesState.value
        val route = viewModel.routeResultState.value
        val location = viewModel.userLocationState.value

        MapsHelper.renderPlacesOnMap(requireContext(), map, places, route, location)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
