package com.example.bigfilefinder.selectSearch

import androidx.lifecycle.ViewModel
import java.io.File


class SelectSearchViewModel : ViewModel() {

    var listOfDirectories: MutableList<File>? = null
    var amount: Int? = null
    var size: Int? = null
    var listOfFiles: MutableList<File>? = null
}