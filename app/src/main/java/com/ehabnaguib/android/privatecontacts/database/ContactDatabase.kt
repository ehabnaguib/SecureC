package com.ehabnaguib.android.privatecontacts.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.ehabnaguib.android.privatecontacts.Contact

@Database(entities = [Contact::class], version = 1)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao() : ContactDao
}