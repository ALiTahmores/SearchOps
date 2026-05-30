package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// ==================== SAAS ENTERPRISE COLOR PALETTE ====================
val Primary = Color(0xFF1F4B99)
val Secondary = Color(0xFF365FB5)
val Accent = Color(0xFF5B7FD9)

val Success = Color(0xFF16A34A)
val Warning = Color(0xFFD97706)
val Error = Color(0xFFDC2626)

// Light Theme Tokens
val BackgroundLight = Color(0xFFF8FAFC)
val CardBackgroundLight = Color(0xFFFFFFFF)
val TextPrimaryLight = Color(0xFF0F172A)
val TextSecondaryLight = Color(0xFF64748B)
val BorderLight = Color(0xFFE2E8F0)

// Dark Theme Tokens
val BackgroundDark = Color(0xFF0F172A)
val CardBackgroundDark = Color(0xFF1E293B)
val TextPrimaryDark = Color(0xFFF8FAFC)
val TextSecondaryDark = Color(0xFF94A3B8)
val BorderDark = Color(0xFF334155)

// Backward compatibility or legacy colors mapped to the new system cleanly
val PrimaryDark = Primary
val OnPrimaryDark = Color.White
val CardSurface = CardBackgroundDark
val BorderColor = BorderDark
val TextPrimary = TextPrimaryDark
val TextSecondary = TextSecondaryDark
val CoralRed = Error
val AccentGreen = Success
val SoftYellow = Warning
val DeepPrimary = Accent
