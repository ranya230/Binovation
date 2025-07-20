package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import fr.isen.amara.isensmartcompanion.R

@Composable
fun WelcomeDashboardScreen(navController: NavController) {
    Surface(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceAround,
            modifier = Modifier.fillMaxSize()
        ) {

            // Titre
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Bienvenue sur Binovation",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Connecté à votre bac intelligent",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }

            // Logo central (remplace R.drawable.wifi_logo par ton image réelle)
            Image(
                painter = painterResource(id = R.drawable.wifi_logo),
                contentDescription = "Wi-Fi logo",
                modifier = Modifier.size(160.dp)
            )

            // Bouton principal
            Button(
                onClick = {
                    navController.navigate("binState") // remplace par ton nom d’écran réel
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(55.dp)
            ) {
                Text("Lancer le Scan", fontSize = 18.sp)
            }

            // Autres options
            Row(
                horizontalArrangement = Arrangement.SpaceEvenly,
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = { navController.navigate("binState") }) {
                    Text("📶 Niveau du bac")
                }
                OutlinedButton(onClick = { navController.navigate("history") }) {
                    Text("📂 Historique")
                }
            }
        }
    }
}
