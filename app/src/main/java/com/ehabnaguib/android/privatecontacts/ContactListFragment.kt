package com.ehabnaguib.android.privatecontacts

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactListBinding
import kotlinx.coroutines.launch
import java.util.UUID


class ContactListFragment : Fragment() {


    private var _binding : FragmentContactListBinding? = null
    private val binding : FragmentContactListBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val contactListViewModel: ContactListViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentContactListBinding.inflate(inflater, container, false)
        binding.contactRecyclerView.layoutManager = LinearLayoutManager(context)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_contact_list, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.new_contact -> {
                val newContact : Contact = Contact(id = UUID.randomUUID(), name = "")
                viewLifecycleOwner.lifecycleScope.launch {
                    contactListViewModel.addContact(newContact)
                }
                findNavController().navigate(
                    ContactListFragmentDirections.openContactDetail(newContact.id)
                )
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}