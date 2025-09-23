package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.EducatorApiService
import fi.kidozz.app.data.models.Educator

class EducatorRepository(
    private val educatorApiService: EducatorApiService
) {
    suspend fun getCurrentEducator(token: String): Educator? {
        val response = educatorApiService.getCurrentEducator("Bearer $token")
        return if (response.isSuccessful) {
            response.body()
        } else {
            null
        }
    }
    
    suspend fun getEducators(daycareId: String, search: String? = null): List<Educator> {
        return educatorApiService.getEducators(daycareId, search)
    }
    
    suspend fun getEducatorByName(daycareId: String, name: String): Educator? {
        val educators = getEducators(daycareId, name)
        return educators.find { it.full_name.equals(name, ignoreCase = true) }
    }
}
