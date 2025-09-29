package fi.kidozz.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.platform.LocalTextToolbar
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * A custom text toolbar provider that shows an anchored toolbar above text fields
 * instead of the default floating toolbar.
 */
@Composable
fun CustomTextToolbarProvider(
    textFieldValue: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    content: @Composable () -> Unit
) {
    val defaultTextToolbar = LocalTextToolbar.current
    
    val customTextToolbar = object : androidx.compose.ui.platform.TextToolbar {
        override fun showMenu(
            rect: Rect,
            onCopyRequested: (() -> Unit)?,
            onPasteRequested: (() -> Unit)?,
            onCutRequested: (() -> Unit)?,
            onSelectAllRequested: (() -> Unit)?
        ) {
            // We'll handle this in our custom toolbar
        }
        
        override fun hide() {
            // Hide our custom toolbar
        }
        
        override val status: androidx.compose.ui.platform.TextToolbarStatus
            get() = androidx.compose.ui.platform.TextToolbarStatus.Hidden
    }
    
    CompositionLocalProvider(
        LocalTextToolbar provides customTextToolbar
    ) {
        Box {
            content()
            
            // Show our custom anchored toolbar
            AnchoredTextToolbar(
                textFieldValue = textFieldValue,
                onValueChange = onValueChange
            )
        }
    }
}
