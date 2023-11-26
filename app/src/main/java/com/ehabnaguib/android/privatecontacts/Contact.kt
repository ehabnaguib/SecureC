package com.ehabnaguib.android.privatecontacts

import java.util.UUID

data class Contact(
    val id : UUID,
    val name : String,
    val number : String = "",
    val description : String = "",
    val photo : String = "",
)
