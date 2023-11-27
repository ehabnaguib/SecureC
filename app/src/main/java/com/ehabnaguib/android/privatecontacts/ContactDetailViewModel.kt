package com.ehabnaguib.android.privatecontacts

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ContactDetailViewModel (contactId : UUID) : ViewModel() {
    
    private val contactRepository = ContactRepository.get()

    private val _contact: MutableStateFlow<Contact?> = MutableStateFlow(null)
    val contact: StateFlow<Contact?> = _contact.asStateFlow()

    init {
        viewModelScope.launch {
            _contact.value = contactRepository.getContact(contactId)
        }
    }

    fun updateContact(onUpdate: (Contact) -> Contact) {
        _contact.update { oldContact ->
            oldContact?.let { onUpdate(it) }
        }
    }

    override fun onCleared() {
        super.onCleared()
       // contact.value?.let { contactRepository.updateContact(it) }
    }
}

class ContactDetailViewModelFactory(
    private val contactId: UUID
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ContactDetailViewModel(contactId) as T
    }
}