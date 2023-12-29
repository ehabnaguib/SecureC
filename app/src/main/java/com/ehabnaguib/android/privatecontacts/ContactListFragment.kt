package com.ehabnaguib.android.privatecontacts

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.widget.SearchView
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.ehabnaguib.android.privatecontacts.databinding.FragmentContactListBinding
import com.ehabnaguib.android.privatecontacts.model.Contact
import com.ehabnaguib.android.privatecontacts.utils.SwipeToDeleteCallback
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import java.util.UUID


class ContactListFragment : Fragment() {


    private var _binding : FragmentContactListBinding? = null
    private val binding : FragmentContactListBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val contactListViewModel: ContactListViewModel by viewModels()

    private var searchView : SearchView? = null

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireActivity(), "You can now make phone calls.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireActivity(), "Allow permission from the settings.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        exitTransition = Fade()
        enterTransition= Fade()
        reenterTransition = Fade()
        returnTransition = Fade()
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactListViewModel.contacts.collect { contacts ->
                    binding.contactRecyclerView.adapter =
                        ContactListAdapter(requireActivity(), contacts) { contactId ->
                            findNavController().navigate(
                                ContactListFragmentDirections.openContactDetail(contactId)
                            )
                        }
                }
            }
        }
        val swipeHandler = object : SwipeToDeleteCallback(requireActivity()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                binding.contactRecyclerView.removeViewAt(viewHolder.adapterPosition)
                val number = contactListViewModel.getNumber(requireActivity(), viewHolder.adapterPosition)
                callNumber(number)
            }
        }

        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.contactRecyclerView)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.fragment_contact_list, menu)

        val searchItem: MenuItem = menu.findItem(R.id.search_contacts)
        searchView = searchItem.actionView as? SearchView

        var allContacts: List<Contact>? = null

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactListViewModel.contacts.collect { contacts ->
                    allContacts = contacts
                }
            }
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (allContacts != null && newText !=null) {
                    if(newText.isEmpty() && allContacts !=null)
                        binding.contactRecyclerView.adapter =
                            ContactListAdapter(requireActivity(), allContacts!!) { contactId ->
                                findNavController().navigate(
                                    ContactListFragmentDirections.openContactDetail(contactId)
                                )
                            }
                    else{
                        val searchContacts = allContacts!!.filter { contact ->
                            contact.name.contains(newText, ignoreCase = true)
                        } .sortedWith(compareBy(
                            { !it.name.startsWith(newText, ignoreCase = true) },
                            { it.name }
                        ))
                        binding.contactRecyclerView.adapter =
                            ContactListAdapter(requireActivity(), searchContacts) { contactId ->
                                findNavController().navigate(
                                    ContactListFragmentDirections.openContactDetail(contactId)
                                )
                            }
                    }
                }

                return true
            }
        })
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {



        return when (item.itemId) {
            R.id.new_contact -> {
                val newContact : Contact = Contact(id = UUID.randomUUID(), name = "")
                viewLifecycleOwner.lifecycleScope.launch {
                    contactListViewModel.addContact(newContact)
                    findNavController().navigate(
                        ContactListFragmentDirections.openContactDetail(newContact.id)
                    )
                }
                true
            }
            R.id.search_contacts -> {


                return true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    fun callNumber (number : String){
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))

        when {
            ContextCompat.checkSelfPermission(requireActivity(),
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(callIntent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), android.Manifest.permission.CALL_PHONE) -> {
                Toast.makeText(requireActivity(), "You just denied permission.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestPermissionLauncher.launch(
                    android.Manifest.permission.CALL_PHONE)
            }
        }
    }
}