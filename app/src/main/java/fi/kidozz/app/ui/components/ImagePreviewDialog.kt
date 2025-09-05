package fi.kidozz.app.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import fi.kidozz.app.R
import fi.kidozz.app.utils.ImageDownloader
import fi.kidozz.app.utils.resolveImageModel
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagePreviewDialog(
    images: List<String>,
    initialIndex: Int = 0,
    onDismiss: () -> Unit
) {
    var currentIndex by remember { mutableStateOf(initialIndex) }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialIndex)
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isDownloading by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Image ${currentIndex + 1} of ${images.size}")
        },
        text = {
            Column {
                // Main image display
                AsyncImage(
                    model = resolveImageModel(images[currentIndex]),
                    contentDescription = "Preview image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Fit,
                    placeholder = painterResource(R.drawable.ic_gallery),
                    error = painterResource(R.drawable.ic_gallery)
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Thumbnail strip
                if (images.size > 1) {
                    LazyRow(
                        state = listState,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(images.size) { index ->
                            AsyncImage(
                                model = resolveImageModel(images[index]),
                                contentDescription = "Thumbnail ${index + 1}",
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable { currentIndex = index },
                                contentScale = ContentScale.Crop,
                                placeholder = painterResource(R.drawable.ic_gallery),
                                error = painterResource(R.drawable.ic_gallery)
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                Button(
                    onClick = { 
                        if (!isDownloading) {
                            scope.launch {
                                isDownloading = true
                                val result = ImageDownloader.downloadImage(context, images[currentIndex])
                                isDownloading = false
                                
                                result.fold(
                                    onSuccess = { savedPath ->
                                        Toast.makeText(
                                            context, 
                                            "Image saved to gallery!", 
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    },
                                    onFailure = { error ->
                                        Toast.makeText(
                                            context, 
                                            "Failed to download: ${error.message}", 
                                            Toast.LENGTH_LONG
                                        ).show()
                                    }
                                )
                            }
                        }
                    },
                    enabled = !isDownloading
                ) {
                    if (isDownloading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Download,
                            contentDescription = "Download",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isDownloading) "Downloading..." else "Download")
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                
                Button(
                    onClick = onDismiss
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Close")
                }
            }
        }
    )
}
