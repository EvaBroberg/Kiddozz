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
    
    fun loadCurrentEducator(token: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (educatorRepository != null) {
                    val educator = educatorRepository.getCurrentEducator(token)
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
    
    fun loadCurrentEducator(daycareId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (educatorRepository != null) {
                    // For now, we'll search for Jessica specifically
                    // In a real app, this would use JWT token from dev-login
                    val educators = educatorRepository.getEducators(daycareId)
                    _currentEducator.value = educators.find { it.full_name == "Jessica" }
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
