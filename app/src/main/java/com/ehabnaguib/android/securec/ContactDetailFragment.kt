package com.ehabnaguib.android.securec


import android.Manifest
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.ColorDrawable
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telephony.TelephonyManager
import android.util.Log
import android.view.Gravity
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
import androidx.core.graphics.scale
import androidx.core.view.MenuProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.setFragmentResultListener
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.transition.Fade
import com.ehabnaguib.android.securec.databinding.DialogPhotoOptionsBinding
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

    private var currentContact : Contact? = null

    private var photoName : String? = ""
    private var photoBitmap : Bitmap? = null

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
            getPhotoAndAdjust(requireContext(), uri, photoName!!)
        } else
            Toast.makeText(requireActivity(), "Couldn't get an image", Toast.LENGTH_SHORT).show()
    }

    private val onBackPressedCallback: OnBackPressedCallback = object : OnBackPressedCallback(true) {
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

        exitTransition = Fade()
        enterTransition= Fade()
        reenterTransition = Fade()
        returnTransition = Fade()

        requireActivity().onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        requireActivity().addMenuProvider(createMenuProvider(), viewLifecycleOwner, Lifecycle.State.RESUMED)


        binding.apply {

            // Listening for current contact data updates, and update the UI with it
            viewLifecycleOwner.lifecycleScope.launch {
                viewLifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    contactDetailViewModel.contact.collect { contact ->
                        contact?.let { updateUi(it) }
                        currentContact = contact
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
                if(currentContact?.photo!!.isBlank())
                    updatePhoto()
                else {

                    // Giving the options to update or remove the contact photo if one already exists
                    val photoDialog = Dialog(requireContext())
                    val dialogBinding: DialogPhotoOptionsBinding = DialogPhotoOptionsBinding.inflate(layoutInflater)
                    photoDialog.setContentView(dialogBinding.root)

                    dialogBinding.apply {
                        btnUpdatePhoto.setOnClickListener {
                            updatePhoto()
                            photoDialog.dismiss()
                        }

                        btnRemovePhoto.setOnClickListener {
                            deleteOldPhotoFile(requireContext())
                            contactDetailViewModel.updateContact { oldContact ->
                                oldContact.copy(photo = "")
                            }
                            contactPhoto.setImageResource(R.drawable.image_person_24)
                            photoDialog.dismiss()
                        }

                        btnCancel.setOnClickListener {
                            photoDialog.dismiss()
                        }
                    }

                    val window = photoDialog.window
                    window?.attributes?.gravity = Gravity.TOP

                    // Make a transparent background for the dialog
                    window?.setBackgroundDrawable(ColorDrawable(Color.parseColor("#00000000")))

                    photoDialog.show()
                }
            }
        }

        // Getting the google maps location that the user set in the MapFragment and updating the contact
        setFragmentResultListener(
            MapFragment.REQUEST_KEY_LOCATION
        ) { _, bundle ->
            val newLocation = bundle.getParcelable(MapFragment.BUNDLE_KEY_LOCATION) as LatLng?
            contactDetailViewModel.updateContact { it.copy(location = newLocation) }
            Log.d(TAG, newLocation.toString())
        }

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        super.onDestroy()
        onBackPressedCallback.remove()
    }

    private fun updateUi(contact: Contact) {
        binding.apply {
            if (contactName.text.toString() != contact.name) {
                contactName.setText(contact.name)
            }
            if (contactNumber.text.toString() != contact.number) {
                contactNumber.setText(contact.number)
            }

            if (contact.photo.isBlank()){
                binding.contactPhoto.setImageResource(R.drawable.image_person_24)
            }
            else{
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

            val location = contact.location


            if(location != null) {
                // Setting up the fragment that shows the current set location of the user on google maps
                mapView.visibility = VISIBLE
                setLocation.text = "Edit Google Maps Location"
                val mapFragment = childFragmentManager.findFragmentById(R.id.map_view) as SupportMapFragment?
                mapFragment?.getMapAsync { googleMap ->
                    googleMap.clear()
                    googleMap.addMarker(MarkerOptions().position(location))
                    googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(location, 14f))
                    googleMap.setOnMapClickListener {
                        val uriBegin = "geo:$location"
                        val latitude = location.latitude
                        val longitude = location.longitude
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

    private fun createMenuProvider(): MenuProvider {
        return object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menuInflater.inflate(R.menu.fragment_contact_detail, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                return when (menuItem.itemId) {
                    R.id.delete_button -> {
                        deleteContact()
                        true
                    }
                    R.id.call_button -> {
                        callNumber(currentContact!!.number)
                        true
                    }
                    R.id.whatsapp_button -> {
                        sendWhatsapp()
                        true
                    }
                    else -> false
                }
            }
        }
    }

    private fun saveContact() {
        if (contactDetailViewModel.saveContact()) {
            photoBitmap?.let{
                val file = File(context?.filesDir, photoName!!)
                val outputStream = FileOutputStream(file)
                it.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                outputStream.close()
            }
            Toast.makeText(requireActivity(), "Data Saved", Toast.LENGTH_SHORT).show()
        }
        requireActivity().supportFragmentManager.popBackStack()
    }

    // Opens the whatsapp chat of the contact number after resolving its country code
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
        } ?: Toast.makeText(context, "Invalid phone number.", Toast.LENGTH_SHORT).show()

    }

    private fun callNumber (number : String){
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))

        when {
            ContextCompat.checkSelfPermission(requireContext(),
                Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(callIntent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), Manifest.permission.CALL_PHONE) -> {
                Toast.makeText(requireContext(), "You need to allow call permission from your phone settings.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestCallPermissionLauncher.launch(
                    Manifest.permission.CALL_PHONE)
            }
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

    private fun deleteContact() {
        val builder = AlertDialog.Builder(requireActivity())
        builder.setTitle("Delete Contact")
        builder.setMessage("Are you sure you want to Delete this contact?")
        builder.setPositiveButton("Yes") { _, _ ->
            viewLifecycleOwner.lifecycleScope.launch {
                contactDetailViewModel.deleteContact()
                deleteOldPhotoFile(requireActivity())
                Toast.makeText(requireActivity(), "Contact Deleted.", Toast.LENGTH_SHORT).show()
                requireActivity().supportFragmentManager.popBackStack()
            }
        }
        builder.setNegativeButton("No") { dialog, _ ->
            dialog.dismiss()
        }
        builder.setCancelable(false)
        builder.show()
    }

    private fun updatePhoto() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_MEDIA_IMAGES
                ) == PackageManager.PERMISSION_GRANTED -> {
                    photoPickerLauncher.launch("image/*")
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(), Manifest.permission.READ_MEDIA_IMAGES
                ) -> {
                    Toast.makeText(
                        requireActivity(),
                        "You need to grant permission to access photos",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    requestPhotoPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                }
            }
        } else {
            when {
                ContextCompat.checkSelfPermission(
                    requireActivity(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED -> {
                    photoPickerLauncher.launch("image/*")
                }

                ActivityCompat.shouldShowRequestPermissionRationale(
                    requireActivity(), Manifest.permission.READ_EXTERNAL_STORAGE
                ) -> {
                    Toast.makeText(
                        requireActivity(),
                        "You need to grant permission to access photos",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                else -> {
                    requestPhotoPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                }
            }
        }
    }

    private fun deleteOldPhotoFile(context: Context) {
        val oldPhotoName: String = contactDetailViewModel.getPhotoName()
        if (oldPhotoName.isNotBlank()) {
            val file = File(context.filesDir, oldPhotoName)
            if (file.exists())
                file.delete()
        }
    }

    private fun getPhotoAndAdjust(context: Context, imageUri: Uri, photoName: String) {

        photoBitmap = fixPhotoSizeAndRotation(context, imageUri)
        binding.contactPhoto.setImageBitmap(photoBitmap)

        contactDetailViewModel.updateContact { oldContact ->
            oldContact.copy(photo = photoName)

        }

    }

    private fun fixPhotoSizeAndRotation(context: Context, imageUri: Uri) : Bitmap?{
        // Some photos appear rotated 90 or 180 degrees and exif is needed to compensate
        // inputStream1 is to get the exif of the photo to determine if it needs rotation
        val inputStream1 = context.contentResolver.openInputStream(imageUri)
        if (inputStream1 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return null
        }
        val exifInterface = ExifInterface(inputStream1)
        inputStream1.close()

        val orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        // inputStream2 is to get the actual bitmap
        val inputStream2 = context.contentResolver.openInputStream(imageUri)
        if (inputStream2 == null){
            Toast.makeText(requireActivity(), "couldn't find stream", Toast.LENGTH_SHORT).show()
            return null
        }

        val originalBitmap = BitmapFactory.decodeStream(inputStream2)
        inputStream2.close()

        // Downsizing the bitmap to not have no more than 400 pixels in height preserving ratio
        // Keeps dividing by 2 until an appropriate size
        val srcWidth = originalBitmap.width
        val srcHeight = originalBitmap.height
        var desiredHeight = srcHeight
        var divisions = 1
        while (desiredHeight > 400){
            desiredHeight /= 2
            divisions++
        }

        val scaledBitmap = originalBitmap.scale(srcWidth/divisions, srcHeight/divisions)

        // Fix rotation issue if exists using the exif
        val photoBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(scaledBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(scaledBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(scaledBitmap, 270f)
            else -> scaledBitmap
        }

        return photoBitmap
    }

    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            matrix, true)
    }

}