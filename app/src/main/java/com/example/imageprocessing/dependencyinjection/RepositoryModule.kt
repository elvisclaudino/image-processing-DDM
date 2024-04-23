package com.example.imageprocessing.dependencyinjection

import com.example.imageprocessing.repositories.EditImageRepositoryImpl
import com.example.imageprocessing.repositories.EditimageReposity
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val repositoryModule = module {
    factory<EditimageReposity> {EditImageRepositoryImpl(androidContext())}
}