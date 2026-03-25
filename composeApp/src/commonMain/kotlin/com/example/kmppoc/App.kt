package com.example.kmppoc

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.serialization.builtins.ListSerializer

// ── Helper: parse hex color string to Compose Color ──

private fun parseHexColor(hex: String): Color {
    val cleaned = hex.removePrefix("#")
    val argb = when (cleaned.length) {
        6 -> "FF$cleaned"
        8 -> cleaned
        else -> "FFFFFFFF"
    }
    return Color(argb.toLong(16).toInt())
}

// ── Resolve typeName for semantics tag ──

private fun UiComponent.typeName(): String = when (this) {
    is UiComponent.UserProfile -> "UserProfile"
    is UiComponent.Statistics -> "Statistics"
    is UiComponent.Settings -> "Settings"
    is UiComponent.ColorPalette -> "ColorPalette"
    is UiComponent.Weather -> "Weather"
    is UiComponent.Quote -> "Quote"
    is UiComponent.TaskProgress -> "TaskProgress"
    is UiComponent.ChipGroup -> "ChipGroup"
}

// ── Wrapper composable ──

@Composable
fun ComponentCard(component: UiComponent) {
    Box(
        modifier = Modifier.semantics {
            testTag = "Component:${component.typeName()}:${component.id}"
        }
    ) {
        when (component) {
            is UiComponent.UserProfile -> UserProfileCard(component)
            is UiComponent.Statistics -> StatisticsCard(component)
            is UiComponent.Settings -> SettingsCard()
            is UiComponent.ColorPalette -> ColorPaletteCard(component)
            is UiComponent.Weather -> WeatherCard(component)
            is UiComponent.Quote -> QuoteCard(component)
            is UiComponent.TaskProgress -> TaskProgressCard(component)
            is UiComponent.ChipGroup -> ChipGroupCard(component)
        }
    }
}

// ── App ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
    var components by remember { mutableStateOf<List<UiComponent>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val client = HttpClient()
            val response = client.get("http://10.0.2.2:$SERVER_PORT/api/components")
            val json = response.bodyAsText()
            components = ComponentJson.decodeFromString(
                ListSerializer(UiComponent.serializer()),
                json,
            )
            client.close()
        } catch (e: Exception) {
            // Fallback to local data if server is unreachable
            error = e.message
            components = defaultComponents
        } finally {
            loading = false
        }
    }

    MaterialTheme {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("KMP Showcase") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        ) { padding ->
            if (loading) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 12.dp),
                ) {
                    items(components, key = { it.id }) { component ->
                        ComponentCard(component)
                    }
                }
            }
        }
    }
}

// ── Card composables ──

@Composable
private fun UserProfileCard(data: UiComponent.UserProfile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    data.initials,
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(data.name, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(data.role, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                Text(data.email, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun StatisticsCard(data: UiComponent.Statistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Statistics", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            data.items.forEachIndexed { i, item ->
                if (i > 0) Spacer(modifier = Modifier.height(8.dp))
                StatRow(item.label, item.progress, item.value)
            }
        }
    }
}

@Composable
private fun StatRow(label: String, progress: Float, value: String) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(label, fontSize = 14.sp)
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(modifier = Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp)),
        )
    }
}

@Composable
private fun SettingsCard() {
    var darkMode by remember { mutableStateOf(false) }
    var notifications by remember { mutableStateOf(true) }
    var analytics by remember { mutableStateOf(false) }
    var volume by remember { mutableStateOf(0.7f) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Settings", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            SettingSwitch("Dark Mode", darkMode) { darkMode = it }
            SettingSwitch("Notifications", notifications) { notifications = it }
            SettingSwitch("Analytics", analytics) { analytics = it }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            Text("Volume", fontSize = 14.sp)
            Slider(value = volume, onValueChange = { volume = it }, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SettingSwitch(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, fontSize = 14.sp)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ColorPaletteCard(data: UiComponent.ColorPalette) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Color Palette", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                data.colors.forEach { hex ->
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(parseHexColor(hex)))
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(data: UiComponent.Weather) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E88E5)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Weather", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(data.city, fontSize = 16.sp, color = Color.White.copy(alpha = 0.9f))
                    Text(data.condition, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
                }
                Text(data.temp, fontSize = 42.sp, fontWeight = FontWeight.Light, color = Color.White)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                WeatherDetail("Humidity", data.humidity)
                WeatherDetail("Wind", data.wind)
                WeatherDetail("UV Index", data.uvIndex)
            }
        }
    }
}

@Composable
private fun WeatherDetail(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.7f))
    }
}

@Composable
private fun QuoteCard(data: UiComponent.Quote) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "\u201C",
                fontSize = 48.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.3f),
                lineHeight = 48.sp,
            )
            Text(
                data.text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "\u2014 ${data.author}",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun TaskProgressCard(data: UiComponent.TaskProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sprint Tasks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            data.tasks.forEach { task ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = task.done, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task.name, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ChipGroupCard(data: UiComponent.ChipGroup) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Technologies", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.tags.take(4).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag, fontSize = 12.sp) })
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                data.tags.drop(4).forEach { tag ->
                    AssistChip(onClick = {}, label = { Text(tag, fontSize = 12.sp) })
                }
            }
        }
    }
}
