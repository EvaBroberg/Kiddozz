package fi.kidozz.app.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.sample.sampleUpcomingEvents
import fi.kidozz.app.data.sample.samplePastEvents
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EventRepository(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("events_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()
    
    private val _upcomingEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val upcomingEvents: StateFlow<List<CalendarEvent>> = _upcomingEvents.asStateFlow()
    
    private val _pastEvents = MutableStateFlow<List<CalendarEvent>>(emptyList())
    val pastEvents: StateFlow<List<CalendarEvent>> = _pastEvents.asStateFlow()
    
    init {
        loadEvents()
    }
    
    private fun loadEvents() {
        val upcomingJson = prefs.getString("upcoming_events", null)
        val pastJson = prefs.getString("past_events", null)
        
        if (upcomingJson != null) {
            val type = object : TypeToken<List<CalendarEvent>>() {}.type
            _upcomingEvents.value = gson.fromJson(upcomingJson, type)
        } else {
            // Load sample data if no saved data exists
            _upcomingEvents.value = sampleUpcomingEvents
            saveUpcomingEvents()
        }
        
        if (pastJson != null) {
            val type = object : TypeToken<List<CalendarEvent>>() {}.type
            _pastEvents.value = gson.fromJson(pastJson, type)
        } else {
            // Load sample data if no saved data exists
            _pastEvents.value = samplePastEvents
            savePastEvents()
        }
    }
    
    private fun saveUpcomingEvents() {
        val json = gson.toJson(_upcomingEvents.value)
        prefs.edit().putString("upcoming_events", json).apply()
    }
    
    private fun savePastEvents() {
        val json = gson.toJson(_pastEvents.value)
        prefs.edit().putString("past_events", json).apply()
    }
    
    fun addEvent(event: CalendarEvent) {
        val currentTime = java.time.LocalDateTime.now()
        if (event.dateTime.isAfter(currentTime)) {
            val updatedList = _upcomingEvents.value.toMutableList()
            updatedList.add(event)
            _upcomingEvents.value = updatedList
            saveUpcomingEvents()
        } else {
            val updatedList = _pastEvents.value.toMutableList()
            updatedList.add(event)
            _pastEvents.value = updatedList
            savePastEvents()
        }
    }
    
    fun updateEvent(updatedEvent: CalendarEvent) {
        val currentTime = java.time.LocalDateTime.now()
        
        // Remove from both lists first
        val upcomingList = _upcomingEvents.value.toMutableList()
        val pastList = _pastEvents.value.toMutableList()
        
        upcomingList.removeAll { it.id == updatedEvent.id }
        pastList.removeAll { it.id == updatedEvent.id }
        
        // Add to appropriate list based on date
        if (updatedEvent.dateTime.isAfter(currentTime)) {
            upcomingList.add(updatedEvent)
            _upcomingEvents.value = upcomingList
            saveUpcomingEvents()
        } else {
            pastList.add(updatedEvent)
            _pastEvents.value = pastList
            savePastEvents()
        }
    }
    
    fun deleteEvent(eventId: String) {
        val upcomingList = _upcomingEvents.value.toMutableList()
        val pastList = _pastEvents.value.toMutableList()
        
        val upcomingRemoved = upcomingList.removeAll { it.id == eventId }
        val pastRemoved = pastList.removeAll { it.id == eventId }
        
        if (upcomingRemoved) {
            _upcomingEvents.value = upcomingList
            saveUpcomingEvents()
        }
        
        if (pastRemoved) {
            _pastEvents.value = pastList
            savePastEvents()
        }
    }
    
    fun getAllEvents(): List<CalendarEvent> {
        return _upcomingEvents.value + _pastEvents.value
    }
}
