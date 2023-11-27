package com.ehabnaguib.android.privatecontacts

import android.app.Application

class PrivateContactsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ContactRepository.initialize(this)
    }
}