package com.ehabnaguib.android.privatecontacts.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.ehabnaguib.android.privatecontacts.model.Contact

@Database(entities = [Contact::class], version = 2)
@TypeConverters(ContactTypeConverters::class)
abstract class ContactDatabase : RoomDatabase() {
    abstract fun contactDao() : ContactDao
}



val migration_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "ALTER TABLE Contact ADD COLUMN location TEXT DEFAULT NULL"
        )
    }
}