package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "clients")
data class Client(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val websiteUrl: String,
    val healthScore: Int = 80, // Default 80
    val status: String = "Active", // "Active", "Paused", "Review"
    val campaignType: String = "Full SEO", // "Technical", "Content", "Backlinks"
    val createdAt: Long = System.currentTimeMillis(),
    val gscSiteUrl: String? = "",
    val ga4PropertyId: String? = ""
)

@Entity(tableName = "keywords")
data class Keyword(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val phrase: String,
    val searchVolume: Int,
    val currentRank: Int,
    val previousRank: Int,
    val difficulty: Int // 0-100%
) {
    val rankChange: Int
        get() = previousRank - currentRank // Positive is good (e.g. ranked 10, now 5 -> change is +5)
}

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val title: String,
    val description: String,
    val dueDate: String,
    val priority: String = "Medium", // "High", "Medium", "Low"
    val isCompleted: Boolean = false
)

@Entity(tableName = "audits")
data class AuditResult(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val score: Int, // 0-100
    val criticalIssues: Int,
    val warnings: Int,
    val passedChecks: Int,
    val lastUpdated: Long = System.currentTimeMillis(),
    val issuesJson: String, // Stringified issues list
    val strategy: String = "MOBILE",
    val performanceScore: Int = 0,
    val accessibilityScore: Int = 0,
    val bestPracticesScore: Int = 0,
    val seoScore: Int = 0,
    val lcp: String = "",
    val cls: String = "",
    val fcp: String = "",
    val inp: String = "",
    val ttfb: String = "",
    val structuredDataCount: Int = 0,
    val hasRobotsTxt: Boolean = false,
    val sitemapsCount: Int = 0
)

@Entity(tableName = "chat_history")
data class ChatHistory(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val clientId: Int,
    val prompt: String,
    val response: String,
    val timestamp: Long = System.currentTimeMillis()
)
