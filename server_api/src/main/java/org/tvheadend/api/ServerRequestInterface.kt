package org.tvheadend.api

import android.content.Intent

interface ServerRequestInterface {
    fun getDiscSpace()
    fun getSystemTime()
    fun getChannel(intent: Intent)
    fun getEvent(intent: Intent)
    fun getEvents(intent: Intent)
    fun getEpgQuery(intent: Intent)
    fun addDvrEntry(intent: Intent)
    fun updateDvrEntry(intent: Intent)
    fun cancelDvrEntry(intent: Intent)
    fun deleteDvrEntry(intent: Intent)
    fun stopDvrEntry(intent: Intent)
    fun addAutorecEntry(intent: Intent)
    fun updateAutorecEntry(intent: Intent)
    fun deleteAutorecEntry(intent: Intent)
    fun addTimerrecEntry(intent: Intent)
    fun updateTimerrecEntry(intent: Intent)
    fun deleteTimerrecEntry(intent: Intent)
    fun getTicket(intent: Intent)
    fun getProfiles()
    fun getDvrConfigs()
    fun getSubscriptions()
    fun getInputs()
    fun getMoreEvents(intent: Intent)
}