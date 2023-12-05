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
import android.provider.MediaStore
import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.navArgs
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactDetailBinding
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import kotlin.math.roundToInt

private const val CALL_PERMISSION_REQUEST_CODE = 1

class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private var photoName : String? = ""

    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            call()
        } else {
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private val requestPhotoPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            photoPickerLauncher.launch("image/*")
        } else {
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
        }
    }

    private val photoPickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            photoName = "IMG_${Date()}.JPG"
            binding.contactPhoto.foreground = null
            downscaleImageAndSave(requireActivity(), uri, photoName!!)
        } else {
            Toast.makeText(requireActivity(), "Cound't get image", Toast.LENGTH_SHORT).show()
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
                        call()
                    }
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        requireActivity(), android.Manifest.permission.CALL_PHONE) -> {
                        Toast.makeText(requireActivity(), "You just denied permission.", Toast.LENGTH_SHORT).show()
                    }
                    else -> {
                        requestCallPermissionLauncher.launch(
                            android.Manifest.permission.CALL_PHONE)
                    }
                }
            }


            contactPhoto.setOnClickListener {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    when {
                        ContextCompat.checkSelfPermission(requireActivity(),
                            android.Manifest.permission.READ_MEDIA_IMAGES
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            photoPickerLauncher.launch("image/*")
                        }
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(), android.Manifest.permission.READ_MEDIA_IMAGES) -> {
                            Toast.makeText(requireActivity(), "You just denied permission.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_MEDIA_IMAGES)
                        }
                    }
                }
                else {
                    when {
                        ContextCompat.checkSelfPermission(requireActivity(),
                            android.Manifest.permission.READ_EXTERNAL_STORAGE
                        ) == PackageManager.PERMISSION_GRANTED -> {
                            photoPickerLauncher.launch("image/*")
                        }
                        ActivityCompat.shouldShowRequestPermissionRationale(
                            requireActivity(), android.Manifest.permission.READ_EXTERNAL_STORAGE) -> {
                            Toast.makeText(requireActivity(), "You just denied permission.", Toast.LENGTH_SHORT).show()
                        }
                        else -> {
                            requestPhotoPermissionLauncher.launch(android.Manifest.permission.READ_EXTERNAL_STORAGE)
                        }
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
            if (contact.photo.isNotBlank()){
                binding.contactPhoto.foreground = null
                val file = File(requireActivity().filesDir, contact.photo)

                if (file.exists()) {
                    val bitmap = BitmapFactory.decodeFile(file.absolutePath)

                    contactPhoto.setImageBitmap(bitmap)
                }
            }
        }
    }
    private fun call(){
        val number = binding.contactNumber.text.toString()
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))
        startActivity(callIntent)
    }

    fun storePhoto(){
        photoName = "IMG_${Date()}.JPG"
        val photoFile = File(
            requireContext().applicationContext.filesDir,
            photoName
        )
        val photoUri = FileProvider.getUriForFile(
            requireContext(),
            "com.bignerdranch.android.criminalintent.fileprovider",
            photoFile
        )
    }

    fun downscaleImageAndSave(context: Context, imageUri: Uri, photoName: String) {

        val inputStream = context.contentResolver.openInputStream(imageUri)
        val originalBitmap = BitmapFactory.decodeStream(inputStream)

        val ei = ExifInterface(inputStream!!)

        val orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

        val editedBitmap = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(originalBitmap, 90f)
            ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(originalBitmap, 180f)
            ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(originalBitmap, 270f)
            else -> originalBitmap
        }

        val file = File(context.filesDir, photoName)
        val outputStream = FileOutputStream(file)

        editedBitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)

        contactDetailViewModel.updateContact { oldContact ->
            oldContact.copy(photo = photoName)
        }

        outputStream.close()
    }

/*
        val downscaledBitmap = Bitmap.createScaledBitmap(
            originalBitmap,
            convertDpToPx(context, 400),
            (originalBitmap.height * (convertDpToPx(context, 400).toFloat() / originalBitmap.width)).toInt(),
            true
        )

 */



    fun convertDpToPx(context: Context, dp: Int): Int {
        return (dp * (context.resources.displayMetrics.densityDpi.toFloat() / DisplayMetrics.DENSITY_DEFAULT)).roundToInt()
    }

    fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(angle)
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height,
            matrix, true)
    }

}