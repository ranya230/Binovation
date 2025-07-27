package fr.isen.amara.isensmartcompanion.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegisterScreen(navController: NavController) {
    val email = remember { mutableStateOf("") }
    val username = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val confirmPassword = remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Créer un compte") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("login") }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Retour à la connexion")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            OutlinedTextField(
                value = email.value,
                onValueChange = { email.value = it.trim() },
                label = { Text("Email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = username.value,
                onValueChange = { username.value = it },
                label = { Text("Nom d'utilisateur") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = password.value,
                onValueChange = { password.value = it },
                label = { Text("Mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = confirmPassword.value,
                onValueChange = { confirmPassword.value = it },
                label = { Text("Confirmer mot de passe") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    coroutineScope.launch {
                        try {
                            if (email.value.isBlank() || username.value.isBlank() || password.value.isBlank()) {
                                snackbarHostState.showSnackbar(" Remplissez tous les champs")
                                return@launch
                            }
                            if (password.value != confirmPassword.value) {
                                snackbarHostState.showSnackbar(" Les mots de passe ne correspondent pas")
                                return@launch
                            }

                            val auth = FirebaseAuth.getInstance()
                            // Vérifier si l'email est déjà utilisé
                            val signInMethods = auth.fetchSignInMethodsForEmail(email.value).await()
                            if (signInMethods.signInMethods?.isNotEmpty() == true) {
                                snackbarHostState.showSnackbar(" Cet email est déjà utilisé")
                                return@launch
                            }

                            // Sinon créer le compte
                            val result = auth.createUserWithEmailAndPassword(email.value, password.value).await()
                            val userId = result.user?.uid

                            if (userId != null) {
                                // Sauvegarder user dans Firestore si besoin ici
                                snackbarHostState.showSnackbar(" Compte créé avec succès")
                                navController.navigate("login") {
                                    popUpTo("register") { inclusive = true }
                                }
                            }
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar(" Erreur : ${e.localizedMessage ?: "erreur inconnue"}")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Créer un compte")
            }
        }
    }
}
