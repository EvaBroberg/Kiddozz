package fi.kidozz.app.features.calendar

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.CalendarEvent
import fi.kidozz.app.data.repository.EventRepository
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class EventViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EventRepository(application)
    
    val upcomingEvents: StateFlow<List<CalendarEvent>> = repository.upcomingEvents
    val pastEvents: StateFlow<List<CalendarEvent>> = repository.pastEvents
    
    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.addEvent(event)
        }
    }
    
    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.updateEvent(event)
        }
    }
    
    fun deleteEvent(eventId: String) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
        }
    }
    
    fun getAllEvents(): List<CalendarEvent> {
        return repository.getAllEvents()
    }
}
