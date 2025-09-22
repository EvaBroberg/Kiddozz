package fi.kidozz.app.ui.styles

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight

/**
 * Contains reusable text styles that extend MaterialTheme.typography for consistency.
 * These provide semantic text styles used across the application.
 */

object AppTypography {
    // Headers
    @Composable
    fun AppTitle(): TextStyle = MaterialTheme.typography.headlineLarge.copy(
        fontWeight = FontWeight.Bold
    )
    
    @Composable
    fun SectionTitle(): TextStyle = MaterialTheme.typography.titleLarge.copy(
        fontWeight = FontWeight.Bold
    )
    
    @Composable
    fun CardTitle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.SemiBold
    )
    
    // Body Text
    @Composable
    fun BodyLarge(): TextStyle = MaterialTheme.typography.bodyLarge
    
    @Composable
    fun BodyMedium(): TextStyle = MaterialTheme.typography.bodyMedium
    
    @Composable
    fun BodySmall(): TextStyle = MaterialTheme.typography.bodySmall
    
    // Labels
    @Composable
    fun LabelLarge(): TextStyle = MaterialTheme.typography.labelLarge.copy(
        fontWeight = FontWeight.Medium
    )
    
    @Composable
    fun LabelMedium(): TextStyle = MaterialTheme.typography.labelMedium
    
    @Composable
    fun LabelSmall(): TextStyle = MaterialTheme.typography.labelSmall
    
    // Specialized Text
    @Composable
    fun KidName(): TextStyle = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.SemiBold
    )
    
    @Composable
    fun KidDetails(): TextStyle = MaterialTheme.typography.bodyMedium
    
    @Composable
    fun EventTitle(): TextStyle = MaterialTheme.typography.titleMedium.copy(
        fontWeight = FontWeight.Medium
    )
    
    @Composable
    fun EventDescription(): TextStyle = MaterialTheme.typography.bodyMedium
}
