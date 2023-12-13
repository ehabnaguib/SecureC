package com.ehabnaguib.android.privatecontacts

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ehabnaguib.android.privatecontacts.model.Contact
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContactListViewModel : ViewModel() {
    private val contactRepository = ContactRepository.get()

    private val _contacts: MutableStateFlow<List<Contact>> = MutableStateFlow(emptyList())
    val contacts: StateFlow<List<Contact>>
        get() = _contacts.asStateFlow()

    init {
        viewModelScope.launch {
            contactRepository.getContacts().collect {
                _contacts.value = it
            }
        }
    }

    suspend fun addContact(contact: Contact) {
        contactRepository.addContact(contact)
    }


    fun getNumber(context : Context, position : Int) : String
    {
        val contacts = contacts.value
        return contacts[position].number

    }
}