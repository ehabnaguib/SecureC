package com.ehabnaguib.android.privatecontacts

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import com.ehabnaguib.android.privatecontacts.databinding.ListItemContactBinding
import java.util.UUID

class ContactHolder(
    private val binding: ListItemContactBinding
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(contact: Contact, onContactClicked: (contactId: UUID) -> Unit) {
        binding.contactName.apply {
            text = contact.name.ifBlank { "No Name" }
        }

        binding.contactNumber.text = contact.number

        binding.root.setOnClickListener {
            onContactClicked(contact.id)
        }
    }
}

class ContactListAdapter(
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
        holder.bind(contact, onContactClicked)
    }

    override fun getItemCount() = contacts.size
}