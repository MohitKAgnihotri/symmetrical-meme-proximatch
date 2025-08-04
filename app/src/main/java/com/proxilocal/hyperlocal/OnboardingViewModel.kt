package com.proxilocal.hyperlocal

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class OnboardingViewModel : ViewModel() {

    private val _gender = MutableStateFlow<Gender?>(null)
    val gender = _gender.asStateFlow()

    private val _myCriteria = MutableStateFlow<List<Int>>(emptyList())
    val myCriteria = _myCriteria.asStateFlow()

    private val _theirCriteria = MutableStateFlow<List<Int>>(emptyList())
    val theirCriteria = _theirCriteria.asStateFlow()

    fun onGenderSelected(gender: Gender) {
        _gender.value = gender
    }

    fun onMyVibesSelected(indices: List<Int>) {
        _myCriteria.value = indices
    }

    fun onTheirVibesSelected(indices: List<Int>) {
        _theirCriteria.value = indices
    }
}