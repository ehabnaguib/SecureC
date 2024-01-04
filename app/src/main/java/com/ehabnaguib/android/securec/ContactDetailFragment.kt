package com.ehabnaguib.android.securec


import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
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
import android.view.WindowManager
import android.widget.Button
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
import com.ehabnaguib.android.securec.databinding.FragmentContactDetailBinding
import com.ehabnaguib.android.securec.model.Contact
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

    private var myContact : Contact? = null

    private var photoName : String? = ""
    private var location : LatLng? = null

    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "You can now make phone calls.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(),
                "You need to allow call permission from your phone settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPhotoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted)
            photoPickerLauncher.launch("image/*")
        else
            Toast.makeText(requireActivity(),
                "You need to allow permission to access files from your phone settings.", Toast.LENGTH_SHORT).show()
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoName = "IMG_${Date()}.JPG"
            deleteOldPhotoFile(requireContext())
            binding.contactPhoto.foreground = null
            adjustAndSavePhoto(requireContext(), uri, photoName!!)
        } else
            Toast.makeText(requireActivity(), "Cound't get image", Toast.LENGTH_SHORT).show()
    }

    private val callback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            if (contactDetailViewModel.isContactChanged()) {
                val builder = AlertDialog.Builder(requireActivity())
                builder.setTitle("Save Changes")
                builder.setMessage("Do you want to save the changes?")
                builder.setPositiveButton("Yes") { _, _ ->
                    saveContact()
                }
                builder.setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                    requireActivity().supportFragmentManager.popBackStack()
                }
                builder.setNeutralButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
                builder.setCancelable(false)
                builder.show()

            }
            else
                requireActivity().supportFragmentManager.popBackStack()
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)

        exitTransition = Fade()
        enterTransition= Fade()
        reenterTransition = Fade()
        returnTransition = Fade()

        // Note that you need to remove the callback when the Fragment is destroyed
        requireActivity().onBackPressedDispatcher.addCallback(this, callback)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_contact_detail, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.delete_button -> {
                deleteContact()

                return true
            }
            R.id.call_button -> {
                callNumber(myContact!!.number)
                return true
            }
            R.id.whatsapp_button -> {
                sendWhatsapp()
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

            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    contactDetailViewModel.contact.collect { contact ->
                        contact?.let { updateUi(it) }
                        myContact = contact
                    }
                }
            }

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

            saveButton.setOnClickListener {
                saveContact()
            }




            contactPhoto.setOnClickListener {

                val builder = AlertDialog.Builder(requireContext())

                builder.setPositiveButton("Update Photo") { dialog, which ->
                    // Handle the positive button action here
                }

                builder.setNegativeButton("Remove Photo") { dialog, which ->
                    // Handle the negative button action here
                }

                val dialog = builder.create()

                dialog.window?.apply {
                    // Set the width of the dialog window to wrap its content
                    val params: WindowManager.LayoutParams = attributes
                    params.width = WindowManager.LayoutParams.WRAP_CONTENT
                    attributes = params

                    // Set the background to transparent (if required)

                }
                dialog.show()



//                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//                    when {
//                        ContextCompat.checkSelfPermission(
//                            requireActivity(),
//                            android.Manifest.permission.READ_MEDIA_IMAGES
//                        ) == PackageManager.PERMISSION_GRANTED -> {
//                            photoPickerLauncher.launch("image/*")
//                        }
//
//                        ActivityCompat.shouldShowRequestPermissionRationale(
//                            requireActivity(), android.Manifest.permission.READ_MEDIA_IMAGES
//                        ) -> {
//                            Toast.makeText(
//                                requireActivity(),
//                                "You just denied permission.",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//
//                        else -> {
//                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
//                        }
//                    }
//                } else {
//                    when {
//                        ContextCompat.checkSelfPermission(
//                            requireActivity(),
//                            android.Manifest.permission.READ_EXTERNAL_STORAGE
//                        ) == PackageManager.PERMISSION_GRANTED -> {
//                            photoPickerLauncher.launch("image/*")
//                        }
//
//                        ActivityCompat.shouldShowRequestPermissionRationale(
//                            requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE
//                        ) -> {
//                            Toast.makeText(
//                                requireActivity(),
//                                "You just denied permission.",
//                                Toast.LENGTH_SHORT
//                            ).show()
//                        }
//
//                        else -> {
//                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
//                        }
//                    }
//                }
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

    private fun saveContact() {
        if (contactDetailViewModel.isContactChanged()) {
            contactDetailViewModel.saveContact()
            Toast.makeText(requireActivity(), "Data Saved", Toast.LENGTH_SHORT).show()
        }
        requireActivity().supportFragmentManager.popBackStack()
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

            if (contactNotes.text.toString() != contact.notes){
                contactNotes.setText(contact.notes)
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
                            `package` = "com.google.android.apps.maps"
                        }
                        startActivity(mapIntent)
                    }
                }
            }
            else {
                mapView.visibility = GONE
                setLocation.text = "Set google maps location"
            }
        }
    }
    private fun callNumber (number : String){
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))

        when {
            ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(callIntent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), android.Manifest.permission.CALL_PHONE) -> {
                Toast.makeText(requireContext(), "You need to allow call permission from your phone settings.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestCallPermissionLauncher.launch(
                    android.Manifest.permission.CALL_PHONE)
            }
        }
    }

    private fun deleteContact() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Delete Contact")
        builder.setMessage("Are you sure you want to Delete this contact?")
        builder.setPositiveButton("Yes") { dialog, which ->
            viewLifecycleOwner.lifecycleScope.launch {
                contactDetailViewModel.deleteContact()
                deleteOldPhotoFile(requireActivity())
                Toast.makeText(requireActivity(), "Deleted", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        builder.setNegativeButton("No") { dialog, which ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun sendWhatsapp () {
        val context = requireActivity()
        val number = binding.contactNumber.text.toString()
        val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val defaultCountryCode = telephonyManager.simCountryIso.uppercase(Locale.US)
        val formattedNumber = formatPhoneNumberWithDefaultCountryCode(number, defaultCountryCode)

        formattedNumber?.let {
            val uri = Uri.parse("https://wa.me/$it")
            val intent = Intent(Intent.ACTION_VIEW, uri)

            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Something went wrong. Make sure WhatsApp is installed.", Toast.LENGTH_SHORT).show()
            }
        } ?: run {
            Toast.makeText(context, "Invalid phone number.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun formatPhoneNumberWithDefaultCountryCode(phoneNumber: String, defaultCountryCode: String): String? {
        val phoneUtil = PhoneNumberUtil.getInstance()
        return try {
            // Parse the phone number
            val numberProto: Phonenumber.PhoneNumber = phoneUtil.parse(phoneNumber, defaultCountryCode)

            // Format the phone number to E.164 format (international format)
            phoneUtil.format(numberProto, PhoneNumberUtil.PhoneNumberFormat.E164).removePrefix("+")
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }



    private fun adjustAndSavePhoto(context: Context, imageUri: Uri, photoName: String) {

        val fixedBitmap = fixPhotoRotation(context, imageUri)

        if (fixedBitmap != null){
            val file = File(context.filesDir, photoName)
            val outputStream = FileOutputStream(file)
            fixedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
            outputStream.close()

            contactDetailViewModel.updateContact { oldContact ->
                oldContact.copy(photo = photoName)
            }
        }
        else
            Toast.makeText(context, "Something went wrong.", Toast.LENGTH_SHORT).show()

    }

    private fun fixPhotoRotation(context: Context, imageUri: Uri) : Bitmap?{
        val inputStream1 = context.contentResolver.openInputStream(imageUri)

        if (inputStream1 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return null
        }
        val exifInterface = ExifInterface(inputStream1)
        inputStream1.close()

        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val inputStream2 = context.contentResolver.openInputStream(imageUri)
        if (inputStream2 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return null
        }
        val originalBitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2.close()

        val fixedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(originalBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(originalBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(originalBitmap, 270f)
            else -> originalBitmap
        }

        return fixedBitmap
    }

    private fun deleteOldPhotoFile(context: Context) {
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
        callback.remove()
    }
}