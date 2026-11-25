package io.github.demiterhugo.propertyhandover.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import io.github.demiterhugo.propertyhandover.domain.EntryType

internal val BrandColor = Color(0xFF31566C)

internal val LightColorScheme = lightColorScheme(
    primary = BrandColor,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD9EAF3),
    onPrimaryContainer = Color(0xFF1E455A),
    secondary = Color(0xFF486373),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE7F1F6),
    onSecondaryContainer = Color(0xFF263F4E),
    background = Color(0xFFF6F9FB),
    onBackground = Color(0xFF192C36),
    surface = Color(0xFFF6F9FB),
    onSurface = Color(0xFF192C36),
    surfaceVariant = Color(0xFFEAF1F5),
    onSurfaceVariant = Color(0xFF475D6B),
    surfaceContainerLow = Color.White,
    outline = Color(0xFF728793),
    outlineVariant = Color(0xFFD2DFE6),
    error = Color(0xFFA1291B),
)

internal val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9CCE0),
    onPrimary = Color(0xFF153B50),
    primaryContainer = Color(0xFF24485B),
    onPrimaryContainer = Color(0xFFD0E8F6),
    secondary = Color(0xFFAAC4D3),
    onSecondary = Color(0xFF233B48),
    secondaryContainer = Color(0xFF293E4A),
    onSecondaryContainer = Color(0xFFD5E7F2),
    background = Color(0xFF141F27),
    onBackground = Color(0xFFE6EEF2),
    surface = Color(0xFF141F27),
    onSurface = Color(0xFFE6EEF2),
    surfaceVariant = Color(0xFF2A3B45),
    onSurfaceVariant = Color(0xFFBFCFD8),
    surfaceContainerLow = Color(0xFF1B2A34),
    outline = Color(0xFF8DA4B2),
    outlineVariant = Color(0xFF455B67),
    error = Color(0xFFFFB4A7),
)

internal fun ColorScheme.forEntry(type: EntryType): ColorScheme {
    val dark = surface.luminance() < 0.5f
    return when (type) {
        EntryType.DAMAGE -> if (dark) {
            withEntryAccent(Color(0xFFF1BC9C), Color(0xFF462B1B), Color(0xFF483326), Color(0xFFFFDAC5))
        } else {
            withEntryAccent(Color(0xFF855135), Color.White, Color(0xFFFAEEE4), Color(0xFF663A24))
        }
        EntryType.METER -> if (dark) {
            withEntryAccent(Color(0xFF9BD7CC), Color(0xFF123C37), Color(0xFF243F3A), Color(0xFFBDECE4))
        } else {
            withEntryAccent(Color(0xFF28665E), Color.White, Color(0xFFE5F2EF), Color(0xFF1D514A))
        }
    }
}

private fun ColorScheme.withEntryAccent(
    accent: Color,
    onAccent: Color,
    container: Color,
    onContainer: Color,
): ColorScheme = copy(
    primary = accent,
    onPrimary = onAccent,
    primaryContainer = container,
    onPrimaryContainer = onContainer,
    secondaryContainer = container,
    onSecondaryContainer = onContainer,
)
