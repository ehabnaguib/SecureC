package com.ehabnaguib.android.securec

import androidx.lifecycle.ViewModel

class MainActivityViewModel : ViewModel() {

    // The variable in a view model to survive configuration changes
    // EX: if the user rotates the screen, he doesn't have to unlock again
    var shouldAuthenticate = true

}