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
}
