package com.ehabnaguib.android.privatecontacts


import android.net.Uri
import android.os.Bundle
import android.provider.ContactsContract
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactDetailBinding
import kotlinx.coroutines.launch


class ContactDetailFragment : Fragment() {

    private var _binding: FragmentContactDetailBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
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
/*
        binding.apply {
            contactTitle.doOnTextChanged { text, _, _, _ ->
                contactDetailViewModel.updateContact { oldContact ->
                    oldContact.copy(title = text.toString())
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

 */
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    /*
    private fun updateUi(contact: Contact) {
        binding.apply {
            if (contactTitle.text.toString() != contact.title) {
                contactTitle.setText(contact.title)
            }
        }
    }


    private fun parseContactSelection(contactUri: Uri) {
        val queryFields = arrayOf(ContactsContract.Contacts.DISPLAY_NAME)

        val queryCursor = requireActivity().contentResolver
            .query(contactUri, queryFields, null, null, null)

        queryCursor?.use { cursor ->
            if (cursor.moveToFirst()) {
                val suspect = cursor.getString(0)
                contactDetailViewModel.updateContact { oldContact ->
                    oldContact.copy(suspect = suspect)
                }
            }
        }
    }

     */
}