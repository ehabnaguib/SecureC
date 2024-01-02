package com.ehabnaguib.android.securec.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.ehabnaguib.android.securec.model.Contact

@Database(entities = [Contact::class], version = 1)
@TypeConverters(ContactTypeConverters::class)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao() : ContactDao
}