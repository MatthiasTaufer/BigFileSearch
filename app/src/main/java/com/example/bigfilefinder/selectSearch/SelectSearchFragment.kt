package com.example.bigfilefinder.selectSearch

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bigfilefinder.R
import com.google.android.material.floatingactionbutton.FloatingActionButton
import kotlinx.coroutines.CompletableJob
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.pow

class SelectSearchFragment : Fragment() {

    private lateinit var documentsLauncher: ActivityResultLauncher<Uri?>
    private var listOfDirectories: MutableList<DocumentFile> = mutableListOf()
    private lateinit var searchTask: CompletableJob
    private var sizeFlag: Int = 1
    private var kiloByte = 1024.0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_search, container, false)
        val floatingActionButtonSearch: FloatingActionButton =
            view.findViewById(R.id.floatingActionSearch)
        //Load in adapter and manager for recycler view
        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewSearch)
        recyclerView.apply {
            adapter = SelectSearchAdapter(listOfDirectories)
            layoutManager = LinearLayoutManager(requireActivity())
        }

        //Initialize swipe to delete functions
        val swipeToDeleteCallback = object : SwipeToDeleteCallback() {
            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val pos = viewHolder.adapterPosition
                listOfDirectories.removeAt(pos)
                recyclerView.adapter?.notifyItemRemoved(pos)
            }
        }

        //Attach the callback
        val itemTouchHelper = ItemTouchHelper(swipeToDeleteCallback)
        itemTouchHelper.attachToRecyclerView(recyclerView)


        //initialize the floating button.
        floatingActionButtonSearch.setOnClickListener() {
            if (listOfDirectories.isEmpty()) {
                Toast.makeText(
                    requireContext(),
                    "Please select at least one directory", Toast.LENGTH_SHORT
                )
                    .show()
            } else {
                createDialog(view)
            }
        }

        //Register activity for folder request
        documentsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree())
        { uri ->
            if (uri != null) {
                DocumentFile.fromTreeUri(requireContext(), uri)?.let {
                    if (!checkForDuplicates(listOfDirectories, it)) {
                        listOfDirectories.add(it)
                    }
                }
                recyclerView.adapter?.notifyDataSetChanged()
                view.findViewById<TextView>(R.id.textView).visibility = View.GONE
            }
        }

        val floatingActionButtonAdd: FloatingActionButton =
            view.findViewById(R.id.floatingActionButtonAdd)
        floatingActionButtonAdd.setOnClickListener {
            documentsLauncher.launch(null)
        }


        return view
    }


    private fun createDialog(view: View) {

        val dialogBinding = layoutInflater.inflate(R.layout.search_options_dialog, null)
        val optionDialog = Dialog(requireContext())
        optionDialog.setContentView(dialogBinding)
        optionDialog.setCancelable(true)
        optionDialog.show()

        val amount: TextView = dialogBinding.findViewById(R.id.optionsAmount)
        val size: TextView = dialogBinding.findViewById(R.id.optionsSize)
        val depth: TextView = dialogBinding.findViewById(R.id.SearchDepth)
        val spinner: Spinner = dialogBinding.findViewById(R.id.spinner)
        val recursion = dialogBinding.findViewById<Switch>(R.id.optionsRecursiveSwitch)
        val confirmButton: Button = dialogBinding.findViewById(R.id.buttonConfirmOptions)

        //Show user field for defining the depth of search for the app if recursion is chosen
        recursion.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                depth.visibility = View.VISIBLE
            } else {
                depth.visibility = View.GONE
            }
        }


        //Set up for spinner to check what kind of size is being requested
        val spinnerAdapter = ArrayAdapter.createFromResource(
            requireContext(),
            R.array.Sizes,
            android.R.layout.simple_spinner_item
        )

        //Add in on select listener
        spinnerAdapter.setDropDownViewResource(
            androidx.transition
                .R.layout.support_simple_spinner_dropdown_item
        )
        spinner.adapter = spinnerAdapter
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                sizeFlag = position
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                return
            }
        }

        //Confirmation on click listener
        confirmButton.setOnClickListener {
            if (size.text.isEmpty() or size.text.contains(Regex("[a-zA-Z]+"))) {
                size.error = "This is a required field, please only use numbers."
                return@setOnClickListener
            }
            if (amount.text.isEmpty() or amount.text.contains(Regex("[a-zA-Z]+"))) {
                amount.error = "This is a required field, please only use numbers."
                return@setOnClickListener
            }
            optionDialog.dismiss()

            //show progress dialog and set up for moving to the next object
            val dialog = progressDialog()
            searchTask = Job()

            if (depth.text.contains(Regex("[a-zA-Z]+"))) {
                depth.error =
                    "This is an optional field only use number or leave blank for full depth search."
                return@setOnClickListener
            }

            var searchDepth: Int

            if (depth.text.isEmpty()) {
                searchDepth = -1
            } else {
                searchDepth = depth.text.toString().toInt()
            }

            search(
                amount.text.toString().toInt(),
                (size.text.toString().toDouble() * ((kiloByte).pow(sizeFlag))).toLong(),
                recursion.isChecked, searchDepth
            )
            searchTask.invokeOnCompletion {
                GlobalScope.launch(Main) {
                    dialog.dismiss()
                    view.findNavController()
                        .navigate(R.id.action_selectSearchFragment_to_resultsFragment)
                }

            }
        }
    }

    private fun search(amount: Int, size: Long, recursive: Boolean, depth: Int) {
        val hashSetDoc = HashSet<DocumentFile>()
        CoroutineScope(Default).launch {

            listOfDirectories.forEach { file ->
                if (file.isDirectory) {
                    hashSetDoc.addAll(
                        sort(
                            file.listFiles().toList(), amount,
                            size, recursive, depth - 1
                        )
                    )
                } else {
                    Toast.makeText(
                        requireContext(),
                        "${file.name}: is not a Directory skipping.", Toast.LENGTH_LONG
                    )
                        .show()
                }
            }

            var sortedList = hashSetDoc.toMutableList()
            val seenFiles = mutableSetOf<String>()
            val duplicates = sortedList.filter { file ->
                val identifier = "${file.name}_${file.length()}"
                val isDuplicate = seenFiles.contains(identifier)
                seenFiles.add(identifier)
                isDuplicate
            }
            sortedList.removeAll(duplicates)
            sortedList.sortByDescending { it.length() }

            if (sortedList.size > amount) {
                sortedList = sortedList.subList(0, amount)
            }
            val viewModel: SelectSearchViewModel by activityViewModels()
            viewModel.listOfFiles = sortedList
            searchTask.complete()
        }
    }

    private suspend fun sort(
        list: List<DocumentFile>, amount: Int,
        size: Long, recursive: Boolean, depth: Int
    )
            : MutableList<DocumentFile> {
        val hashSet: HashSet<DocumentFile> =
            list.filter { it.isFile && it.length() >= size }.toHashSet()
        if (recursive and (depth != 0)) {
            list.filter { it.isDirectory }
                .forEach { dir ->
                    val newList = dir.listFiles().toList()
                    hashSet.addAll(sort(newList, amount, size, true, depth - 1))
                }
        }
        var listOfBest = hashSet.toMutableList()
        listOfBest.sortByDescending { it.length() }
        if (listOfBest.size > amount) {
            listOfBest = listOfBest.subList(0, amount)
        }
        return listOfBest
    }

    private fun progressDialog(): Dialog {
        val progressBinding = layoutInflater.inflate(R.layout.processing_file_dialog, null)
        val progressDialog = Dialog(requireContext())
        progressDialog.setContentView(progressBinding)
        progressDialog.setCancelable(false)
        progressDialog.show()
        return progressDialog
    }

    private fun checkForDuplicates(list: List<DocumentFile>, file: DocumentFile): Boolean {
        val fileNamesSet = HashSet<String>()
        list.forEach {
            val filename = it.name
            if (!fileNamesSet.add(filename.toString())) {
                return false
            }
        }
        return !fileNamesSet.add(file.name.toString())
    }



    override fun onResume() {
        super.onResume()
        if (listOfDirectories.isNotEmpty()) {
            view?.findViewById<TextView>(R.id.textView)?.visibility = View.GONE
        }
    }

}