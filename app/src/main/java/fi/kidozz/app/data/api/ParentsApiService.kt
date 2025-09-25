package fi.kidozz.app.data.api

import fi.kidozz.app.data.models.Kid
import retrofit2.http.GET
import retrofit2.http.Path

/**
 * Retrofit API service for parent-related endpoints.
 */
interface ParentsApiService {
    
    /**
     * Get all kids linked to a specific parent.
     * 
     * @param parentId The ID of the parent
     * @return List of kids linked to the parent
     */
    @GET("/api/v1/parents/{parent_id}/kids")
    suspend fun getKidsForParent(@Path("parent_id") parentId: String): List<Kid>
}
