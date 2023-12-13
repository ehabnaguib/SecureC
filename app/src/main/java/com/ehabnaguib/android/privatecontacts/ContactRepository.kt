package com.ehabnaguib.android.privatecontacts

import android.content.Context
import androidx.room.Room
import com.ehabnaguib.android.privatecontacts.database.ContactDatabase
import com.ehabnaguib.android.privatecontacts.database.migration_1_2
import com.ehabnaguib.android.privatecontacts.model.Contact
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import java.util.UUID

private const val DATABASE_NAME ="crime-database"

class ContactRepository private constructor (context : Context) {
    val coroutineScope : CoroutineScope = GlobalScope

    private val contactDatabase : ContactDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ContactDatabase::class.java,
            DATABASE_NAME)
            .addMigrations(migration_1_2)
            .build()


    fun getContacts() : Flow<List<Contact>> = contactDatabase.contactDao().getContacts()

    suspend fun getContact(id : UUID) : Contact = contactDatabase.contactDao().getContact(id)

    suspend fun addContact(contact : Contact) = contactDatabase.contactDao().addContact(contact)

    fun updateContact(contact : Contact) {
        coroutineScope.launch {
            contactDatabase.contactDao().updateContact(contact)}

    }

    fun deleteContact (contact : Contact) {
        coroutineScope.launch {
            contactDatabase.contactDao().deleteContact(contact)
        }
    }

companion object {
    private var INSTANCE: ContactRepository? = null

    fun initialize(context: Context) {
        if (INSTANCE == null) {
            INSTANCE = ContactRepository(context)
        }
    }

    fun get(): ContactRepository {
        return INSTANCE
            ?: throw IllegalStateException("Contact Repository must be initialized")
    }
}

}