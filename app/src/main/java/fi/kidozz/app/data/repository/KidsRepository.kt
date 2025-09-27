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
            println("KidsRepository: Making API call to update attendance for kid $kidId to $attendance")
            val response = api.updateAttendance(kidId, mapOf("attendance" to attendance))
            println("KidsRepository: API response code: ${response.code()}, success: ${response.isSuccessful}")
            if (response.isSuccessful) {
                println("KidsRepository: Successfully updated attendance in database")
                Result.success(Unit)
            } else {
                println("KidsRepository: API call failed with code ${response.code()}")
                Result.failure(Exception("Failed to update attendance: ${response.code()}"))
            }
        } catch (e: Exception) {
            println("KidsRepository: Exception occurred: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getAbsenceReasons(): List<String> = withContext(Dispatchers.IO) {
        try {
            val response = api.getAbsenceReasons()
            if (response.isSuccessful) {
                response.body()?.absence_reasons ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
