package fi.kidozz.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

@Composable
fun resolveImageModel(image: String): Any {
    val context = LocalContext.current
    return when {
        image.startsWith("http") -> image // URL
        image.startsWith("file://") -> image // Local file path
        image.startsWith("android.resource://") -> image // Android resource URI
        image.startsWith("content://") -> image // Content URI
        else -> { // Assume it's a drawable resource name
            val resourceName = image.substringBeforeLast(".")
            val resId = context.resources.getIdentifier(
                resourceName,
                "drawable",
                context.packageName
            )
            if (resId != 0) resId else image // fallback to string
        }
    }
}
