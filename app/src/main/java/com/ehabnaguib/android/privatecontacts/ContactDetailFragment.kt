package com.ehabnaguib.android.privatecontacts


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Fade
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactDetailBinding
import com.ehabnaguib.android.privatecontacts.model.Contact
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.i18n.phonenumbers.PhoneNumberUtil
import com.google.i18n.phonenumbers.Phonenumber
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.Locale
import java.util.UUID

private const val TAG = "ContactDetailFragment"

class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private val args: ContactDetailFragmentArgs by navArgs()

    private val contactDetailViewModel: ContactDetailViewModel by viewModels {
        ContactDetailViewModelFactory(args.contactId)
    }
    private var photoName : String? = ""

    private var location : LatLng? = null

    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted)
            call()
        else
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
    }

    private val requestPhotoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted)
            photoPickerLauncher.launch("image/*")
        else
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoName = "IMG_${Date()}.JPG"
            binding.contactPhoto.foreground = null
            downscaleImageAndSave(requireActivity(), uri, photoName!!)
        } else
            Toast.makeText(requireActivity(), "Cound't get image", Toast.LENGTH_SHORT).show()
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        exitTransition = Fade()
        enterTransition= Fade()
        reenterTransition = Fade()
        returnTransition = Fade()

        val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (contactDetailViewModel.isContactChanged()) {
                    val builder = AlertDialog.Builder(requireActivity())
                    builder.setTitle("Save Changes")
                    builder.setMessage("Do you want to save the changes?")
                    builder.setPositiveButton("Yes") { dialog, which ->
                        save()
                    }
                    builder.setNegativeButton("No") { dialog, which ->
                        dialog.dismiss()
                        // Handle the "No" case by closing the activity or whatever is appropriate for your app
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                    builder.setNeutralButton("Cancel") { dialog, which ->

                    }
                    builder.setCancelable(false)
                    builder.show()
                    // Handle the back press here
                    // e.g., pop the fragment from the stack, close an opened menu, etc.
                    // If you want to propagate the back press event to the hosting activity, call remove()
                    if (shouldInterceptBackPress()) {
                        // Intercepted the back press
                    } else {
                        // If you want the default back press behavior to occur (e.g., back navigation),
                        // then call this:
                        isEnabled = false
                        requireActivity().onBackPressed()
                    }
                }
                else if (contactDetailViewModel.isContactBlank())
                    save()
                else
                    requireActivity().supportFragmentManager.popBackStack()
            }
        }

        // Note that you need to remove the callback when the Fragment is destroyed
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    private fun shouldInterceptBackPress(): Boolean {
        // Your logic to determine if the back press should be intercepted
        // Return true to intercept, false to continue with the regular back press handling
        return true // or false based on your condition
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_contact_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_button -> {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle("Delete Contact")
                builder.setMessage("Are you sure you want to Delete this contact?")
                builder.setPositiveButton("Yes") { dialog, which ->
                    viewLifecycleOwner.lifecycleScope.launch {
                        contactDetailViewModel.deleteContact()
                        deletePhotoFile(requireActivity())
                        Toast.makeText(requireActivity(), "Deleted", Toast.LENGTH_SHORT).show()
                        requireActivity().supportFragmentManager.popBackStack()
                    }
                }
                builder.setNegativeButton("No") { dialog, which ->
                    dialog.dismiss()
                }
                builder.setCancelable(false)
                builder.show()

                return true
            }
            R.id.call_button -> {
                when {
                    ContextCompat.checkSelfPermission(
                        requireActivity(),
                        android.Manifest.permission.CALL_PHONE
                    ) == PackageManager.PERMISSION_GRANTED -> {
                        call()
                    }

                    ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(), android.Manifest.permission.CALL_PHONE
                    ) -> {
                        Toast.makeText(
                            requireActivity(),
                            "You just denied permission.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    else -> {
                        requestCallPermissionLauncher.launch(
                            android.Manifest.permission.CALL_PHONE
                        )
                    }
                }
                return true
            }
            R.id.whatsapp_button -> {
                whatsapp()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContactDetailBinding.inflate(inflater, container, false)
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

            contactNotes.doOnTextChanged { text, _, _, _ ->
                contactDetailViewModel.updateContact { oldContact ->
                    oldContact.copy(notes = text.toString())
                }
            }

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    contactDetailViewModel.contact.collect { contact ->
                        contact?.let { updateUi(it) }
                    }
                }
            }

            saveButton.setOnClickListener {
                save()
            }




            contactPhoto.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            photoPickerLauncher.launch("image/*")
                        }

                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(), android.Manifest.permission.READ_MEDIA_IMAGES
                        ) -> {
                            Toast.makeText(
                                requireActivity(),
                                "You just denied permission.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    }
                } else {
                    when {
                        ContextCompat.checkSelfPermission(
                            requireActivity(),
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            photoPickerLauncher.launch("image/*")
                        }

                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) -> {
                            Toast.makeText(
                                requireActivity(),
                                "You just denied permission.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }

                        else -> {
                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
                    }
                }
            }
        }

        setFragmentResultListener(
            MapFragment.REQUEST_KEY_LOCATION
        ) { _, bundle ->
            val newLocation =
                bundle.getParcelable(MapFragment.BUNDLE_KEY_LOCATION) as LatLng?
            contactDetailViewModel.updateContact { it.copy(location = newLocation) }
            Log.d(TAG, newLocation.toString())
        }

    }

    private fun save() {
        contactDetailViewModel.saveContact()
        requireActivity().supportFragmentManager.popBackStack()
        if (contactDetailViewModel.isContactChanged())
            Toast.makeText(requireActivity(), "Data Saved", Toast.LENGTH_SHORT).show()
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
            if (contact.photo.isNotBlank()){
                binding.contactPhoto.foreground = null
                val file = File(requireActivity().filesDir, contact.photo)

                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                    contactPhoto.setImageBitmap(bitmap)
                }
            }

            if (contact.notes.isNotBlank()){
                binding.contactNotes.setText(contact.notes)
            }

            setLocation.setOnClickListener {
                findNavController().navigate(
                    ContactDetailFragmentDirections.setLocation(contactDetailViewModel.contact.value?.location)
                )
            }

            location = contact.location

            if(location != null) {
                mapView.visibility = VISIBLE
                setLocation.text = "Edit Google Maps Location"
                //fillerView.visibility = GONE
                val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
                mapFragment?.getMapAsync { googleMap ->
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(location!!))
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location!!, 14f))
                    googleMap.setOnMapClickListener {
                        val uriBegin = "geo:$location"
                        val latitude = location!!.latitude
                        val longitude = location!!.longitude
                        val label = contact.name
                        val query = "$latitude,$longitude($label)"
                        val encodedQuery = Uri.encode(query)
                        val uriString = "$uriBegin?q=$encodedQuery&z=16"
                        val uri = Uri.parse(uriString)

                        val mapIntent = Intent(Intent.ACTION_VIEW, uri).apply {
                            // Ensure the Google Maps app handles the intent
                            `package` = "com.google.android.apps.maps"
                        }
                        startActivity(mapIntent)
                    }
                }
            }
            else {
                mapView.visibility = GONE
                //fillerView.visibility = VISIBLE
                setLocation.text = "Set google maps location"
            }

        }
    }
    private fun call(){
        val number = binding.contactNumber.text.toString()
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        startActivity(callIntent)
    }

    private fun whatsapp () {
        val context = requireActivity()
        val number = binding.contactNumber.text.toString()
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val defaultCountryCode = telephonyManager.simCountryIso.toUpperCase(Locale.US)
        val formattedNumber = formatPhoneNumberWithDefaultCountryCode(number, defaultCountryCode)

        formattedNumber?.let {
            val uri = Uri.parse("https://wa.me/$it")
            val intent = Intent(Intent.ACTION_VIEW, uri)

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "WhatsApp is not installed on your device.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Invalid phone number.", Toast.LENGTH_SHORT).show()
        }
    }


    fun formatPhoneNumberWithDefaultCountryCode(phoneNumber: String, defaultCountryCode: String): String? {
        val phoneUtil = PhoneNumberUtil.getInstance()
        try {
            // Parse the phone number
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, defaultCountryCode)

            // Format the phone number to E.164 format (international format)
            return phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164).removePrefix("+")
        } catch (e: Exception) {
            e.printStackTrace()
            return null // Return null or handle the error as you prefer
        }
    }



    private fun downscaleImageAndSave(context: Context, imageUri: Uri, photoName: String) {

        val inputStream1 = context.contentResolver.openInputStream(imageUri)

        if (inputStream1 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return
        }
        val ei = ExifInterface(inputStream1)
        inputStream1.close()

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        Log.d("ContactDetailFragment", orientation.toString())

        val inputStream2 = context.contentResolver.openInputStream(imageUri)
        if (inputStream2 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return
        }
        val originalBitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2.close()

        val editedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(originalBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(originalBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(originalBitmap, 270f)
            else -> originalBitmap
        }

        deletePhotoFile(context)

        val file = File(context.filesDir, photoName)
        val outputStream = FileOutputStream(file)

        editedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        contactDetailViewModel.updateContact { oldContact ->
            oldContact.copy(photo = photoName)
        }

        outputStream.close()
    }

    private fun deletePhotoFile(context: Context) {
        val oldPhotoName: String = contactDetailViewModel.getPhotoName()
        if (oldPhotoName.isNotBlank()) {
            val file = File(context.filesDir, oldPhotoName)
            if (file.exists())
                file.delete()
        }
    }


    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()

    }

}