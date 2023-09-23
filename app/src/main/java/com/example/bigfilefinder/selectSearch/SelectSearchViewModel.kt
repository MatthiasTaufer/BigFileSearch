package com.example.bigfilefinder.selectSearch

import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import java.io.File


class SelectSearchViewModel : ViewModel() {
    var listOfFiles: MutableList<DocumentFile>? = null
}