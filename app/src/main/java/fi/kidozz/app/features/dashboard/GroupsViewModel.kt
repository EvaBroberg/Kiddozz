package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.Group
import fi.kidozz.app.data.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GroupsViewModel(
    private val groupsRepository: GroupsRepository?
) : ViewModel() {
    
    private val _groups = MutableStateFlow<List<Group>>(emptyList())
    val groups: StateFlow<List<Group>> = _groups.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    fun loadGroups(daycareId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                if (groupsRepository != null) {
                    val groupsList = groupsRepository.getGroups(daycareId)
                    _groups.value = groupsList
                    android.util.Log.d("EducatorFilter", "groups loaded: ${groupsList.map { "${it.id}(${it.name})" }}")
                } else {
                    // Fallback to empty list if no repository
                    _groups.value = emptyList()
                }
            } catch (e: Exception) {
                _error.value = e.message ?: "Failed to load groups"
            } finally {
                _isLoading.value = false
            }
        }
    }
}
