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

    @Serializable
    @SerialName("UserProfile")
    data class UserProfile(
        override val id: String,
        val name: String,
        val role: String,
        val email: String,
        val initials: String,
        override val events: List<UiEvent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("Statistics")
    data class Statistics(
        override val id: String,
        val items: List<StatItem>,
        override val events: List<UiEvent> = emptyList(),
    ) : UiComponent() {
        @Serializable
        data class StatItem(val label: String, val progress: Float, val value: String)
    }

    @Serializable
    @SerialName("Settings")
    data class Settings(
        override val id: String,
        override val events: List<UiEvent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("ColorPalette")
    data class ColorPalette(
        override val id: String,
        val colors: List<String>,
        override val events: List<UiEvent> = emptyList(),
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
    ) : UiComponent()

    @Serializable
    @SerialName("Quote")
    data class Quote(
        override val id: String,
        val text: String,
        val author: String,
        override val events: List<UiEvent> = emptyList(),
    ) : UiComponent()

    @Serializable
    @SerialName("TaskProgress")
    data class TaskProgress(
        override val id: String,
        val tasks: List<TaskItem>,
        override val events: List<UiEvent> = emptyList(),
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
            UiEvent.Deeplink("app://profile/up-001"),
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
    ),
    UiComponent.Settings(id = "se-001"),
    UiComponent.ColorPalette(
        id = "cp-001",
        colors = listOf("#E57373", "#81C784", "#64B5F6", "#FFD54F", "#BA68C8", "#4DD0E1", "#FF8A65", "#A1887F"),
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
            UiEvent.Deeplink("app://weather/San Francisco"),
        ),
    ),
    UiComponent.Quote(
        id = "qu-001",
        text = "The best way to predict the future is to invent it.",
        author = "Alan Kay",
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
    ),
    UiComponent.ChipGroup(
        id = "cg-001",
        tags = listOf("Kotlin", "Compose", "Multiplatform", "Android", "iOS", "Web", "Ktor", "Gradle"),
    ),
)
