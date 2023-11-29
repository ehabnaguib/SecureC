package com.ehabnaguib.android.privatecontacts


import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactDetailBinding
import kotlinx.coroutines.launch

private const val CALL_PERMISSION_REQUEST_CODE = 1

val dial = "tel:010123"
val callIntent = Intent(Intent.ACTION_CALL, Uri.parse(dial))



class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            startActivity(callIntent)
        } else {
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private val args: ContactDetailFragmentArgs by navArgs()

    private val contactDetailViewModel: ContactDetailViewModel by viewModels {
        ContactDetailViewModelFactory(args.contactId)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding =
            FragmentContactDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.apply {
            contactName.doOnTextChanged { text, _, _, _ ->
                contactDetailViewModel.updateContact { oldContact ->
                    oldContact.copy(name = text.toString())
                }
            }

            contactNumber.doOnTextChanged { text, _, _, _ ->
                contactDetailViewModel.updateContact { oldContact ->
                    oldContact.copy(number = text.toString())
                }
            }

            saveButton.setOnClickListener{
                contactDetailViewModel.saveContact()
                Toast.makeText(requireActivity(), "Data Saved", Toast.LENGTH_SHORT).show()
            }

            deleteButton.setOnClickListener {
                viewLifecycleOwner.lifecycleScope.launch {
                    contactDetailViewModel.deleteContact()
                    requireActivity().supportFragmentManager.popBackStack()
                }
            }

            callButton.setOnClickListener {
                when {
                    ContextCompat.checkSelfPermission(requireActivity(),
                        android.Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        startActivity(callIntent)
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(), android.Manifest.permission.CALL_PHONE) -> {
                        Toast.makeText(requireActivity(), "You just denied permission.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        requestPermissionLauncher.launch(
                            android.Manifest.permission.CALL_PHONE)
                    }
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    contactDetailViewModel.contact.collect { contact ->
                        contact?.let { updateUi(it) }
                    }
                }
            }
        }
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun updateUi(contact: Contact) {
        binding.apply {
            if (contactName.text.toString() != contact.name) {
                contactName.setText(contact.name)
            }
            if (contactNumber.text.toString() != contact.number) {
                contactNumber.setText(contact.number)
            }
        }
    }
}