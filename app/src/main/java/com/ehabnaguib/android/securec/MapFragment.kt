package com.ehabnaguib.android.securec

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.MenuProvider
import androidx.fragment.app.setFragmentResult
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.navArgs
import com.ehabnaguib.android.securec.databinding.FragmentMapBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener

class MapFragment : Fragment() {

    private var _binding : FragmentMapBinding? = null
    private val binding : FragmentMapBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val args: MapFragmentArgs by navArgs()

    private var location : LatLng? = null

    private lateinit var placesClient: PlacesClient


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(createMenuProvider(), viewLifecycleOwner, Lifecycle.State.RESUMED)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.search_view_fragment)
                as AutocompleteSupportFragment

        location = args.location as LatLng?

        // Updating the location upon user clicks on the map
        mapFragment?.getMapAsync { googleMap ->
            googleMap.setOnMapClickListener { latLng ->
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
                location = latLng
                setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to location))
            }
        }

        updateMap(mapFragment, location)

        // Getting the API key from the manifest
        val apiKey = requireContext().applicationContext.packageManager
            .getApplicationInfo(requireContext().packageName, PackageManager.GET_META_DATA)
            .metaData.getString("com.google.android.geo.API_KEY")

        // Initialize the Places API
        if (!Places.isInitialized()) {
            if (apiKey != null) {
                Places.initialize(requireContext().applicationContext, apiKey)
            }
        }
        placesClient = Places.createClient(requireContext())

        // Specify the types of place data to return
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                updateMap(mapFragment, place.latLng)
            }
            override fun onError(status: Status) {

            }
        })
        binding.saveButton.setOnClickListener {
            requireActivity().supportFragmentManager.popBackStack()
        }
    }

    private fun createMenuProvider(): MenuProvider {
        return object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_map, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.clear_map -> {
                        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
                        mapFragment?.getMapAsync { googleMap ->
                            googleMap.clear()
                            Toast.makeText(requireContext(), "Location cleared.", Toast.LENGTH_SHORT).show()
                            location = null
                            setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to null))
                        }
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun updateMap(mapFragment: SupportMapFragment?, newLocation : LatLng?) {
        mapFragment?.getMapAsync { googleMap ->
            if (newLocation != null) {
                location = newLocation
                googleMap.addMarker(MarkerOptions().position(location!!))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location!!, 14f))

                setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to location))
            }
            else location = null
        }
    }


    companion object {
        // Used as keys to the values of the bundle passed back
        const val REQUEST_KEY_LOCATION = "REQUEST_KEY_LOCATION"
        const val BUNDLE_KEY_LOCATION = "BUNDLE_KEY_LOCATION"
    }


}