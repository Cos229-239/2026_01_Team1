package edu.fullsail.anchor.ui.theme

import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme

// Colorblind Accessibility Theme (Placeholders)
// Color schemes used for Deuteranopia, Protanopia, and Tritanopia modes.
// These use the palette values that are defined in Color.kt and integrate
// with the app's existing light and dark modes.
// TODO: Replace placeholder values with real colorblind-safe palettes

// Deuteranopia
val DeuteranopiaLightColorScheme = lightColorScheme(
    primary = DeuteranopiaPrimary,
    secondary = DeuteranopiaSecondary,
    tertiary = DeuteranopiaAccent,
)
val DeuteranopiaDarkColorScheme = darkColorScheme(
    primary = DeuteranopiaPrimary,
    secondary = DeuteranopiaSecondary,
    tertiary = DeuteranopiaAccent
)

// Protanopia
val ProtanopiaLightColorScheme = lightColorScheme(
    primary = ProtanopiaPrimary,
    secondary = ProtanopiaSecondary,
    tertiary = ProtanopiaAccent
)
val ProtanopiaDarkColorScheme = darkColorScheme(
    primary = ProtanopiaPrimary,
    secondary = ProtanopiaSecondary,
    tertiary = ProtanopiaAccent
)

//Tritanopia
val TritanopiaLightColorScheme = lightColorScheme(
    primary = TritanopiaPrimary,
    secondary = TritanopiaSecondary,
    tertiary = TritanopiaAccent
)
val TritanopiaDarkColorScheme = darkColorScheme(
    primary = TritanopiaPrimary,
    secondary = TritanopiaSecondary,
    tertiary = TritanopiaAccent
)