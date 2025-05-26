package com.example.persistence.services.auth_gate

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.persistence.screens.home.HomeScreen
import com.example.persistence.screens.authentication.SignInScreen
import com.example.persistence.screens.settings.ThemeViewModel
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider

import androidx.navigation.NavController
import com.example.persistence.services.database_services.users.UserService.createUserInFirestore
import com.example.persistence.utilities.LoadingScreen
import com.example.persistence.utilities.isInternetAvailable

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AuthGate(themeViewModel: ThemeViewModel, navController: NavController) {
    val context = LocalContext.current
    val authState = rememberFirebaseAuthAuthState()

    var isLoading by remember { mutableStateOf(false) }

    when (val user = authState.value) {
        null -> {
            if (isLoading){
                LoadingScreen()
            } else {
                SignInScreen(
                    onGoogleSignInResult = { task ->
                        if (!isInternetAvailable(context)) {
                            Toast.makeText(context, "No Internet", Toast.LENGTH_LONG).show()
                            isLoading = false
                            return@SignInScreen
                        }
                        try {
                            isLoading = true
                            val account = task?.getResult(ApiException::class.java)
                            val idToken = account?.idToken

                            if (idToken != null) {
                                val credential = GoogleAuthProvider.getCredential(idToken, null)
                                FirebaseAuth.getInstance().signInWithCredential(credential)
                                    .addOnCompleteListener { firebaseAuthTask ->
                                        isLoading = false
                                        if (firebaseAuthTask.isSuccessful) {
                                            createUserInFirestore{ success, message ->
                                                if (success) {
                                                    //Toast.makeText(context, "User saved!", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    Toast.makeText(context, "Failed: $message", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                            Toast.makeText(context, "Signed In!", Toast.LENGTH_SHORT).show()
                                            navController.navigate("home") {
                                                popUpTo("auth_gate") { inclusive = true }
                                            }
                                        } else {
                                            Toast.makeText(context, "Firebase Sign In Failed: ${firebaseAuthTask.exception?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    }
                            } else {
                                isLoading = false
                                //Toast.makeText(context, "Google Sign-In Failed: ID Token Missing", Toast.LENGTH_LONG).show()
                            }
                        } catch (e: ApiException) {
                            isLoading = false
                            Toast.makeText(context, "Google Sign In Failed: ${e.message}", Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            isLoading = false
                            Toast.makeText(context, "Sign In Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                )
            }
        }
        else -> {
            HomeScreen(themeViewModel = themeViewModel, navController = navController)
        }
    }
}



// --- Helper Composable to observe Firebase Auth State ---
// This helper is fine as it already uses core FirebaseAuth
@Composable
fun rememberFirebaseAuthAuthState(): State<FirebaseUser?> {
    val firebaseAuth = FirebaseAuth.getInstance()
    val authState = remember { mutableStateOf(firebaseAuth.currentUser) }

    DisposableEffect(firebaseAuth) {
        val authStateListener = FirebaseAuth.AuthStateListener { auth ->
            authState.value = auth.currentUser
        }
        firebaseAuth.addAuthStateListener(authStateListener)

        onDispose {
            firebaseAuth.removeAuthStateListener(authStateListener)
        }
    }
    return authState
}
