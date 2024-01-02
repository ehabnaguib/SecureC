package com.ehabnaguib.android.securec.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ehabnaguib.android.securec.model.Contact
import kotlinx.coroutines.flow.Flow
import java.util.UUID


@Dao
interface ContactDao {

    @Query("SELECT * FROM contact")
    fun getContacts() : Flow<List<Contact>>

    @Query("SELECT * FROM contact WHERE id=(:id)")
    suspend fun getContact(id : UUID) : Contact

    @Insert
    suspend fun addContact(contact : Contact)

    @Update
    suspend fun updateContact(contact : Contact)

    @Delete
    suspend fun deleteContact(contact : Contact)
}