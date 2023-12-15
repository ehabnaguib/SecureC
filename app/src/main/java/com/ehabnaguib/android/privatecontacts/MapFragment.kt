package com.ehabnaguib.android.privatecontacts

import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.fragment.app.setFragmentResult
import androidx.navigation.fragment.navArgs
import com.ehabnaguib.android.privatecontacts.database.ContactTypeConverters
import com.ehabnaguib.android.privatecontacts.databinding.FragmentMapBinding
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions


private const val TAG = "MapFragment"

class MapFragment : Fragment() {

    private var _binding : FragmentMapBinding? = null
    private val binding : FragmentMapBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val args: MapFragmentArgs by navArgs()

    private var location : LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }


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

        location = args.location as LatLng?
        setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to location))
        mapFragment?.getMapAsync { googleMap ->
            if (location != null){
                googleMap.addMarker(MarkerOptions().position(location!!).title("Selected Location"))
                googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location!!, 10f))
            }

            googleMap.setOnMapClickListener { latLng ->
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(latLng).title("Selected Location"))
                location = latLng
                setFragmentResult(REQUEST_KEY_LOCATION, bundleOf(BUNDLE_KEY_LOCATION to location))
            }
        }





    }
    companion object {
        const val REQUEST_KEY_LOCATION = "REQUEST_KEY_LOCATION"
        const val BUNDLE_KEY_LOCATION = "BUNDLE_KEY_LOCATION"
    }


}