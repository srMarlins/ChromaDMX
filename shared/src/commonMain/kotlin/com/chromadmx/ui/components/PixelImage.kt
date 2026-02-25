package com.chromadmx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.imageResource

/**
 * A wrapper around [Image] that ensures pixel-perfect scaling by using [FilterQuality.None].
 * This uses [imageResource] to load the asset as an [ImageBitmap].
 */
@Composable
fun PixelImage(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        bitmap = imageResource(resource),
        contentDescription = contentDescription,
        modifier = modifier,
        filterQuality = FilterQuality.None,
        contentScale = contentScale
    )
}
