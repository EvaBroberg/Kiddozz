package fi.kidozz.app.ui.styles

import androidx.compose.ui.graphics.Color
import fi.kidozz.app.ui.theme.Purple40
import fi.kidozz.app.ui.theme.Purple80
import fi.kidozz.app.ui.theme.PurpleGrey40
import fi.kidozz.app.ui.theme.PurpleGrey80

/**
 * Contains semantic color definitions that reuse the base colors from ui/theme/Color.kt.
 * These provide meaningful color names for different UI elements and states.
 */

object AppColors {
    // Primary Colors
    val Primary = Purple40
    val PrimaryVariant = Purple80
    val OnPrimary = Color.White
    
    // Secondary Colors
    val Secondary = PurpleGrey40
    val SecondaryVariant = PurpleGrey80
    val OnSecondary = Color.White
    
    // Background Colors
    val PrimaryBackground = Color.White
    val SecondaryBackground = Color(0xFFF5F5F5)
    val Surface = Color.White
    val OnSurface = Color.Black
    
    // Card Colors
    val CardBackground = Color.White
    val CardSurface = Color.White
    val OnCard = Color.Black
    
    // Text Colors
    val PrimaryText = Color.Black
    val SecondaryText = Color(0xFF666666)
    val TertiaryText = Color(0xFF999999)
    val OnBackground = Color.Black
    
    // Status Colors
    val Success = Color(0xFF4CAF50)
    val Warning = Color(0xFFFF9800)
    val Error = Color(0xFFF44336)
    val Info = Color(0xFF2196F3)
    
    // Interactive Colors
    val Link = Color(0xFF1976D2)
    val Disabled = Color(0xFFBDBDBD)
    val Divider = Color(0xFFE0E0E0)
    
    // Kid Card Specific Colors
    val KidCardBackground = Color.White
    val KidCardBorder = Color(0xFFE0E0E0)
    val KidNameText = Color.Black
    val KidDetailsText = Color(0xFF666666)
}
