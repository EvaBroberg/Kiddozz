package fi.kidozz.app.data.repository

import fi.kidozz.app.data.api.ParentsApiService
import fi.kidozz.app.data.models.Kid

/**
 * Repository for parent-related data operations.
 * Wraps API calls and provides a clean interface for the ViewModel.
 */
class ParentsRepository(
    private val parentsApiService: ParentsApiService
) {
    
    /**
     * Get all kids linked to a specific parent.
     * 
     * @param parentId The ID of the parent
     * @return List of kids linked to the parent
     */
    suspend fun getKidsForParent(parentId: String): List<Kid> {
        return parentsApiService.getKidsForParent(parentId)
    }
}
