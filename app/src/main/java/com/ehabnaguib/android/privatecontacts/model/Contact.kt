package com.ehabnaguib.android.privatecontacts.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.android.gms.maps.model.LatLng
import java.util.UUID

@Entity
data class Contact(
    @PrimaryKey val id : UUID,
    val name : String,
    val number : String = "",
    val description : String = "",
    val photo : String = "",
    val location : LatLng? = null
)
