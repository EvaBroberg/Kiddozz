package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.KidsApiService
import fi.kidozz.app.data.auth.TokenManager
import fi.kidozz.app.data.models.Kid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class KidsRepository(
    private val api: KidsApiService,
    private val tokenManager: TokenManager
) {
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
    
    suspend fun submitAbsence(
        kidId: String,
        dates: List<String>,
        reason: String,
        details: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            println("KidsRepository: Submitting absence for kid $kidId")
            println("KidsRepository: Dates: $dates, Reason: $reason, Details: $details")
            
            // Create individual absence records for each date
            var allSuccessful = true
            val errors = mutableListOf<String>()
            
            for (date in dates) {
                val absenceData = mapOf(
                    "date" to date,
                    "reason" to reason
                )
                
                println("KidsRepository: Submitting absence for date: $date")
                val token = tokenManager.getToken()
                if (token == null) {
                    allSuccessful = false
                    errors.add("No authentication token found")
                    continue
                }
                val response = api.submitAbsence(kidId, "Bearer $token", absenceData)
                println("KidsRepository: Response for $date - code: ${response.code()}, success: ${response.isSuccessful}")
                
                if (!response.isSuccessful) {
                    allSuccessful = false
                    errors.add("Failed to submit absence for $date: ${response.code()}")
                }
            }
            
            if (allSuccessful) {
                println("KidsRepository: Successfully submitted all absences to database")
                Result.success(Unit)
            } else {
                println("KidsRepository: Some absence submissions failed: ${errors.joinToString(", ")}")
                Result.failure(Exception("Failed to submit some absences: ${errors.joinToString(", ")}"))
            }
        } catch (e: Exception) {
            println("KidsRepository: Exception occurred during absence submission: ${e.message}")
            Result.failure(e)
        }
    }
    
    suspend fun getAbsences(kidId: String): Result<List<Map<String, Any>>> = withContext(Dispatchers.IO) {
        try {
            println("KidsRepository: Fetching absences for kid $kidId")
            val token = tokenManager.getToken()
            if (token == null) {
                return@withContext Result.failure(Exception("No authentication token found"))
            }
            
            val response = api.getAbsences(kidId, "Bearer $token")
            println("KidsRepository: Get absences response - code: ${response.code()}, success: ${response.isSuccessful}")
            
            if (response.isSuccessful) {
                val absences = response.body() ?: emptyList()
                println("KidsRepository: Successfully fetched ${absences.size} absences")
                Result.success(absences)
            } else {
                println("KidsRepository: Failed to fetch absences: ${response.code()}")
                Result.failure(Exception("Failed to fetch absences: ${response.code()}"))
            }
        } catch (e: Exception) {
            println("KidsRepository: Exception occurred while fetching absences: ${e.message}")
            Result.failure(e)
        }
    }
}
