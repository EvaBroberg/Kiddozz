package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.models.Educator
import fi.kidozz.app.data.repository.EducatorRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class EducatorsListViewModel(
    private val educatorRepository: EducatorRepository
) : ViewModel() {

    private val _educators = MutableStateFlow<List<Educator>>(emptyList())
    val educators: StateFlow<List<Educator>> = _educators.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun load(daycareId: String, search: String? = null) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            try {
                _educators.value = educatorRepository.getEducators(daycareId, search)
            } catch (e: Exception) {
                _error.value = e.message
                _educators.value = emptyList()
            } finally {
                _isLoading.value = false
            }
        }
    }
}
