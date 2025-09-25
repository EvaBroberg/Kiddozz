package fi.kidozz.app.ui.styles

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import fi.kidozz.app.R

/**
 * Contains reusable text styles that extend MaterialTheme.typography for consistency.
 * These provide semantic text styles used across the application.
 */

// Google Fonts configuration
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// App font family using Google Sans with Roboto fallback
@Composable
fun AppFontFamily(): FontFamily = FontFamily(
    Font(
        googleFont = GoogleFont("Google Sans"),
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    )
)

object AppTypography {
    // Typography using Google Sans with Roboto fallback
    @Composable
    fun Typography(): Typography = Typography(
        // Headlines - using AppFontFamily with bold/medium weights
        headlineLarge = MaterialTheme.typography.headlineLarge.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Bold
        ),
        headlineMedium = MaterialTheme.typography.headlineMedium.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Bold
        ),
        headlineSmall = MaterialTheme.typography.headlineSmall.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Medium
        ),
        
        // Body text - using AppFontFamily with normal weight
        bodyLarge = MaterialTheme.typography.bodyLarge.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal
        ),
        bodyMedium = MaterialTheme.typography.bodyMedium.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal
        ),
        bodySmall = MaterialTheme.typography.bodySmall.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Normal
        ),
        
        // Labels - using AppFontFamily
        labelLarge = MaterialTheme.typography.labelLarge.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Medium
        ),
        labelSmall = MaterialTheme.typography.labelSmall.copy(
            fontFamily = AppFontFamily(),
            fontWeight = FontWeight.Medium
        )
    )

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
