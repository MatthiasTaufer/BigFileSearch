package com.example.bigfilefinder.selectSearch

import android.Manifest
import android.app.Dialog
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.Tag
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.registerForActivityResult
import androidx.core.app.ActivityCompat

import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bigfilefinder.R
import com.example.bigfilefinder.databinding.FragmentSelectSearchBinding
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File

class SelectSearchFragment : Fragment() {

    private lateinit var documentsLauncher: ActivityResultLauncher<Uri?>
    private lateinit var viewModel: SelectSearchViewModel
    private lateinit var binding: FragmentSelectSearchBinding
    private var isReadPermissionGranted = false
    private var listOfDirectories: MutableList<File> = mutableListOf()

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
            if(!listOfDirectories.isEmpty()){
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
                listOfDirectories.add(File(uri.path!!))
                recyclerView.adapter?.notifyDataSetChanged()
                Log.d(tag,uri.toString())
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
        val confirmButton:Button = dialogBinding.findViewById<Button>(R.id.buttonConfirmOptions)

        confirmButton.setOnClickListener{
            search(view, amount.text.toString().toInt(), size.text.toString().toLong())
        }
    }

    private fun search(view: View, amount: Int, size: Long){

    }
}