package fr.isen.amara.isensmartcompanion.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import fr.isen.amara.isensmartcompanion.R
import kotlin.math.roundToInt

@Composable
fun BinLevelScreen() {
    val app = LocalContext.current.applicationContext as android.app.Application
    val vm: BinSharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
        factory = androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.getInstance(app)
    )

    LaunchedEffect(Unit) { vm.start() }

    val ui by vm.ui.collectAsState()
    val percent = ui.percent ?: 0f

    // ✅ Couleur/texte d’état selon le pourcentage
    val statusColor = when {
        percent >= 95f -> Color(0xFFC62828) // Rouge
        percent >= 80f -> Color(0xFFFF8F00) // Orange
        percent >= 50f -> Color(0xFFFFD600) // Jaune
        else -> Color(0xFF43A047)          // Vert
    }
    val statusText = if (percent >= 95f) "The bin must be emptied." else ""

    // ✅ UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text(
            text = "Bin Level",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Image(
            painter = painterResource(id = R.drawable.poubelle),
            contentDescription = "Trash Bin",
            modifier = Modifier.size(200.dp)
        )

        Text(
            text = "Fill Level",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "${percent.roundToInt()}%",
                fontSize = 36.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.width(16.dp))
            Surface(
                modifier = Modifier.size(32.dp),
                color = statusColor,
                shape = RoundedCornerShape(6.dp)
            ) {}
        }

        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp),
            color = statusColor
        )

        if (statusText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = statusText,
                    modifier = Modifier.padding(16.dp),
                    color = Color(0xFFC62828),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
