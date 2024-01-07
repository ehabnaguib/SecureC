package com.ehabnaguib.android.securec

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import androidx.core.view.MenuProvider
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Fade
import com.ehabnaguib.android.securec.databinding.FragmentContactListBinding
import com.ehabnaguib.android.securec.model.Contact
import com.ehabnaguib.android.securec.utils.SwipeToDeleteCallback
import kotlinx.coroutines.launch


class ContactListFragment : Fragment() {

    private var _binding : FragmentContactListBinding? = null
    private val binding : FragmentContactListBinding
        get() =  checkNotNull(_binding) {
            "cannot access binding because it's null."
        }

    private val contactListViewModel: ContactListViewModel by viewModels()

    private var allContacts : List<Contact>? = null
    private var searchResult : List<Contact>? = null

    private val requestCallPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(requireContext(), "You can now make phone calls.", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(requireContext(), "You need to allow call permission from your phone settings.", Toast.LENGTH_SHORT).show()
        }
    }



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        exitTransition = Fade()
        enterTransition= Fade()
        reenterTransition = Fade()
        returnTransition = Fade()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentContactListBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.contactRecyclerView.layoutManager = LinearLayoutManager(context)

        // Listening for changes in the contact list and re-populate the recycler view
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                contactListViewModel.contacts.collect { contacts ->
                    binding.contactRecyclerView.adapter =
                        ContactListAdapter(requireContext(), contacts) { contactId ->
                            findNavController().navigate(
                                ContactListFragmentDirections.openContactDetail(contactId)
                            )
                        }
                    allContacts = contacts
                    searchResult = allContacts
                }
            }
        }

        requireActivity().addMenuProvider(createMenuProvider(), viewLifecycleOwner, Lifecycle.State.RESUMED)



        // Adding the swipe-to-call functionality to the contact list
        val swipeHandler = object : SwipeToDeleteCallback(requireContext()) {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                binding.contactRecyclerView.removeViewAt(viewHolder.adapterPosition)

                if (searchResult != null)
                    callNumber(searchResult!![viewHolder.adapterPosition].number)
            }
        }
        val itemTouchHelper = ItemTouchHelper(swipeHandler)
        itemTouchHelper.attachToRecyclerView(binding.contactRecyclerView)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun createMenuProvider() = object : MenuProvider {
        override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
            menuInflater.inflate(R.menu.fragment_contact_list, menu)

            val searchItem: MenuItem = menu.findItem(R.id.search_contacts)
            val searchView = searchItem.actionView as? SearchView

            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    if (allContacts != null && newText != null) {
                        if (newText.isEmpty())
                            binding.contactRecyclerView.adapter =
                                ContactListAdapter(requireContext(), allContacts!!) { contactId ->
                                    findNavController().navigate(
                                        ContactListFragmentDirections.openContactDetail(contactId)
                                    )
                                }
                        else {
                            searchResult = allContacts!!.filter { contact ->
                                contact.name.contains(newText, ignoreCase = true)
                            }.sortedWith(compareBy(
                                { !it.name.startsWith(newText, ignoreCase = true) },
                                { it.name }
                            ))
                            binding.contactRecyclerView.adapter =
                                ContactListAdapter(requireContext(), searchResult!!) { contactId ->
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

        override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
            return when (menuItem.itemId) {
                R.id.new_contact -> {
                    findNavController().navigate(
                        ContactListFragmentDirections.openContactDetail(null)
                    )
                    true
                }
                else -> false
            }
        }
    }

    private fun callNumber (number : String){
        val callIntent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number"))

        when {
            ContextCompat.checkSelfPermission(requireContext(),
                android.Manifest.permission.CALL_PHONE
            ) == PackageManager.PERMISSION_GRANTED -> {
                startActivity(callIntent)
            }
            ActivityCompat.shouldShowRequestPermissionRationale(
                requireActivity(), android.Manifest.permission.CALL_PHONE) -> {
                Toast.makeText(requireContext(), "You need to allow call permission from your phone settings.", Toast.LENGTH_SHORT).show()
            }
            else -> {
                requestCallPermissionLauncher.launch(
                    android.Manifest.permission.CALL_PHONE)
            }
        }
    }
}