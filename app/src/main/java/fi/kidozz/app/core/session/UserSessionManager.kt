package fi.kidozz.app.core.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class UserSessionManager(initial: UserSession) {
    private val _session = MutableStateFlow(initial)
    val session: StateFlow<UserSession> = _session
    
    fun update(session: UserSession) { 
        _session.value = session 
    }
}