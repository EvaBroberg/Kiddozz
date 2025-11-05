package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.repository.EducatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EducatorViewModel(
    private val educatorRepository: EducatorRepository?
) : ViewModel() {
    
    private val _currentEducator = MutableStateFlow<Educator?>(null)
    val currentEducator: StateFlow<Educator?> = _currentEducator.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load current educator by ID from token.
     * Falls back to name-based lookup if ID not found.
     */
    fun loadCurrentEducatorById(daycareId: String, educatorId: String?) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                if (educatorRepository != null) {
                    var educator: Educator? = null
                    
                    // Try to find educator by ID first
                    if (educatorId != null) {
                        val educators = educatorRepository.getEducators(daycareId)
                        educator = educators.find { it.id == educatorId }
                        if (educator != null) {
                            android.util.Log.d("EducatorViewModel", "Loaded educator by ID: ${educator.full_name} (id=$educatorId)")
                        }
                    }
                    
                    // Fallback to name-based lookup if ID not found (for backward compatibility)
                    if (educator == null) {
                        educator = educatorRepository.getEducatorByName(daycareId, "Jessica")
                        if (educator != null) {
                            android.util.Log.d("EducatorViewModel", "Fallback: Loaded educator by name: ${educator.full_name}")
                        }
                    }
                    
                    _currentEducator.value = educator
                    educator?.let {
                        android.util.Log.d("EducatorFilter", "educator loaded: ${it.full_name} with groups: ${it.groups.map { group -> "${group.id}(${group.name})" }}")
                    }
                } else {
                    _currentEducator.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load educator"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    @Deprecated("Use loadCurrentEducatorById instead")
    fun loadCurrentEducatorByDaycare(daycareId: String) {
        loadCurrentEducatorById(daycareId, null)
    }
    
    fun loadEducatorByName(daycareId: String, name: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (educatorRepository != null) {
                    val educator = educatorRepository.getEducatorByName(daycareId, name)
                    _currentEducator.value = educator
                } else {
                    _currentEducator.value = null
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load educator"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
