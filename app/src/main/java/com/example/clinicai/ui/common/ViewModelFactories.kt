package com.example.clinicai.ui.common

import androidx.activity.ComponentActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.clinicai.AppContainer
import com.example.clinicai.ClinicAIApplication

val Fragment.container: AppContainer
    get() = (requireActivity().application as ClinicAIApplication).container

val ComponentActivity.container: AppContainer
    get() = (application as ClinicAIApplication).container

/** ViewModel scoped to this fragment, built with the app's dependency container. */
inline fun <reified VM : ViewModel> Fragment.appViewModels(crossinline create: (AppContainer) -> VM) =
    viewModels<VM> {
        viewModelFactory { initializer { create(container) } }
    }

/** ViewModel shared by every fragment of the host activity (used for the booking flow). */
inline fun <reified VM : ViewModel> Fragment.sharedAppViewModels(crossinline create: (AppContainer) -> VM) =
    activityViewModels<VM> {
        viewModelFactory { initializer { create(container) } }
    }

inline fun <reified VM : ViewModel> ComponentActivity.appViewModels(crossinline create: (AppContainer) -> VM) =
    androidx.lifecycle.ViewModelLazy(
        VM::class,
        { viewModelStore },
        {
            viewModelFactory { initializer { create(container) } }
        },
        { defaultViewModelCreationExtras },
    )
