package fi.kidozz.app.ui.theme

import androidx.compose.material3.Typography

/**
 * Material3 Typography using our DesignTypography system
 * Maps Material3 text styles to our custom typography
 */
val Typography = Typography(
    // Headlines
    headlineLarge = DesignTypography.Title,
    headlineMedium = DesignTypography.Subtitle,
    headlineSmall = DesignTypography.Heading,
    
    // Titles
    titleLarge = DesignTypography.Title,
    titleMedium = DesignTypography.Subtitle,
    titleSmall = DesignTypography.Heading,
    
    // Body text
    bodyLarge = DesignTypography.Body,
    bodyMedium = DesignTypography.BodyMedium,
    bodySmall = DesignTypography.BodySmall,
    
    // Labels
    labelLarge = DesignTypography.ButtonLarge,
    labelMedium = DesignTypography.Button,
    labelSmall = DesignTypography.Label,
    
    // Display (for very large text)
    displayLarge = DesignTypography.Title,
    displayMedium = DesignTypography.Subtitle,
    displaySmall = DesignTypography.Heading
)