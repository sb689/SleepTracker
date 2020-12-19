/*
 * Copyright 2018, The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.trackmysleepquality.sleeptracker

import android.app.Application
import android.text.Spanned
import androidx.lifecycle.*
import com.example.android.trackmysleepquality.database.SleepDatabaseDao
import com.example.android.trackmysleepquality.database.SleepNight
import com.example.android.trackmysleepquality.formatNights
import kotlinx.coroutines.*

/**
 * ViewModel for SleepTrackerFragment.
 */
class SleepTrackerViewModel(
        val database: SleepDatabaseDao,
        application: Application) : AndroidViewModel(application) {


    private var viewModelJOb = Job()

    override fun onCleared() {
        super.onCleared()
        viewModelJOb.cancel()
    }

    private val _showSnackBarEvent = MutableLiveData<Boolean>()
    val showSnackBarEvent : LiveData<Boolean>
        get() = _showSnackBarEvent

    private val _navigateToSleepQuality = MutableLiveData<SleepNight>()
    val navigateToSleepQuality: LiveData<SleepNight>
    get() = _navigateToSleepQuality

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJOb)
    private var toNight = MutableLiveData<SleepNight?>()
    val nights = database.getAllNights()


    val nightString: LiveData<Spanned> = Transformations.map(nights){ nights ->
        formatNights(nights, application.resources)
    }

    init {
        initializeTonight()
    }

    fun doneShowingSnackBar(){
        _showSnackBarEvent.value = false;
    }

    private fun initializeTonight() {
        uiScope.launch {
            toNight.value = getTonightFromDB()
        }
    }

    val startButtonVisible = Transformations.map(toNight){
        null == it
    }

    val stopButtonVisible = Transformations.map(toNight){
        null != it
    }

    val clearButtonVisible = Transformations.map(nights){
         it?.isNotEmpty()
    }

    fun doneNavigating()
    {
        _navigateToSleepQuality.value = null
    }

    private suspend fun getTonightFromDB(): SleepNight? {
        return withContext(Dispatchers.IO)
        {
            var night = database.getTonight()
            if (night?.endTimeMilli != night?.startTimeMilli) {
                night = null
            }
            night
        }
    }

    fun onStartTracking()
    {
        uiScope.launch {
            val newNight = SleepNight()
            insert(newNight)
            toNight.value = getTonightFromDB()
        }
    }

    private suspend fun insert(night : SleepNight)
    {
        withContext(Dispatchers.IO)
        {
            database.insert(night)
        }
    }

    fun onStopClick(){
        uiScope.launch {
            val night = toNight.value ?: return@launch
            night.endTimeMilli = System.currentTimeMillis()
            update(night)
            _navigateToSleepQuality.value = night
        }
    }

    private suspend fun update(night: SleepNight)
    {
        withContext(Dispatchers.IO)
        {
            database.update(night)
        }
    }

    fun onClear()
    {
        uiScope.launch {
            clear()
            toNight.value = null
            _showSnackBarEvent.value = true;
        }
    }

    private suspend fun clear(){
        withContext(Dispatchers.IO)
        {
            database.clear()
        }
    }


}

