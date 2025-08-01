package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun ResetPasswordScreen(
    onNavigateBackToLogin: () -> Unit
) {
    val email = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(32.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Réinitialisation du mot de passe", style = MaterialTheme.typography.headlineSmall)

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it },
                label = { Text("Email") },
                leadingIcon = { Icon(Icons.Default.Email, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    scope.launch {
                        try {
                            FirebaseAuth.getInstance().sendPasswordResetEmail(email.value.trim()).await()
                            snackbarHostState.showSnackbar("Email de réinitialisation envoyé.")
                            onNavigateBackToLogin()
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Erreur : ${e.localizedMessage}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Envoyer l’email")
            }

            TextButton(onClick = onNavigateBackToLogin) {
                Text("Retour")
            }
        }
    }
}
