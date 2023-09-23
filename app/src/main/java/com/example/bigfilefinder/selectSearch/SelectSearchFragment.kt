package com.example.bigfilefinder.selectSearch

import android.app.Dialog
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
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
import java.io.File

class SelectSearchFragment : Fragment() {

    private lateinit var documentsLauncher: ActivityResultLauncher<Uri?>
    private var listOfDirectories: MutableList<DocumentFile> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_select_search, container, false)


        var recyclerView:RecyclerView = view.findViewById(R.id.recyclerViewSearch)
        recyclerView.apply {
            adapter = SelectSearchAdapter(listOfDirectories)
            layoutManager = LinearLayoutManager(requireActivity())
        }

        val floatingActionButtonSearch:FloatingActionButton =
            view.findViewById(R.id.floatingActionSearch)
        floatingActionButtonSearch.setOnClickListener(){
            if(listOfDirectories.isEmpty()){
                Toast.makeText(requireContext(),
                    "Please select at least one directory",Toast.LENGTH_SHORT)
                    .show()
            }else{
                createDialog(view)
            }
        }


        documentsLauncher = registerForActivityResult(ActivityResultContracts.OpenDocumentTree())
        { uri ->
            if(uri != null){
                DocumentFile.fromTreeUri(requireContext(),uri)?.let { listOfDirectories.add(it) }
                recyclerView.adapter?.notifyDataSetChanged()
            }else{
                Toast.makeText(requireContext(),
                    "Something went wrong.",Toast.LENGTH_LONG)
                    .show()
            }
        }

        val floatingActionButtonAdd: FloatingActionButton =
            view.findViewById(R.id.floatingActionButtonAdd)
        floatingActionButtonAdd.setOnClickListener{
            documentsLauncher.launch(null)
        }


        return view
    }


    private fun createDialog(view: View){

        val dialogBinding = layoutInflater.inflate(R.layout.search_options_dialog, null)
        val optionDialog = Dialog(requireContext())
        optionDialog.setContentView(dialogBinding)
        optionDialog.setCancelable(true)
        //optionDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        optionDialog.show()

        val amount:TextView = dialogBinding.findViewById<TextView>(R.id.optionsAmount)
        val size:TextView = dialogBinding.findViewById<TextView>(R.id.optionsSize)
        val recursion:Switch = dialogBinding.findViewById<Switch>(R.id.optionsRecursiveSwitch)
        val confirmButton:Button = dialogBinding.findViewById<Button>(R.id.buttonConfirmOptions)


        confirmButton.setOnClickListener{
            if(size.text.isEmpty() or size.text.contains(Regex("[a-zA-Z]+"))){
                size.error="This is a require field, please only use numbers."
                return@setOnClickListener
            }
            if(amount.text.isEmpty() or amount.text.contains(Regex("[a-zA-Z]+"))){
                amount.error="This is a require field, please only use numbers."
                return@setOnClickListener
            }
            search(view, amount.text.toString().toInt(),
                size.text.toString().toLong(),recursion.isChecked)
            optionDialog.dismiss()
            view.findNavController().navigate(R.id.action_selectSearchFragment_to_resultsFragment)
        }
    }

    private fun search(view: View, amount: Int, size: Long, recursive:Boolean){
        var sortedList:MutableList<DocumentFile> = mutableListOf<DocumentFile>()
        listOfDirectories.forEach{ file->
            if(file.isDirectory){
                Log.e(tag,"${file.listFiles()}")
                sortedList.addAll(sort(file.listFiles().toList(),amount,size,recursive))
            }else{
                Toast.makeText(requireContext(),
                    "${file.name}: is not a Directory skipping.",Toast.LENGTH_LONG)
                    .show()
            }
        }
        sortedList.sortByDescending { it.length() }
        if(sortedList.size > amount){
            sortedList = sortedList.subList(0,amount)
        }
        val viewModel: SelectSearchViewModel by activityViewModels()
        viewModel.listOfFiles = sortedList
    }

    private fun sort(list: List<DocumentFile>, amount: Int, size: Long, recursive:Boolean)
    :MutableList<DocumentFile>{
        var listOfBest = mutableListOf<DocumentFile>()
        list.forEach{ file ->
            //make a recursive call if the file is a directory
            if(file.isDirectory and recursive){
                val result = sort(file.listFiles().toList(), amount, size, recursive)
                listOfBest.addAll(result)
                listOfBest.sortByDescending { it.length() }
                listOfBest = listOfBest.subList(0,amount)
                return@forEach
            }
            //if it isn't a file and we don't have recursion check for size
            if (file.length() > size) {
                if(listOfBest.isEmpty()){
                    listOfBest.add(file)
                    return@forEach
                }
                if (file.length() > listOfBest.last().length()) {
                    if (listOfBest.size < amount) {
                        listOfBest.add(file)
                        listOfBest.sortByDescending { it.length() }
                    } else {
                        listOfBest[listOfBest.size - 1] = file
                        listOfBest.sortByDescending { it.length() }
                    }
                }
            }
        }
        return listOfBest
    }
}