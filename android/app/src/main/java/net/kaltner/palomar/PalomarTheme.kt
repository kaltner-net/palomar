package net.kaltner.palomar

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal data class PalomarSemanticColors(
    val success: Color,
    val successContainer: Color,
    val working: Color,
    val workingContainer: Color,
    val attention: Color,
    val attentionContainer: Color,
    val warning: Color,
    val warningContainer: Color,
    val failure: Color,
    val failureContainer: Color,
    val fullAccess: Color,
    val fullAccessContainer: Color,
)

internal data class PalomarThemeVariant(
    val background: Color,
    val surface: Color,
    val alternateSurface: Color,
    val raisedSurface: Color,
    val border: Color,
    val divider: Color,
    val text: Color,
    val mutedText: Color,
    val accent: Color,
    val onAccent: Color,
    val accentEmphasis: Color,
    val accentContainer: Color,
    val onAccentContainer: Color,
    val brandStructure: Color,
    val link: Color,
    val focus: Color,
    val selection: Color,
    val selectionText: Color,
    val subtleAccentSurface: Color,
    val disabledSurface: Color,
    val disabledText: Color,
    val disabledBorder: Color,
    val card: Color,
    val groupedHeader: Color,
    val usageTrack: Color,
    val usageFill: Color,
    val contextTrack: Color,
    val contextFill: Color,
    val navigation: Color,
    val dialog: Color,
    val popover: Color,
    val semantic: PalomarSemanticColors,
)

internal data class PalomarThemePalette(
    val light: PalomarThemeVariant,
    val dark: PalomarThemeVariant,
)

private val baseLightSemantic =
    PalomarSemanticColors(
        success = Color(0xFF087443),
        successContainer = Color(0xFFD9F4E5),
        working = Color(0xFF315FC4),
        workingContainer = Color(0xFFE2E9FF),
        attention = Color(0xFF9B5800),
        attentionContainer = Color(0xFFFFF0CF),
        warning = Color(0xFF8A5000),
        warningContainer = Color(0xFFFFF5D8),
        failure = Color(0xFFB42318),
        failureContainer = Color(0xFFFEE4E2),
        fullAccess = Color(0xFFA4293D),
        fullAccessContainer = Color(0xFFFFE4E9),
    )

private val baseDarkSemantic =
    PalomarSemanticColors(
        success = Color(0xFF6CE9A6),
        successContainer = Color(0xFF153C2E),
        working = Color(0xFFA9C7FF),
        workingContainer = Color(0xFF263E70),
        attention = Color(0xFFFFC56F),
        attentionContainer = Color(0xFF4B2D0C),
        warning = Color(0xFFFFD58A),
        warningContainer = Color(0xFF49330F),
        failure = Color(0xFFFFB4AB),
        failureContainer = Color(0xFF571D1B),
        fullAccess = Color(0xFFFFB0BC),
        fullAccessContainer = Color(0xFF5C1F2B),
    )

private val highContrastLightSemantic =
    PalomarSemanticColors(
        success = Color(0xFF005A2B),
        successContainer = Color(0xFFD5F5DF),
        working = Color(0xFF0033A0),
        workingContainer = Color(0xFFDBE7FF),
        attention = Color(0xFF7A3E00),
        attentionContainer = Color(0xFFFFEDC2),
        warning = Color(0xFF6B3C00),
        warningContainer = Color(0xFFFFF0C7),
        failure = Color(0xFF970B0B),
        failureContainer = Color(0xFFFFE0E0),
        fullAccess = Color(0xFF8F1232),
        fullAccessContainer = Color(0xFFFFDCE5),
    )

private val highContrastDarkSemantic =
    PalomarSemanticColors(
        success = Color(0xFF7FF0A8),
        successContainer = Color(0xFF002E16),
        working = Color(0xFF9BC7FF),
        workingContainer = Color(0xFF00265C),
        attention = Color(0xFFFFD27A),
        attentionContainer = Color(0xFF442400),
        warning = Color(0xFFFFE08A),
        warningContainer = Color(0xFF3A2600),
        failure = Color(0xFFFF9E9E),
        failureContainer = Color(0xFF4A0000),
        fullAccess = Color(0xFFFF9FBD),
        fullAccessContainer = Color(0xFF4A0018),
    )

private val neonWaveLightSemantic =
    PalomarSemanticColors(
        success = Color(0xFF08704C),
        successContainer = Color(0xFFD8F3E9),
        working = Color(0xFF006F78),
        workingContainer = Color(0xFFD5F4F4),
        attention = Color(0xFF8A4B00),
        attentionContainer = Color(0xFFFFF0CF),
        warning = Color(0xFF7B4A00),
        warningContainer = Color(0xFFFFF5D8),
        failure = Color(0xFFB42318),
        failureContainer = Color(0xFFFEE4E2),
        fullAccess = Color(0xFFA4293D),
        fullAccessContainer = Color(0xFFFFE4E9),
    )

private val neonWaveDarkSemantic =
    PalomarSemanticColors(
        success = Color(0xFF66E8B7),
        successContainer = Color(0xFF143B31),
        working = Color(0xFF55F6FF),
        workingContainer = Color(0xFF103A4A),
        attention = Color(0xFFFFC56F),
        attentionContainer = Color(0xFF4B2D0C),
        warning = Color(0xFFFFD58A),
        warningContainer = Color(0xFF49330F),
        failure = Color(0xFFFFB4AB),
        failureContainer = Color(0xFF571D1B),
        fullAccess = Color(0xFFFF9FC0),
        fullAccessContainer = Color(0xFF5C1F2B),
    )

private val obsidianLightSemantic =
    PalomarSemanticColors(
        success = Color(0xFF166D45),
        successContainer = Color(0xFFDBF1E4),
        working = Color(0xFF67507C),
        workingContainer = Color(0xFFE8E0F0),
        attention = Color(0xFF8A4B00),
        attentionContainer = Color(0xFFFFF0CF),
        warning = Color(0xFF7B4A00),
        warningContainer = Color(0xFFFFF5D8),
        failure = Color(0xFFB42318),
        failureContainer = Color(0xFFFEE4E2),
        fullAccess = Color(0xFF872957),
        fullAccessContainer = Color(0xFFF5DAE6),
    )

private val obsidianDarkSemantic =
    PalomarSemanticColors(
        success = Color(0xFF70D9A0),
        successContainer = Color(0xFF18382C),
        working = Color(0xFFB39BC8),
        workingContainer = Color(0xFF34283F),
        attention = Color(0xFFF4C274),
        attentionContainer = Color(0xFF443016),
        warning = Color(0xFFF5D18A),
        warningContainer = Color(0xFF433518),
        failure = Color(0xFFF3AAA5),
        failureContainer = Color(0xFF4C2020),
        fullAccess = Color(0xFFE394B7),
        fullAccessContainer = Color(0xFF4A2033),
    )

private fun tint(base: Color, accent: Color, amount: Float): Color =
    Color(
        red = base.red + (accent.red - base.red) * amount,
        green = base.green + (accent.green - base.green) * amount,
        blue = base.blue + (accent.blue - base.blue) * amount,
        alpha = base.alpha + (accent.alpha - base.alpha) * amount,
    )

private fun themedSemanticColors(
    base: PalomarSemanticColors,
    accent: Color,
    accentContainer: Color,
    background: Color,
    dark: Boolean,
): PalomarSemanticColors =
    base.copy(
        success = tint(base.success, accent, 0.10f),
        successContainer = tint(base.successContainer, accentContainer, 0.10f),
        working = accent,
        workingContainer = if (dark) tint(accentContainer, background, 0.20f) else accentContainer,
        attention = tint(base.attention, accent, 0.10f),
        attentionContainer = tint(base.attentionContainer, accentContainer, 0.10f),
        warning = tint(base.warning, accent, 0.10f),
        warningContainer = tint(base.warningContainer, accentContainer, 0.10f),
        failure = tint(base.failure, accent, 0.10f),
        failureContainer = tint(base.failureContainer, accentContainer, 0.10f),
        fullAccess = tint(base.fullAccess, accent, 0.10f),
        fullAccessContainer = tint(base.fullAccessContainer, accentContainer, 0.10f),
    )

private fun lightVariant(
    background: Long,
    surface: Long,
    alternate: Long,
    border: Long,
    text: Long,
    muted: Long,
    accent: Long,
    accentEmphasis: Long,
    accentContainer: Long,
    onAccentContainer: Long,
    link: Long,
    focus: Long,
    brandStructure: Long = accent,
    disabledSurface: Long = 0xFFE7E3EB,
    disabledText: Long = 0xFF8A8492,
    disabledBorder: Long = 0xFFD3CDD9,
    semantic: PalomarSemanticColors? = null,
) = PalomarThemeVariant(
    background = Color(background),
    surface = Color(surface),
    alternateSurface = Color(alternate),
    raisedSurface = Color(surface),
    border = Color(border),
    divider = Color(border),
    text = Color(text),
    mutedText = Color(muted),
    accent = Color(accent),
    onAccent = Color.White,
    accentEmphasis = Color(accentEmphasis),
    accentContainer = Color(accentContainer),
    onAccentContainer = Color(onAccentContainer),
    brandStructure = Color(brandStructure),
    link = Color(link),
    focus = Color(focus),
    selection = Color(accentContainer),
    selectionText = Color(onAccentContainer),
    subtleAccentSurface = tint(Color(surface), Color(accentContainer), 0.45f),
    disabledSurface = Color(disabledSurface),
    disabledText = Color(disabledText),
    disabledBorder = Color(disabledBorder),
    card = Color(surface),
    groupedHeader = Color(alternate),
    usageTrack = Color(alternate),
    usageFill = Color(brandStructure),
    contextTrack = Color(border),
    contextFill = Color(accent),
    navigation = Color(surface),
    dialog = Color(alternate),
    popover = Color(surface),
    semantic = semantic ?: themedSemanticColors(
        baseLightSemantic,
        Color(accent),
        Color(accentContainer),
        Color(background),
        dark = false,
    ),
)

private fun darkVariant(
    background: Long,
    surface: Long,
    alternate: Long,
    border: Long,
    text: Long,
    muted: Long,
    accent: Long,
    onAccent: Long,
    accentEmphasis: Long,
    accentContainer: Long,
    onAccentContainer: Long,
    link: Long,
    focus: Long,
    brandStructure: Long = accent,
    disabledSurface: Long = 0xFF2D2834,
    disabledText: Long = 0xFF746C7D,
    disabledBorder: Long = 0xFF3A3442,
    semantic: PalomarSemanticColors? = null,
) = PalomarThemeVariant(
    background = Color(background),
    surface = Color(surface),
    alternateSurface = Color(alternate),
    raisedSurface = Color(surface),
    border = Color(border),
    divider = Color(border),
    text = Color(text),
    mutedText = Color(muted),
    accent = Color(accent),
    onAccent = Color(onAccent),
    accentEmphasis = Color(accentEmphasis),
    accentContainer = Color(accentContainer),
    onAccentContainer = Color(onAccentContainer),
    brandStructure = Color(brandStructure),
    link = Color(link),
    focus = Color(focus),
    selection = Color(accentContainer),
    selectionText = Color(onAccentContainer),
    subtleAccentSurface = tint(Color(surface), Color(accentContainer), 0.45f),
    disabledSurface = Color(disabledSurface),
    disabledText = Color(disabledText),
    disabledBorder = Color(disabledBorder),
    card = Color(surface),
    groupedHeader = Color(alternate),
    usageTrack = Color(alternate),
    usageFill = Color(brandStructure),
    contextTrack = Color(border),
    contextFill = Color(accent),
    navigation = Color(surface),
    dialog = Color(alternate),
    popover = Color(surface),
    semantic = semantic ?: themedSemanticColors(
        baseDarkSemantic,
        Color(accent),
        Color(accentContainer),
        Color(background),
        dark = true,
    ),
)

internal fun palomarThemePalette(themeId: ThemeId): PalomarThemePalette =
    when (themeId) {
        ThemeId.Palomar -> PalomarThemePalette(
            light = lightVariant(
                0xFFF7F8FC, 0xFFFFFFFF, 0xFFEEF0F7, 0xFFD7DAE5,
                0xFF111326, 0xFF5D6175, 0xFF006E73, 0xFF493B82,
                0xFFEAE5FF, 0xFF2F2853, 0xFF006E73, 0xFF006E73,
                brandStructure = 0xFF493B82,
                disabledSurface = 0xFFE8EAF1,
                disabledText = 0xFF686B7C,
                disabledBorder = 0xFFCDD1DC,
                semantic = baseLightSemantic,
            ).copy(contextFill = Color(0xFF006E73)),
            dark = darkVariant(
                0xFF090B16, 0xFF111326, 0xFF171527, 0xFF30354D,
                0xFFFFFFFF, 0xFFB6B7CA, 0xFF62F9F8, 0xFF090B16,
                0xFFCFC1FD, 0xFF352D63, 0xFFF5F0FF, 0xFF62F9F8, 0xFF62F9F8,
                brandStructure = 0xFFCFC1FD,
                disabledSurface = 0xFF202338,
                disabledText = 0xFF989BAD,
                disabledBorder = 0xFF30344A,
                semantic = baseDarkSemantic,
            ).copy(contextFill = Color(0xFF62F9F8)),
        )
        ThemeId.Harbor -> PalomarThemePalette(
            light = lightVariant(0xFFF1F7F8, 0xFFFFFFFF, 0xFFE5F0F2, 0xFFCBDFE2, 0xFF122326, 0xFF5D7377, 0xFF006B75, 0xFF00515A, 0xFFC5EDF0, 0xFF00363C, 0xFF005E8A, 0xFF007984),
            dark = darkVariant(0xFF0D1719, 0xFF142226, 0xFF203137, 0xFF385159, 0xFFEDF8FA, 0xFFA2B9BD, 0xFF74D8E1, 0xFF00363C, 0xFF4BBBC6, 0xFF134D54, 0xFFD3F8FB, 0xFF8BDCFF, 0xFF74D8E1),
        )
        ThemeId.Grove -> PalomarThemePalette(
            light = lightVariant(0xFFF4F7F1, 0xFFFFFFFF, 0xFFE9F0E5, 0xFFD2DECF, 0xFF19231A, 0xFF637062, 0xFF356A3F, 0xFF25522F, 0xFFD6ECD2, 0xFF15391D, 0xFF2D6337, 0xFF40784A),
            dark = darkVariant(0xFF111812, 0xFF19231A, 0xFF263127, 0xFF3E5140, 0xFFF0F8ED, 0xFFA8B9A5, 0xFF91D99A, 0xFF113919, 0xFF6FBD79, 0xFF285C31, 0xFFDDF8DC, 0xFFA5E7AD, 0xFF91D99A),
        )
        ThemeId.Ember -> PalomarThemePalette(
            light = lightVariant(0xFFFAF3F4, 0xFFFFFFFF, 0xFFF3E7EB, 0xFFE4D2D9, 0xFF291B21, 0xFF77636B, 0xFF8A3D61, 0xFF6D2A4B, 0xFFF0D3DF, 0xFF48142D, 0xFF7D3558, 0xFF97496D),
            dark = darkVariant(0xFF1A1115, 0xFF25191E, 0xFF34242B, 0xFF523B45, 0xFFFFF1F5, 0xFFC0A7B1, 0xFFEFACC8, 0xFF521A35, 0xFFD888AA, 0xFF6B2948, 0xFFFFE8F1, 0xFFFFC0DA, 0xFFEFACC8),
        )
        ThemeId.Dune -> PalomarThemePalette(
            light = lightVariant(0xFFFAF6ED, 0xFFFFFDF8, 0xFFF2E7D2, 0xFFD9C6A3, 0xFF2B2115, 0xFF6F604D, 0xFF7A4F00, 0xFF5D3C00, 0xFFF3DCA9, 0xFF3F2900, 0xFF704600, 0xFF8A5A00),
            dark = darkVariant(0xFF18140D, 0xFF221C12, 0xFF302719, 0xFF55462F, 0xFFFFF6E4, 0xFFC0AD8B, 0xFFF2C66D, 0xFF3E2D00, 0xFFD7A942, 0xFF5A4213, 0xFFFFEBBD, 0xFFFFD587, 0xFFF2C66D),
        )
        ThemeId.Slate -> PalomarThemePalette(
            light = lightVariant(0xFFF3F6FA, 0xFFFFFFFF, 0xFFE7EDF4, 0xFFCBD5E1, 0xFF172033, 0xFF59677B, 0xFF365A8C, 0xFF27456F, 0xFFD8E6F8, 0xFF152F52, 0xFF2D5489, 0xFF40699E),
            dark = darkVariant(0xFF0F141C, 0xFF171F2B, 0xFF222D3B, 0xFF3B4A5F, 0xFFF2F6FB, 0xFFAAB7C7, 0xFF9FC5F5, 0xFF183656, 0xFF78A7DD, 0xFF294F78, 0xFFE4F0FF, 0xFFAFD2FF, 0xFF9FC5F5),
        )
        ThemeId.NeonWave -> PalomarThemePalette(
            light = lightVariant(
                0xFFF8F6FC, 0xFFFFFFFF, 0xFFF0ECF8, 0xFFD7CDE6,
                0xFF171126, 0xFF655D75, 0xFF9B006F, 0xFF006F78,
                0xFFF2D7EE, 0xFF4E123E, 0xFF006F78, 0xFF9B006F,
                brandStructure = 0xFF6C3CB2,
                disabledSurface = 0xFFEBE7F0,
                disabledText = 0xFF716A7B,
                disabledBorder = 0xFFD4CCDF,
                semantic = neonWaveLightSemantic,
            ).copy(
                raisedSurface = Color(0xFFF0ECF8),
                card = Color(0xFFFFFFFF),
                groupedHeader = Color(0xFFE9E2F6),
                navigation = Color(0xFFF0ECF8),
                dialog = Color(0xFFF8F6FC),
                popover = Color(0xFFF0ECF8),
                contextFill = Color(0xFF006F78),
            ),
            dark = darkVariant(
                0xFF060817, 0xFF0C1024, 0xFF161438, 0xFF3B3F6B,
                0xFFF8F6FF, 0xFFB7B3C9, 0xFFFF4FD8, 0xFF200018,
                0xFF55F6FF, 0xFF4A174C, 0xFFFFE8FA, 0xFF73F4FF, 0xFFFF4FD8,
                brandStructure = 0xFFB69CFF,
                disabledSurface = 0xFF1B1E35,
                disabledText = 0xFF9692A8,
                disabledBorder = 0xFF34374D,
                semantic = neonWaveDarkSemantic,
            ).copy(
                raisedSurface = Color(0xFF161438),
                card = Color(0xFF161438),
                groupedHeader = Color(0xFF211B49),
                navigation = Color(0xFF0C1024),
                dialog = Color(0xFF161438),
                popover = Color(0xFF161438),
                contextFill = Color(0xFF55F6FF),
            ),
        )
        ThemeId.Obsidian -> PalomarThemePalette(
            light = lightVariant(
                0xFFF7F5F6, 0xFFFFFFFF, 0xFFEEE9ED, 0xFFD3CBD3,
                0xFF1D171C, 0xFF685F67, 0xFF872957, 0xFF67507C,
                0xFFEEDAE4, 0xFF4D1832, 0xFF75305A, 0xFF872957,
                brandStructure = 0xFF67507C,
                disabledSurface = 0xFFE8E4E7,
                disabledText = 0xFF746C73,
                disabledBorder = 0xFFCFC8CE,
                semantic = obsidianLightSemantic,
            ).copy(
                raisedSurface = Color(0xFFFFFFFF),
                card = Color(0xFFFFFFFF),
                groupedHeader = Color(0xFFEEE9ED),
                navigation = Color(0xFFFFFFFF),
                dialog = Color(0xFFF7F5F6),
                popover = Color(0xFFFFFFFF),
            ),
            dark = darkVariant(
                0xFF0B0C0F, 0xFF13151A, 0xFF1C1E24, 0xFF45414B,
                0xFFF6F3F5, 0xFFB7AFB5, 0xFFD66A99, 0xFF260914,
                0xFFB39BC8, 0xFF4A2033, 0xFFFFE7F0, 0xFFE394B7, 0xFFE176A7,
                brandStructure = 0xFFB39BC8,
                disabledSurface = 0xFF24252B,
                disabledText = 0xFF999298,
                disabledBorder = 0xFF3A3B42,
                semantic = obsidianDarkSemantic,
            ).copy(
                raisedSurface = Color(0xFF1C1E24),
                card = Color(0xFF13151A),
                groupedHeader = Color(0xFF24262C),
                navigation = Color(0xFF13151A),
                dialog = Color(0xFF1C1E24),
                popover = Color(0xFF1C1E24),
            ),
        )
        ThemeId.HighContrast -> PalomarThemePalette(
            light = lightVariant(
                0xFFFFFFFF, 0xFFFFFFFF, 0xFFE6E6E6, 0xFF1A1A1A, 0xFF000000, 0xFF333333,
                0xFF0033A0, 0xFF001F66, 0xFFC9DCFF, 0xFF001B54, 0xFF0033A0, 0xFF7A1F00,
                disabledSurface = 0xFFD9D9D9,
                disabledText = 0xFF595959,
                disabledBorder = 0xFF595959,
                semantic = highContrastLightSemantic,
            ),
            dark = darkVariant(
                0xFF000000, 0xFF050505, 0xFF1A1A1A, 0xFFFFFFFF, 0xFFFFFFFF, 0xFFE0E0E0,
                0xFF79B8FF, 0xFF000000, 0xFFA9D1FF, 0xFF002D73, 0xFFFFFFFF, 0xFF8FC6FF, 0xFFFFDD57,
                disabledSurface = 0xFF1F1F1F,
                disabledText = 0xFFB3B3B3,
                disabledBorder = 0xFF999999,
                semantic = highContrastDarkSemantic,
            ),
        )
    }

internal fun palomarThemeVariant(themeId: ThemeId, darkTheme: Boolean): PalomarThemeVariant =
    palomarThemePalette(themeId).let { if (darkTheme) it.dark else it.light }

internal fun palomarColorScheme(themeId: ThemeId, darkTheme: Boolean) =
    palomarThemeVariant(themeId, darkTheme).let { colors ->
        val semantic = colors.semantic
        val scheme = if (darkTheme) darkColorScheme() else lightColorScheme()
        scheme.copy(
            primary = colors.accent,
            onPrimary = colors.onAccent,
            primaryContainer = colors.accentContainer,
            onPrimaryContainer = colors.onAccentContainer,
            secondary = colors.accentEmphasis,
            onSecondary = colors.onAccent,
            secondaryContainer = colors.selection,
            onSecondaryContainer = colors.onAccentContainer,
            tertiary = colors.link,
            onTertiary = colors.onAccent,
            background = colors.background,
            onBackground = colors.text,
            surface = colors.surface,
            onSurface = colors.text,
            surfaceVariant = colors.alternateSurface,
            onSurfaceVariant = colors.mutedText,
            surfaceDim = colors.background,
            surfaceBright = colors.alternateSurface,
            surfaceContainerLowest = colors.background,
            surfaceContainer = colors.raisedSurface,
            surfaceContainerLow = colors.card,
            surfaceContainerHigh = colors.dialog,
            surfaceContainerHighest = colors.popover,
            outline = colors.border,
            outlineVariant = colors.divider,
            error = semantic.failure,
            onError = if (darkTheme) Color(0xFF690005) else Color.White,
            errorContainer = semantic.failureContainer,
            onErrorContainer = semantic.failure,
            surfaceTint = colors.accent,
            scrim = Color.Black,
        )
    }

internal val LocalPalomarColors = staticCompositionLocalOf {
    palomarThemePalette(ThemeId.Palomar).light.semantic
}

internal val LocalPalomarThemeVariant = staticCompositionLocalOf {
    palomarThemePalette(ThemeId.Palomar).light
}

internal val LocalPalomarDarkTheme = staticCompositionLocalOf { false }

@Composable
internal fun PalomarTheme(
    themeId: ThemeId,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val variant = palomarThemeVariant(themeId, darkTheme)
    CompositionLocalProvider(
        LocalPalomarColors provides variant.semantic,
        LocalPalomarThemeVariant provides variant,
        LocalPalomarDarkTheme provides darkTheme,
    ) {
        MaterialTheme(colorScheme = palomarColorScheme(themeId, darkTheme), content = content)
    }
}
