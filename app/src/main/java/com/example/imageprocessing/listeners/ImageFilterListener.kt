package com.example.imageprocessing.listeners

import com.example.imageprocessing.data.ImageFilter

interface ImageFilterListener {
    fun onFilterSelected(imageFilter: ImageFilter)
}