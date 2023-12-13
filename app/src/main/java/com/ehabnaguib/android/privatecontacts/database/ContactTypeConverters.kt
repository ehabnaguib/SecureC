package com.ehabnaguib.android.privatecontacts.database

import androidx.room.TypeConverter
import com.google.android.gms.maps.model.LatLng
import java.util.Date

class ContactTypeConverters {
    @TypeConverter
    fun fromLatLng(latLng: LatLng?): String? {
        if (latLng != null)
            return "${latLng.latitude},${latLng.longitude}"
        else return null
    }

    @TypeConverter
    fun toLatLng(locationString : String?): LatLng? {
        if (locationString != null){
            val pieces = locationString.split(",")
            return LatLng(pieces[0].toDouble(), pieces[1].toDouble())
        }
        else return null
    }
}