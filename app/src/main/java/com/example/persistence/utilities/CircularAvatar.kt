package com.example.persistence.utilities

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import androidx.compose.runtime.Composable // Don't forget this!
import androidx.compose.foundation.layout.size

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun CircularAvatarWithGif(
    gifResId: Int,
    modifier: Modifier = Modifier,
    size: Dp = 100.dp,
    contentDescription: String? = null
) {
    GlideImage(
        model = gifResId,
        contentDescription = contentDescription,
        modifier = modifier
            .size(size)
            .clip(CircleShape),
        contentScale = ContentScale.Crop
    )
}