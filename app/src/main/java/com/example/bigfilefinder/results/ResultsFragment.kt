package com.example.bigfilefinder.results

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bigfilefinder.R
import com.example.bigfilefinder.selectSearch.SelectSearchViewModel


class ResultsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_results, container, false)

        val viewModel: SelectSearchViewModel by activityViewModels()


        val recyclerView:RecyclerView = view.findViewById(R.id.recyclerViewResult)
        recyclerView.apply {
            adapter = ResultsAdapter(viewModel.listOfFiles!!)
            layoutManager = LinearLayoutManager(requireActivity())
        }
        return view
    }

}