package app.vela.ui.theme

import androidx.compose.ui.graphics.Color

// The MAP-RMG brand. The app was built on a map-teal accent; the names below are what the rest of
// the app has always referred to (VelaTeal and friends), now carrying the royal-blue brand, so the
// palette change reaches every screen that already read a theme token instead of a literal.
//
//   royal blue #2457D6  the primary accent: buttons, selected rows, fields, links, the hub glyphs
//   deep blue  #0B1C3D  the brand's dark blue: splash, dark-mode accents, text on gold
//   gold       #D4AF37  the secondary accent: route and traffic highlights, the last onboarding page

/** Primary: MAP-RMG royal blue (#2457D6). */
val VelaTeal = Color(0xFF2457D6)

/** The pressed/darker royal blue (#1A44B0), for dark-mode primary containers and headings. */
val VelaTealDark = Color(0xFF1A44B0)

/** The light royal blue (#6595F6) the dark theme uses as its primary, where #2457D6 is too dim. */
val VelaTealLight = Color(0xFF6595F6)

/** Brand gold (#D4AF37). */
val VelaAmber = Color(0xFFD4AF37) // route / traffic accents

/** The blue a drawn route uses on the map. Kept distinct from [VelaTeal]: a route line wants to be
 *  unmistakably a route, and it must stay legible over every map style. */
val VelaRouteBlue = Color(0xFF1F6FEB)

// --- MAP-RMG brand tokens, spelled out for the places that need the raw brand values ------------

val MapRmgRoyalBlue = Color(0xFF2457D6)
val MapRmgRoyalBlueDark = Color(0xFF1A44B0)
val MapRmgGold = Color(0xFFD4AF37)
val MapRmgGoldIcon = Color(0xFFB8860B)
val MapRmgDarkBlue = Color(0xFF0B1C3D)
val MapRmgPaleGold = Color(0xFFFFF8EC)

/** Royal-blue tints for the theme's container roles (light theme): a soft tint of the brand, the
 *  step Material's own baseline would otherwise fill with purple. */
val MapRmgBlueContainer = Color(0xFFDCE5FB)
val MapRmgOnBlueContainer = Color(0xFF0B1C3D)

/** The dark theme's royal-blue containers: deep blue, light text. */
val MapRmgBlueContainerDark = Color(0xFF1E3A80)
val MapRmgOnBlueContainerDark = Color(0xFFDCE5FB)
