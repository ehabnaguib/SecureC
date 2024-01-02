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

    fun saveContact() {
        contact.value?.let { contact ->
            if (isNewContact){
                if(!isContactBlank(contact)){
                    viewModelScope.launch {
                        contactRepository.addContact(contact)
                    }
                }
            }
            else{
                contactRepository.updateContact(contact)
            }
        }
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

    fun isContactBlank(contact: Contact) :Boolean {
        return (contact.name.isBlank() && contact.number.isBlank())
    }
}



class ContactDetailViewModelFactory(
    private val contactId: UUID?
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactDetailViewModel(contactId) as T
    }
}