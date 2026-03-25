package com.example.kmppoc

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonClassDiscriminator

@OptIn(ExperimentalSerializationApi::class)
val ComponentJson = Json {
    classDiscriminator = "componentType"
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("eventType")
sealed class UiEvent {
    @Serializable
    @SerialName("deeplink")
    data class Deeplink(val url: String) : UiEvent()

    @Serializable
    @SerialName("analytics")
    data class Analytics(val eventName: String, val params: Map<String, String> = emptyMap()) : UiEvent()
}

@OptIn(ExperimentalSerializationApi::class)
@Serializable
@JsonClassDiscriminator("componentType")
sealed class UiComponent {
    abstract val id: String
    abstract val events: List<UiEvent>
    abstract val children: List<UiComponent>

    // ── Complex components ──

    @Serializable
    @SerialName("UserProfile")
    data class UserProfile(
        override val id: String,
        val name: String,
        val role: String,
        val email: String,
        val initials: String,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("Statistics")
    data class Statistics(
        override val id: String,
        val items: List<StatItem>,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent() {
        @Serializable
        data class StatItem(val label: String, val progress: Float, val value: String)
    }

    @Serializable
    @SerialName("Settings")
    data class Settings(
        override val id: String,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("ColorPalette")
    data class ColorPalette(
        override val id: String,
        val colors: List<String>,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("Weather")
    data class Weather(
        override val id: String,
        val city: String,
        val condition: String,
        val temp: String,
        val humidity: String,
        val wind: String,
        val uvIndex: String,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("Quote")
    data class Quote(
        override val id: String,
        val text: String,
        val author: String,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("TaskProgress")
    data class TaskProgress(
        override val id: String,
        val tasks: List<TaskItem>,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent() {
        @Serializable
        data class TaskItem(val name: String, val done: Boolean)
    }

    @Serializable
    @SerialName("ChipGroup")
    data class ChipGroup(
        override val id: String,
        val tags: List<String>,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    // ── Simple visual components ──

    @Serializable
    @SerialName("Label")
    data class Label(
        override val id: String,
        val text: String,
        val style: String = "body",
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("UiButton")
    data class UiButton(
        override val id: String,
        val label: String,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("UiCheckbox")
    data class UiCheckbox(
        override val id: String,
        val label: String,
        val checked: Boolean = false,
        override val events: List<UiEvent> = emptyList(),
        override val children: List<UiComponent> = emptyList(),
    ) : UiComponent()
}

val defaultComponents: List<UiComponent> = listOf(
    UiComponent.UserProfile(
        id = "up-001",
        name = "John Doe",
        role = "Senior Developer",
        email = "john.doe@example.com",
        initials = "JD",
        events = listOf(
            UiEvent.Analytics("profile_viewed", mapOf("user_id" to "up-001")),
        ),
        children = listOf(
            UiComponent.Label(id = "up-001-name", text = "John Doe", style = "title"),
            UiComponent.Label(id = "up-001-role", text = "Senior Developer", style = "subtitle"),
            UiComponent.Label(
                id = "up-001-email",
                text = "john.doe@example.com",
                events = listOf(UiEvent.Deeplink("mailto:john.doe@example.com")),
            ),
        ),
    ),
    UiComponent.Statistics(
        id = "st-001",
        items = listOf(
            UiComponent.Statistics.StatItem("Projects Completed", 0.85f, "85%"),
            UiComponent.Statistics.StatItem("Tasks Done", 0.62f, "62%"),
            UiComponent.Statistics.StatItem("Code Coverage", 0.91f, "91%"),
        ),
        events = listOf(
            UiEvent.Analytics("statistics_impression", mapOf("section" to "dashboard")),
        ),
        children = listOf(
            UiComponent.Label(id = "st-001-title", text = "Statistics", style = "title"),
            UiComponent.UiButton(
                id = "st-001-details",
                label = "View Details",
                events = listOf(UiEvent.Deeplink("app://stats/details")),
            ),
        ),
    ),
    UiComponent.Settings(
        id = "se-001",
        children = listOf(
            UiComponent.Label(id = "se-001-title", text = "Settings", style = "title"),
            UiComponent.UiCheckbox(
                id = "se-001-dark",
                label = "Dark Mode",
                checked = false,
                events = listOf(
                    UiEvent.Analytics("setting_toggled", mapOf("setting" to "dark_mode")),
                ),
            ),
            UiComponent.UiCheckbox(id = "se-001-notif", label = "Notifications", checked = true),
            UiComponent.UiCheckbox(
                id = "se-001-analytics",
                label = "Analytics",
                checked = false,
                events = listOf(
                    UiEvent.Analytics("setting_toggled", mapOf("setting" to "analytics_opt_in")),
                ),
            ),
        ),
    ),
    UiComponent.ColorPalette(
        id = "cp-001",
        colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#4DD0E1", "#FF8A65", "#A1887F"),
        children = listOf(
            UiComponent.Label(id = "cp-001-title", text = "Color Palette", style = "title"),
        ),
    ),
    UiComponent.Weather(
        id = "we-001",
        city = "San Francisco",
        condition = "Partly Cloudy",
        temp = "22°C",
        humidity = "65%",
        wind = "12 km/h",
        uvIndex = "3",
        events = listOf(
            UiEvent.Analytics("weather_viewed", mapOf("city" to "San Francisco")),
        ),
        children = listOf(
            UiComponent.Label(id = "we-001-city", text = "San Francisco", style = "title"),
            UiComponent.Label(id = "we-001-temp", text = "22°C", style = "headline"),
            UiComponent.UiButton(
                id = "we-001-forecast",
                label = "Full Forecast",
                events = listOf(UiEvent.Deeplink("app://weather/forecast/san-francisco")),
            ),
        ),
    ),
    UiComponent.Quote(
        id = "qu-001",
        text = "The best way to predict the future is to invent it.",
        author = "Alan Kay",
        children = listOf(
            UiComponent.Label(id = "qu-001-text", text = "The best way to predict the future is to invent it.", style = "quote"),
            UiComponent.Label(id = "qu-001-author", text = "Alan Kay", style = "caption"),
            UiComponent.UiButton(
                id = "qu-001-share",
                label = "Share",
                events = listOf(
                    UiEvent.Deeplink("app://share?text=The+best+way+to+predict+the+future+is+to+invent+it."),
                    UiEvent.Analytics("quote_shared", mapOf("author" to "Alan Kay")),
                ),
            ),
        ),
    ),
    UiComponent.TaskProgress(
        id = "tp-001",
        tasks = listOf(
            UiComponent.TaskProgress.TaskItem("Design System Update", true),
            UiComponent.TaskProgress.TaskItem("API Integration", true),
            UiComponent.TaskProgress.TaskItem("Unit Tests", false),
            UiComponent.TaskProgress.TaskItem("Documentation", false),
            UiComponent.TaskProgress.TaskItem("Code Review", false),
        ),
        events = listOf(
            UiEvent.Analytics("tasks_impression", mapOf("completed" to "2", "total" to "5")),
        ),
        children = listOf(
            UiComponent.Label(id = "tp-001-title", text = "Sprint Tasks", style = "title"),
            UiComponent.UiCheckbox(
                id = "tp-001-task1",
                label = "Design System Update",
                checked = true,
                events = listOf(UiEvent.Analytics("task_toggled", mapOf("task" to "Design System Update", "done" to "true"))),
            ),
            UiComponent.UiCheckbox(id = "tp-001-task2", label = "API Integration", checked = true),
            UiComponent.UiCheckbox(id = "tp-001-task3", label = "Unit Tests", checked = false),
            UiComponent.UiCheckbox(
                id = "tp-001-task4",
                label = "Documentation",
                checked = false,
                events = listOf(UiEvent.Deeplink("app://docs/sprint-42")),
            ),
            UiComponent.UiCheckbox(id = "tp-001-task5", label = "Code Review", checked = false),
        ),
    ),
    UiComponent.ChipGroup(
        id = "cg-001",
        tags = listOf("Kotlin", "Compose", "Multiplatform", "Android", "iOS", "Web", "Ktor", "Gradle"),
        children = listOf(
            UiComponent.Label(id = "cg-001-title", text = "Technologies", style = "title"),
            UiComponent.UiButton(
                id = "cg-001-kotlin",
                label = "Kotlin",
                events = listOf(UiEvent.Deeplink("https://kotlinlang.org")),
            ),
            UiComponent.UiButton(
                id = "cg-001-compose",
                label = "Compose",
                events = listOf(
                    UiEvent.Deeplink("https://developer.android.com/jetpack/compose"),
                    UiEvent.Analytics("chip_clicked", mapOf("chip" to "Compose")),
                ),
            ),
            UiComponent.UiButton(id = "cg-001-mp", label = "Multiplatform"),
            UiComponent.UiButton(id = "cg-001-android", label = "Android"),
        ),
    ),
)
