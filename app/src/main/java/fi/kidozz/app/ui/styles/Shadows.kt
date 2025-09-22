package fi.kidozz.app.ui.styles

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape

/**
 * A soft, bottom-weighted colored shadow for cards.
 * Uses the available shadow API with proper parameters for bottom-heavy effect.
 *
 * @param color The shadow color (default: soft pink #FDDADB).
 * @param shape Shape of the card; keep it in sync with the card's corner radius.
 * @param radius The blur radius of the shadow.
 * @param spread The spread of the shadow.
 * @param offset The offset of the shadow.
 */
fun Modifier.bottomDropShadow(
    elevation: Dp = 12.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    ambientColor: Color = Color(0xFFFDDADB),
    spotColor: Color = Color(0xFFFDDADB),
    clip: Boolean = false
): Modifier = this.then(
    Modifier.shadow(
        elevation = elevation,
        shape = shape,
        ambientColor = ambientColor,
        spotColor = spotColor,
        clip = clip
    )
)