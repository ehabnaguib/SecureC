package com.ehabnaguib.android.securec

import android.app.Application
import com.ehabnaguib.android.securec.database.ContactRepository

class PrivateContactsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ContactRepository.initialize(this)
    }
}