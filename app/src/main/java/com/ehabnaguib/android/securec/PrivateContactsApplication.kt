package com.ehabnaguib.android.securec

import android.app.Application
import com.ehabnaguib.android.securec.database.ContactRepository

class PrivateContactsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Makes a repository singleton to be used through the life of the app
        ContactRepository.initialize(this)
    }
}