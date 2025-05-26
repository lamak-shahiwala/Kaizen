package com.example.persistence.screens.home.navigation_drawer

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.persistence.R
import com.example.persistence.utilities.CircularAvatarWithGif


@Composable
fun DrawerHeader(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 44.dp, bottom = 32.dp),
        contentAlignment = Alignment.Center
    ) {
        CircularAvatarWithGif(
            gifResId = R.drawable.goku_chilling,
            size = 144.dp,
            contentDescription = ""
        )
    }
}

@Composable
fun DrawerBody(items: List<MenuItem>,
               modifier: Modifier = Modifier,
               itemTextStyle: TextStyle = TextStyle(fontSize = 18.sp),
               onItemClick: (MenuItem) -> Unit
) {
    LazyColumn (modifier) {
        items(items) {
            item -> Row (
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onItemClick(item)
                    }
                    .padding(16.dp)
            ){
                Icon(contentDescription = "", imageVector = item.icon)
                Spacer(modifier = Modifier.width(10.dp))
                Text(item.title, modifier = Modifier.weight(1f), style = itemTextStyle)
        }
        }
    }
}
