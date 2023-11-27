package com.ehabnaguib.android.privatecontacts

import android.content.Context
import androidx.room.Room
import com.ehabnaguib.android.privatecontacts.database.ContactDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import java.util.UUID

private const val DATABASE_NAME ="crime-database"

class ContactRepository private constructor (context : Context) {
    val coroutineScope : CoroutineScope = GlobalScope

    private val contactDatabase : ContactDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            ContactDatabase::class.java,
            DATABASE_NAME)
            .build()


    fun getContacts() : Flow<List<Contact>> = contactDatabase.contactDao().getContacts()

    suspend fun getContact(id : UUID) : Contact = contactDatabase.contactDao().getContact(id)

    suspend fun addContact(contact : Contact) = contactDatabase.contactDao().addContact(contact)

    suspend fun updateContact(contact : Contact) = contactDatabase.contactDao().updateContact(contact)

    suspend fun deleteContact (contact : Contact) = contactDatabase.contactDao().deleteContact(contact)

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