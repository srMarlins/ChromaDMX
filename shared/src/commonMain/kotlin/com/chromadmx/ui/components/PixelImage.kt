package com.chromadmx.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

/**
 * A wrapper around [Image] that ensures pixel-perfect scaling by using [FilterQuality.None].
 */
@Composable
fun PixelImage(
    resource: DrawableResource,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painterResource(resource),
        contentDescription = contentDescription,
        modifier = modifier,
        filterQuality = FilterQuality.None,
        contentScale = contentScale
    )
}

/**
 * A version of [PixelImage] that takes a [Painter] directly.
 */
@Composable
fun PixelImage(
    painter: Painter,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
) {
    Image(
        painter = painter,
        contentDescription = contentDescription,
        modifier = modifier,
        filterQuality = FilterQuality.None,
        contentScale = contentScale
    )
}
