package com.ehabnaguib.android.privatecontacts

import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.ehabnaguib.android.privatecontacts.database.ContactTypeConverters
import com.ehabnaguib.android.privatecontacts.databinding.FragmentMapBinding
import com.google.android.gms.common.api.Status
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.AutocompletePrediction
import com.google.android.libraries.places.api.model.AutocompleteSessionToken
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.model.TypeFilter
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.AutocompleteSupportFragment
import com.google.android.libraries.places.widget.listener.PlaceSelectionListener


private const val TAG = "MapFragment"

class MapFragment : Fragment() {

    private var _binding : FragmentMapBinding? = null
    private val binding : FragmentMapBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val args: MapFragmentArgs by navArgs()

    private var location : LatLng? = null

    private lateinit var placesClient: PlacesClient
    private lateinit var predictionsAdapter: ArrayAdapter<AutocompletePrediction>


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentMapBinding.inflate(inflater, container, false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        val autocompleteFragment = childFragmentManager.findFragmentById(R.id.search_view_fragment)
                as AutocompleteSupportFragment

        location = args.location as LatLng?

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

        // Initialize the AutocompleteSupportFragment


        // Specify the types of place data to return
        autocompleteFragment.setPlaceFields(listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG))

        // Set up a PlaceSelectionListener to handle the response
        autocompleteFragment.setOnPlaceSelectedListener(object : PlaceSelectionListener {
            override fun onPlaceSelected(place: Place) {
                updateMap(mapFragment, place.latLng)
            }

            override fun onError(status: Status) {
                // TODO: Handle the error
            }
        })
    }

    private fun updateMap(mapFragment: SupportMapFragment?, newLocation : LatLng?) {
        mapFragment?.getMapAsync { googleMap ->
            if (newLocation != null) {
                location = newLocation
                googleMap.addMarker(MarkerOptions().position(location!!))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location!!, 14f))

                setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to location))
            }
        }
    }


    companion object {
        const val REQUEST_KEY_LOCATION = "REQUEST_KEY_LOCATION"
        const val BUNDLE_KEY_LOCATION = "BUNDLE_KEY_LOCATION"
    }


}