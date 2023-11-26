package com.ehabnaguib.android.privatecontacts

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity
data class Contact(
    @PrimaryKey val id : UUID,
    val name : String,
    val number : String = "",
    val description : String = "",
    val photo : String = ""
)
