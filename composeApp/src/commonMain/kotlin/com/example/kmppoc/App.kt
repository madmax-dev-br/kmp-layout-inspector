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
import kotlin.random.Random

// ── Random ID generator (UUID-like, KMP-safe) ──

private fun randomId(): String {
    val c = "abcdef0123456789"
    fun seg(n: Int) = buildString { repeat(n) { append(c[Random.nextInt(c.length)]) } }
    return "${seg(8)}-${seg(4)}-${seg(4)}-${seg(4)}-${seg(12)}"
}

// ── Sealed class Component hierarchy ──

sealed class Component {
    abstract val id: String
    abstract val typeName: String

    data class UserProfile(
        override val id: String = randomId(),
        val name: String,
        val role: String,
        val email: String,
        val initials: String,
    ) : Component() {
        override val typeName = "UserProfile"
    }

    data class Statistics(
        override val id: String = randomId(),
        val items: List<StatItem>,
    ) : Component() {
        override val typeName = "Statistics"
        data class StatItem(val label: String, val progress: Float, val value: String)
    }

    data class Settings(
        override val id: String = randomId(),
    ) : Component() {
        override val typeName = "Settings"
    }

    data class ColorPalette(
        override val id: String = randomId(),
        val colors: List<Color>,
    ) : Component() {
        override val typeName = "ColorPalette"
    }

    data class Weather(
        override val id: String = randomId(),
        val city: String,
        val condition: String,
        val temp: String,
        val humidity: String,
        val wind: String,
        val uvIndex: String,
    ) : Component() {
        override val typeName = "Weather"
    }

    data class Quote(
        override val id: String = randomId(),
        val text: String,
        val author: String,
    ) : Component() {
        override val typeName = "Quote"
    }

    data class TaskProgress(
        override val id: String = randomId(),
        val tasks: List<Pair<String, Boolean>>,
    ) : Component() {
        override val typeName = "TaskProgress"
    }

    data class ChipGroup(
        override val id: String = randomId(),
        val tags: List<String>,
    ) : Component() {
        override val typeName = "ChipGroup"
    }
}

// ── Data array ──

val components: List<Component> = listOf(
    Component.UserProfile(
        name = "John Doe",
        role = "Senior Developer",
        email = "john.doe@example.com",
        initials = "JD",
    ),
    Component.Statistics(
        items = listOf(
            Component.Statistics.StatItem("Projects Completed", 0.85f, "85%"),
            Component.Statistics.StatItem("Tasks Done", 0.62f, "62%"),
            Component.Statistics.StatItem("Code Coverage", 0.91f, "91%"),
        ),
    ),
    Component.Settings(),
    Component.ColorPalette(
        colors = listOf(
            Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6),
            Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DD0E1),
            Color(0xFFFF8A65), Color(0xFFA1887F),
        ),
    ),
    Component.Weather(
        city = "San Francisco",
        condition = "Partly Cloudy",
        temp = "22°C",
        humidity = "65%",
        wind = "12 km/h",
        uvIndex = "3",
    ),
    Component.Quote(
        text = "The best way to predict the future is to invent it.",
        author = "Alan Kay",
    ),
    Component.TaskProgress(
        tasks = listOf(
            "Design System Update" to true,
            "API Integration" to true,
            "Unit Tests" to false,
            "Documentation" to false,
            "Code Review" to false,
        ),
    ),
    Component.ChipGroup(
        tags = listOf("Kotlin", "Compose", "Multiplatform", "Android", "iOS", "Web", "Ktor", "Gradle"),
    ),
)

// ── Wrapper composable ──

@Composable
fun ComponentCard(component: Component) {
    Box(
        modifier = Modifier.semantics {
            testTag = "Component:${component.typeName}:${component.id}"
        }
    ) {
        when (component) {
            is Component.UserProfile -> UserProfileCard(component)
            is Component.Statistics -> StatisticsCard(component)
            is Component.Settings -> SettingsCard()
            is Component.ColorPalette -> ColorPaletteCard(component)
            is Component.Weather -> WeatherCard(component)
            is Component.Quote -> QuoteCard(component)
            is Component.TaskProgress -> TaskProgressCard(component)
            is Component.ChipGroup -> ChipGroupCard(component)
        }
    }
}

// ── App ──

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun App() {
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

// ── Card composables ──

@Composable
private fun UserProfileCard(data: Component.UserProfile) {
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
private fun StatisticsCard(data: Component.Statistics) {
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
private fun ColorPaletteCard(data: Component.ColorPalette) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Color Palette", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(12.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                data.colors.forEach { color ->
                    Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(color))
                }
            }
        }
    }
}

@Composable
private fun WeatherCard(data: Component.Weather) {
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
private fun QuoteCard(data: Component.Quote) {
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
private fun TaskProgressCard(data: Component.TaskProgress) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Sprint Tasks", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Spacer(modifier = Modifier.height(8.dp))
            data.tasks.forEach { (task, done) ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(checked = done, onCheckedChange = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(task, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
private fun ChipGroupCard(data: Component.ChipGroup) {
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
