package com.example.kmppoc

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform