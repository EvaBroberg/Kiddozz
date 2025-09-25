package fi.kidozz.app.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.googlefonts.Font
import androidx.compose.ui.text.googlefonts.GoogleFont
import fi.kidozz.app.R

/**
 * Typography system for the Kiddozz app
 * Using Google Fonts for consistent, modern typography
 */

// Google Fonts configuration
val fontProvider = GoogleFont.Provider(
    providerAuthority = "com.google.android.gms.fonts",
    providerPackage = "com.google.android.gms",
    certificates = R.array.com_google_android_gms_fonts_certs
)

// Font families using Google Fonts
val TitleFontFamily = GoogleFont("Poppins")
val BodyFontFamily = GoogleFont("Inter")

@Composable
fun TitleFont(): FontFamily = FontFamily(
    Font(
        googleFont = TitleFontFamily,
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    )
)

@Composable
fun BodyFont(): FontFamily = FontFamily(
    Font(
        googleFont = BodyFontFamily,
        fontProvider = fontProvider,
        weight = FontWeight.Normal
    )
)

/**
 * Design Typography System for Kiddozz
 * Provides consistent text styles across the app using Google Fonts
 */
object DesignTypography {
    // Headings - Using Poppins for friendly, approachable titles
    @Composable
    fun Title(): TextStyle = TextStyle(
        fontFamily = TitleFont(),
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun Subtitle(): TextStyle = TextStyle(
        fontFamily = TitleFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun Heading(): TextStyle = TextStyle(
        fontFamily = TitleFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.sp
    )
    
    // Body text - Using Inter for clean, readable content
    @Composable
    fun Body(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun BodyMedium(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun BodySmall(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun BodySmallMedium(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    // Labels and captions
    @Composable
    fun Label(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun Caption(): TextStyle = TextStyle(
        fontFamily = BodyFont(),
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    )
    
    // Button text
    @Composable
    fun Button(): TextStyle = TextStyle(
        fontFamily = TitleFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.sp
    )
    
    @Composable
    fun ButtonLarge(): TextStyle = TextStyle(
        fontFamily = TitleFont(),
        fontWeight = FontWeight.Medium,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.sp
    )
}
