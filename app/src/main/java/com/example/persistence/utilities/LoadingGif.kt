package com.example.persistence.utilities

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.persistence.R // Replace with your actual R class

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column {
            GlideImage(
                model = R.drawable.goku_loading1,
                contentDescription = "Loading...",
                modifier = Modifier
                    .size(64.dp)
            )
            Text("loading...")
        }
    }
}
