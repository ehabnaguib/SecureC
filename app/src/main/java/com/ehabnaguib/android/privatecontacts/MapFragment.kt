package com.ehabnaguib.android.privatecontacts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.ehabnaguib.android.privatecontacts.databinding.FragmentMapBinding


class MapFragment : Fragment() {

    private var _binding : FragmentMapBinding? = null
    private val binding : FragmentMapBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

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


}