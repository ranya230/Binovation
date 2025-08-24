// BinLevelScreen.kt
package fr.isen.amara.isensmartcompanion.screens

import androidx.activity.ComponentActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import fr.isen.amara.isensmartcompanion.R
import kotlin.math.roundToInt

@Composable
fun BinLevelScreen() {
    val activity = LocalContext.current as ComponentActivity
    val vm: BinSharedViewModel = viewModel(
        activity,
        factory = ViewModelProvider.AndroidViewModelFactory.getInstance(activity.application)
    )

    val ui by vm.ui.collectAsState()
    val percent = ui.percent ?: 0f

    val statusColor = when {
        percent >= 95f -> Color(0xFFC62828)
        percent >= 80f -> Color(0xFFFF8F00)
        percent >= 50f -> Color(0xFFFFD600)
        else -> Color(0xFF43A047)
    }
    val statusText = if (percent >= 95f) "The bin must be emptied." else ""

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("Bin Level", style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(bottom = 8.dp))

        Image(painter = painterResource(id = R.drawable.poubelle), contentDescription = "Trash Bin", modifier = Modifier.size(200.dp))

        Text("Fill Level", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            Text("${percent.roundToInt()}%", fontSize = 36.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(16.dp))
            Surface(modifier = Modifier.size(32.dp), color = statusColor, shape = RoundedCornerShape(6.dp)) {}
        }

        LinearProgressIndicator(
            progress = percent / 100f,
            modifier = Modifier.fillMaxWidth().height(8.dp),
            color = statusColor
        )

        if (statusText.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFFEBEE)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(statusText, modifier = Modifier.padding(16.dp), color = Color(0xFFC62828), fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}
