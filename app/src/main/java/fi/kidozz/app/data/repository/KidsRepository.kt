package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.KidsApiService
import fi.kidozz.app.data.models.Kid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KidsRepository(private val api: KidsApiService) {
    suspend fun fetchKids(daycareId: String, groupId: String? = null): List<Kid> {
        return api.getKids(daycareId, groupId)
    }
    
    suspend fun updateAttendance(kidId: String, attendance: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = api.updateAttendance(kidId, mapOf("attendance" to attendance))
            if (response.isSuccessful) {
                Result.success(Unit)
            } else {
                Result.failure(Exception("Failed to update attendance: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
