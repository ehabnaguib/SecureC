package com.ehabnaguib.android.securec

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.ehabnaguib.android.securec.database.ContactRepository
import com.ehabnaguib.android.securec.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ContactDetailViewModel (contactId : UUID?) : ViewModel() {

    private val contactRepository = ContactRepository.get()

    private val _contact: MutableStateFlow<Contact?> = MutableStateFlow(null)
    val contact: StateFlow<Contact?> = _contact.asStateFlow()

    private lateinit var initialContact : Contact
    private var isNewContact = false

    init {
        viewModelScope.launch {
            if(contactId == null){
                initialContact = Contact(id = UUID.randomUUID(), name = "")
                isNewContact = true
            }
            else
                initialContact = contactRepository.getContact(contactId)

            _contact.value = initialContact
        }
    }


    fun updateContact(onUpdate: (Contact) -> Contact) {
        _contact.update { oldContact ->
            oldContact?.let { onUpdate(it) }
        }
    }

    fun saveContact() : Boolean {
        contact.value?.let { contact ->
            if(isContactChanged()){
                if(isNewContact && !isContactBlank()) {
                    viewModelScope.launch {
                        contactRepository.addContact(contact)
                    }
                    return true
                }
                else {
                    contactRepository.updateContact(contact)
                    return true
                }
            }
            else return false
        }
        return false
    }

    fun deleteContact() {
        contact.value?.let { contactRepository.deleteContact(it)}
    }

    fun getPhotoName() : String {
        var currentPhotoName = ""
        contact.value?.let{
            currentPhotoName = it.photo
            updateContact { oldContact ->
                oldContact.copy(photo = "")
            }
        }
        return currentPhotoName
    }


    fun isContactChanged() : Boolean {
        return contact.value != initialContact
    }

    private fun isContactBlank() :Boolean {
        val contact = contact.value
        return if (contact != null)
            (contact.name.isBlank() && contact.number.isBlank() && contact.photo.isBlank() && contact.location == null)
        else
            true
    }
}



class ContactDetailViewModelFactory(
    private val contactId: UUID?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactDetailViewModel(contactId) as T
    }
}