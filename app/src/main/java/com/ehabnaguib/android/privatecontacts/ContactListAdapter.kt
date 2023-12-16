package com.ehabnaguib.android.privatecontacts

import android.content.Context
import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.ehabnaguib.android.privatecontacts.databinding.ListItemContactBinding
import com.ehabnaguib.android.privatecontacts.model.Contact
import java.io.File
import java.util.UUID

class ContactHolder(
    private val binding: ListItemContactBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(context: Context, contact: Contact, onContactClicked: (contactId: UUID) -> Unit)  {
        binding.contactName.apply {
            text = contact.name.ifBlank { "No Name" }
        }

        binding.contactNumber.text = contact.number


        if(contact.photo.isNotBlank()) {
            val file = File(context.filesDir, contact.photo)

            if (file.exists()) {
                val bitmap = BitmapFactory.decodeFile(file.absolutePath)
                binding.contactPhoto.setImageBitmap(bitmap)
            }
        }

        binding.root.setOnClickListener {
            onContactClicked(contact.id)
        }
    }
}

class ContactListAdapter(
    private val context: Context,
    private val contacts: List<Contact>,
    private val onContactClicked: (contactId: UUID) -> Unit
) : RecyclerView.Adapter<ContactHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ContactHolder {
        val inflater = LayoutInflater.from(parent.context)
        val binding = ListItemContactBinding.inflate(inflater, parent, false)
        return ContactHolder(binding)
    }

    override fun onBindViewHolder(holder: ContactHolder, position: Int) {
        val contact = contacts[position]
        holder.bind(context, contact, onContactClicked)
    }

    override fun getItemCount() = contacts.size
}