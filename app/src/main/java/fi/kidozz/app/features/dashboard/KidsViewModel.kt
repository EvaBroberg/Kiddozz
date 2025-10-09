package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.repository.KidsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class KidsViewModel(
    private val kidsRepository: KidsRepository?
) : ViewModel() {
    
    private val _kids = MutableStateFlow<List<Kid>>(emptyList())
    val kids: StateFlow<List<Kid>> = _kids.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadKids(daycareId: String, groupId: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (kidsRepository != null) {
                    val kidsList = kidsRepository.fetchKids(daycareId, groupId)
                    _kids.value = kidsList
                } else {
                    _kids.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load kids"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshKids(daycareId: String, groupId: String? = null) {
        viewModelScope.launch {
            loadKids(daycareId, groupId)
        }
    }
    
    fun updateAttendance(kidId: String, attendance: String) {
        viewModelScope.launch {
            if (kidsRepository != null) {
                println("KidsViewModel: Updating attendance for kid $kidId to $attendance")
                kidsRepository.updateAttendance(kidId, attendance).fold(
                    onSuccess = {
                        println("KidsViewModel: Successfully updated attendance for kid $kidId")
                        // Update the local state to reflect the change
                        _kids.value = _kids.value.map { kid ->
                            if (kid.id == kidId) {
                                kid.copy(attendance = attendance)
                            } else {
                                kid
                            }
                        }
                    },
                    onFailure = { exception ->
                        println("KidsViewModel: Failed to update attendance for kid $kidId: ${exception.message}")
                        _error.value = exception.message ?: "Failed to update attendance"
                    }
                )
            } else {
                println("KidsViewModel: kidsRepository is null, cannot update attendance")
            }
        }
    }
}
