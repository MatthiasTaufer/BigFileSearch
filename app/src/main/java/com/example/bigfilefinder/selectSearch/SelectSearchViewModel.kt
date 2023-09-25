package com.example.bigfilefinder.selectSearch

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel


class SelectSearchViewModel : ViewModel() {
    var listOfFiles: MutableList<DocumentFile>? = null
}