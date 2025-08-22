package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import fr.isen.amara.isensmartcompanion.R
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(navController: NavController? = null) {
    val context = LocalContext.current
    val prefs = context.getSharedPreferences("user_settings", Context.MODE_PRIVATE)
    val auth = FirebaseAuth.getInstance()
    val user = auth.currentUser

    // 🔄 Charger AppSettings (pour la hauteur)
    LaunchedEffect(Unit) { AppSettings.init(context) }
    val maxMm by AppSettings.maxDistanceMmFlow.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var username by remember { mutableStateOf(prefs.getString("username", "User") ?: "User") }
    var newPassword by remember { mutableStateOf("") }
    var notificationsEnabled by remember { mutableStateOf(prefs.getBoolean("notifications", true)) }
    var profileUri by remember { mutableStateOf<Uri?>(null) }
    var passwordVisible by remember { mutableStateOf(false) }

    // Dialog pour éditer la hauteur
    var showHeightDialog by remember { mutableStateOf(false) }

    // Charger avatar si déjà sauvegardé
    LaunchedEffect(Unit) {
        prefs.getString("profileUri", null)?.let { profileUri = Uri.parse(it) }
    }

    // Picker image
    val pickImageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            profileUri = it
            prefs.edit().putString("profileUri", it.toString()).apply()
            scope.launch { snackbarHostState.showSnackbar("Profile photo updated") }
        }
    }

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Titre
            item {
                Text("Settings", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            // Profil + Username affiché + photo + email
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(text = username, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                        Image(
                            painter = if (profileUri != null)
                                rememberAsyncImagePainter(profileUri)
                            else
                                painterResource(id = R.drawable.ic_default_avatar),
                            contentDescription = "Profile Picture",
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .clickable { pickImageLauncher.launch("image/*") },
                            contentScale = ContentScale.Crop
                        )

                        OutlinedTextField(
                            value = user?.email ?: "No email",
                            onValueChange = {},
                            label = { Text("Email") },
                            leadingIcon = { Icon(Icons.Default.Email, null) },
                            enabled = false,
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // 🔧 Carte: Hauteur de poubelle (édition ici aussi)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Bin height", fontWeight = FontWeight.SemiBold)
                        Text(
                            text = if (maxMm > 0f) "${maxMm.toInt()} mm" else "Not set",
                            fontSize = 16.sp
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(onClick = { showHeightDialog = true }) {
                                Text(if (maxMm > 0f) "Edit" else "Set")
                            }
                        }
                    }
                }
            }

            // Username modifiable
            item {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Change Username") },
                    leadingIcon = { Icon(Icons.Default.Person, null) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Notifications
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Notifications, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Enable notifications")
                        }
                        Switch(
                            checked = notificationsEnabled,
                            onCheckedChange = { notificationsEnabled = it }
                        )
                    }
                }
            }

            // Changement de mot de passe
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = newPassword,
                            onValueChange = { newPassword = it },
                            label = { Text("New password") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            singleLine = true,
                            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (passwordVisible) "Hide password" else "Show password"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (newPassword.isNotEmpty()) {
                                    auth.currentUser?.updatePassword(newPassword)
                                        ?.addOnCompleteListener { task ->
                                            if (task.isSuccessful) {
                                                Toast.makeText(context, "Password updated", Toast.LENGTH_SHORT).show()
                                                newPassword = ""
                                            } else {
                                                Toast.makeText(
                                                    context,
                                                    "Error: ${task.exception?.localizedMessage}",
                                                    Toast.LENGTH_SHORT
                                                ).show()
                                            }
                                        }
                                } else {
                                    Toast.makeText(context, "Enter a new password", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) { Text("Change password") }
                    }
                }
            }

            // Save
            item {
                Button(
                    onClick = {
                        prefs.edit()
                            .putString("username", username)
                            .putBoolean("notifications", notificationsEnabled)
                            .apply()
                        scope.launch { snackbarHostState.showSnackbar("Settings saved") }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Save changes") }
            }

            // Logout
            item {
                Button(
                    onClick = {
                        auth.signOut()
                        navController?.navigate("login") {
                            popUpTo("settings") { inclusive = true }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Logout") }
            }

            // Logos
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.isen),
                        contentDescription = "ISEN logo",
                        modifier = Modifier.size(70.dp)
                    )
                    Image(
                        painter = painterResource(id = R.drawable.binovation_logo),
                        contentDescription = "Binovation logo",
                        modifier = Modifier.size(70.dp)
                    )
                }
            }
        }
    }

    // --- Dialog édition hauteur (mm/cm/m), identique à la logique du gate ---
    if (showHeightDialog) {
        HeightEditDialog(
            initialMm = maxMm.takeIf { it > 0f },
            onDismiss = { showHeightDialog = false },
            onApply = { mm ->
                AppSettings.setMaxDistanceMm(context, mm)
                showHeightDialog = false
            }
        )
    }
}

/* ------------------------------------------------------------------ */
/* Dialog réutilisable pour saisir la hauteur en mm / cm / m          */
/* S’aligne avec UnitChoice (définie publique dans AppGate.kt)        */
/* ------------------------------------------------------------------ */

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HeightEditDialog(
    initialMm: Float?,
    onDismiss: () -> Unit,
    onApply: (Float) -> Unit
) {
    var unit by remember { mutableStateOf(UnitChoice.MM) }
    var valueText by remember {
        mutableStateOf(
            initialMm?.takeIf { it > 0f }?.toInt()?.toString() ?: ""
        )
    }

    val parsedMm = remember(valueText, unit) {
        val n = valueText.trim().replace(',', '.').toFloatOrNull()
        val mm = when (unit) {
            UnitChoice.MM -> n
            UnitChoice.CM -> n?.times(10f)
            UnitChoice.M  -> n?.times(1000f)
        }
        mm?.takeIf { it > 0f }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set bin height") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = valueText,
                    onValueChange = { valueText = it },
                    label = {
                        Text(
                            when (unit) {
                                UnitChoice.MM -> "Height (mm)"
                                UnitChoice.CM -> "Height (cm)"
                                UnitChoice.M  -> "Height (m)"
                            }
                        )
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    FilterChip(
                        selected = unit == UnitChoice.MM,
                        onClick = { unit = UnitChoice.MM },
                        label = { Text("mm") }
                    )
                    FilterChip(
                        selected = unit == UnitChoice.CM,
                        onClick = { unit = UnitChoice.CM },
                        label = { Text("cm") }
                    )
                    FilterChip(
                        selected = unit == UnitChoice.M,
                        onClick = { unit = UnitChoice.M },
                        label = { Text("m") }
                    )
                }
                if (initialMm == null || initialMm <= 0f) {
                    Text(
                        "Required to unlock bin screens.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { parsedMm?.let(onApply) },
                enabled = parsedMm != null
            ) { Text("Apply") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
