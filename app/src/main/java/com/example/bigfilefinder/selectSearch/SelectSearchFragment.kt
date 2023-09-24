package com.example.bigfilefinder.selectSearch

import android.app.Dialog
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.findNavController
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

class SelectSearchFragment : Fragment() {

    private lateinit var documentsLauncher: ActivityResultLauncher<Uri?>
    private var listOfDirectories: MutableList<DocumentFile> = mutableListOf()
    private lateinit var searchTask: CompletableJob

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_search, container, false)


        val recyclerView: RecyclerView = view.findViewById(R.id.recyclerViewSearch)
        recyclerView.apply {
            adapter = SelectSearchAdapter(listOfDirectories)
            layoutManager = LinearLayoutManager(requireActivity())
        }

        val floatingActionButtonSearch: FloatingActionButton =
            view.findViewById(R.id.floatingActionSearch)
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


        documentsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree())
        { uri ->
            if (uri != null) {
                DocumentFile.fromTreeUri(requireContext(), uri)?.let { listOfDirectories.add(it) }
                recyclerView.adapter?.notifyDataSetChanged()
                view.findViewById<TextView>(R.id.textView).visibility = View.GONE
            } else {
                Toast.makeText(
                    requireContext(),
                    "Something went wrong.", Toast.LENGTH_LONG
                )
                    .show()
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

        val amount: TextView = dialogBinding.findViewById<TextView>(R.id.optionsAmount)
        val size: TextView = dialogBinding.findViewById<TextView>(R.id.optionsSize)
        val recursion = dialogBinding.findViewById<Switch>(R.id.optionsRecursiveSwitch)
        val confirmButton: Button = dialogBinding.findViewById<Button>(R.id.buttonConfirmOptions)


        confirmButton.setOnClickListener {
            if (size.text.isEmpty() or size.text.contains(Regex("[a-zA-Z]+"))) {
                size.error = "This is a require field, please only use numbers."
                return@setOnClickListener
            }
            if (amount.text.isEmpty() or amount.text.contains(Regex("[a-zA-Z]+"))) {
                amount.error = "This is a require field, please only use numbers."
                return@setOnClickListener
            }
            optionDialog.dismiss()
            val dialog = progressDialog()
            searchTask = Job()



            search(
                amount.text.toString().toInt(),
                size.text.toString().toLong(), recursion.isChecked
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

    private fun search(amount: Int, size: Long, recursive: Boolean) {
        var sortedList: MutableList<DocumentFile> = mutableListOf<DocumentFile>()
        CoroutineScope(Default).launch {
            listOfDirectories.forEach { file ->
                if (file.isDirectory) {
                    sortedList.addAll(sort(file.listFiles().toList(), amount, size, recursive))
                } else {
                    Toast.makeText(
                        requireContext(),
                        "${file.name}: is not a Directory skipping.", Toast.LENGTH_LONG
                    )
                        .show()
                }
            }
            sortedList.sortByDescending { it.length() }

            sortedList = sortedList.subList(0, amount)
            val viewModel: SelectSearchViewModel by activityViewModels()
            viewModel.listOfFiles = sortedList
            searchTask.complete()
        }
    }

    private suspend fun sort(list: List<DocumentFile>, amount: Int, size: Long, recursive: Boolean)
            : MutableList<DocumentFile> {
        delay(4000)
        val listOfBest: MutableList<DocumentFile> =
            list.filter { it.isFile && it.length() >= size }.toMutableList()
        if (recursive) {
            list.filter { it.isDirectory }
                .forEach { dir ->
                    val newList = dir.listFiles().toList()
                    listOfBest.addAll(sort(newList, amount, size, true))
                }
        }
        listOfBest.sortByDescending { it.length() }
        return listOfBest.subList(0, amount)
    }

    private fun progressDialog(): Dialog {
        val progressBinding = layoutInflater.inflate(R.layout.processing_file_dialog, null)
        val progressDialog = Dialog(requireContext())
        progressDialog.setContentView(progressBinding)
        progressDialog.setCancelable(false)
        progressDialog.show()
        return progressDialog
    }
}