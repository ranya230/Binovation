package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.tasks.await
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onForgotPassword: () -> Unit = {} // tu peux rediriger vers un autre écran si besoin
) {
    val email = remember { mutableStateOf("") }
    val password = remember { mutableStateOf("") }
    val errorMessage = remember { mutableStateOf<String?>(null) }
    val isLoginMode = remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ✅ Logo
        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Logo",
            modifier = Modifier
                .size(120.dp)
                .padding(bottom = 24.dp)
        )

        Text(
            text = if (isLoginMode.value) "Connexion" else "Créer un compte",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 📧 Email
        OutlinedTextField(
            value = email.value,
            onValueChange = { email.value = it },
            label = { Text("Email") },
            leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 🔒 Mot de passe
        OutlinedTextField(
            value = password.value,
            onValueChange = { password.value = it },
            label = { Text("Mot de passe") },
            leadingIcon = { Icon(Icons.Filled.Lock, contentDescription = null) },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        TextButton(onClick = onForgotPassword, modifier = Modifier.align(Alignment.End)) {
            Text("Mot de passe oublié ?")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ✅ Bouton Connexion / Inscription
        Button(
            onClick = {
                coroutineScope.launch {
                    val auth = FirebaseAuth.getInstance()
                    try {
                        if (isLoginMode.value) {
                            auth.signInWithEmailAndPassword(email.value, password.value).await()
                        } else {
                            auth.createUserWithEmailAndPassword(email.value, password.value).await()
                        }
                        onLoginSuccess()
                    } catch (e: Exception) {
                        errorMessage.value = e.localizedMessage ?: "Erreur inconnue"
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (isLoginMode.value) "Se connecter" else "Créer un compte")
        }

        // ❗ Erreur affichée
        errorMessage.value?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 🔁 Lien bascule login / register
        TextButton(onClick = {
            isLoginMode.value = !isLoginMode.value
            errorMessage.value = null
        }) {
            Text(
                if (isLoginMode.value)
                    "Pas de compte ? Créer un compte"
                else
                    "Déjà un compte ? Se connecter"
            )
        }
    }
}
