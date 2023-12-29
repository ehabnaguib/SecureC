package com.ehabnaguib.android.privatecontacts.database

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import java.util.Date

class ContactTypeConverters {
    @TypeConverter
    fun fromLatLng(location: LatLng?): String? {
        return if (location != null)
            "${location.latitude},${location.longitude}"
        else null
    }

    @TypeConverter
    fun toLatLng(locationString : String?): LatLng? {
        return if (locationString != null){
            val pieces = locationString.split(",")
            LatLng(pieces[0].toDouble(), pieces[1].toDouble())
        } else null
    }
}