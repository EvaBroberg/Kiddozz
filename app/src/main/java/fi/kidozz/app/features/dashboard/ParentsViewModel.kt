package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.Kid
import fi.kidozz.app.data.repository.ParentsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for parent dashboard functionality.
 * Manages state for kids data, loading, and error states.
 */
class ParentsViewModel(
    private val parentsRepository: ParentsRepository
) : ViewModel() {
    
    private val _kids = MutableStateFlow<List<Kid>>(emptyList())
    val kids: StateFlow<List<Kid>> = _kids.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    /**
     * Load kids for a specific parent.
     * 
     * @param parentId The ID of the parent
     */
    fun loadKidsForParent(parentId: String) {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                _error.value = null
                
                val kidsList = parentsRepository.getKidsForParent(parentId)
                _kids.value = kidsList
            } catch (e: Exception) {
                _error.value = e.message ?: "Unknown error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    /**
     * Refresh kids data for a specific parent.
     * 
     * @param parentId The ID of the parent
     */
    fun refreshKids(parentId: String) {
        viewModelScope.launch {
            loadKidsForParent(parentId)
        }
    }
    
    /**
     * Clear error state.
     */
    fun clearError() {
        _error.value = null
    }
}
