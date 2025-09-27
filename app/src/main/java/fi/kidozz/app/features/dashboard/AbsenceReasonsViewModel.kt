package fi.kidozz.app.features.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import fi.kidozz.app.data.repository.KidsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AbsenceReasonsViewModel(
    private val kidsRepository: KidsRepository
) : ViewModel() {
    private val _absenceReasons = MutableStateFlow<List<String>>(emptyList())
    val absenceReasons: StateFlow<List<String>> = _absenceReasons.asStateFlow()

    fun loadAbsenceReasons() {
        viewModelScope.launch {
            _absenceReasons.value = kidsRepository.getAbsenceReasons()
        }
    }
}
