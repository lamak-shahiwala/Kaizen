package com.example.persistence.screens.authentication

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.example.persistence.R
import com.example.persistence.utilities.CircularAvatarWithGif
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.tasks.Task

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun SignInScreen(onGoogleSignInResult: (Task<GoogleSignInAccount>?) -> Unit) {
    // Get the current Android context
    val context = LocalContext.current

    // Configure Google Sign-in
    val googleSignInOptions = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("1050648804474-l1ae858oc51cuf5abjq7sgf125kdnrnv.apps.googleusercontent.com")
            .requestEmail() // Request email address
            .build()
    }

    // Create a GoogleSignInClient
    val googleSignInClient = remember { GoogleSignIn.getClient(context, googleSignInOptions) }

    // Register the ActivityResultLauncher to handle the Google Sign-In result
    // Using the standard StartActivityForResult contract
    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
        onResult = { result: ActivityResult ->
            // Handle the result from the Google Sign-In Intent
            if (result.resultCode == Activity.RESULT_OK) {
                val intent = result.data
                val task = GoogleSignIn.getSignedInAccountFromIntent(intent)
                onGoogleSignInResult(task)
            } else {
                // Google Sign-In failed or was canceled
                onGoogleSignInResult(null)
            }
        }
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularAvatarWithGif(
            gifResId = R.drawable.goku_transforming,
            contentDescription = "Goku Login Animation",
            size = 150.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text("Kaizen ~ The Saiyan Way", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onTertiary)
        Spacer(modifier = Modifier.height(12.dp))
        Divider(
            modifier = Modifier.fillMaxWidth(),
            thickness = 1.dp
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Kaizen ~ The Saiyan Way is the mindset of constant growth and rising beyond" +
                    " limits. Like a Saiyan, it’s about embracing challenges, learning from " +
                    "failure, and improving every day — no matter how strong you already are!",
            fontWeight = FontWeight.Normal, fontSize = 12.sp, textAlign = TextAlign.Justify,
        )
        Spacer(modifier = Modifier.height(12.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Divider(
                modifier = Modifier.weight(1F),
                thickness = 1.dp
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                "Sign in with",
                color = MaterialTheme.colorScheme.onTertiary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Divider(
                modifier = Modifier.weight(1F),
                thickness = 1.dp
            )
        }
        Spacer(modifier = Modifier.height(24.dp))

        Button(
            modifier = Modifier
                .padding(horizontal = 16.dp),
            onClick = {
                googleSignInClient.signOut().addOnCompleteListener {
                    val signInIntent = googleSignInClient.signInIntent
                    signInLauncher.launch(signInIntent)
                }
            }
            ,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.tertiary
            ),
            border = BorderStroke(1.dp, color = Color.Black),
            shape = RoundedCornerShape(5.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(id = R.drawable.google_icon),
                    contentDescription = "Google Icon",
                    modifier = Modifier.size(32.dp)
                )
                //Spacer(modifier = Modifier.width(width = 10.dp))
                //Text("Sign in with Google")
            }
        }
    }
}