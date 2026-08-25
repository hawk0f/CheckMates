package dev.hawk0f.checkmates.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.hawk0f.checkmates.resources.Res
import dev.hawk0f.checkmates.resources.bitter_extrabold
import dev.hawk0f.checkmates.resources.caprasimo_regular
import dev.hawk0f.checkmates.resources.figtree_bold
import dev.hawk0f.checkmates.resources.figtree_regular
import dev.hawk0f.checkmates.resources.figtree_semibold
import dev.hawk0f.checkmates.resources.onest_bold
import dev.hawk0f.checkmates.resources.onest_regular
import dev.hawk0f.checkmates.resources.onest_semibold
import org.jetbrains.compose.resources.Font

private val cyrillicLanguages = setOf("ru", "uk", "be", "bg", "sr", "mk", "kk")

@Composable
private fun needsCyrillic(): Boolean = Locale.current.language in cyrillicLanguages

@Composable
fun displayFamily(): FontFamily = if (needsCyrillic()) {
    FontFamily(Font(Res.font.bitter_extrabold, FontWeight.Bold))
} else {
    FontFamily(Font(Res.font.caprasimo_regular))
}

@Composable
fun textFamily(): FontFamily = if (needsCyrillic()) {
    FontFamily(
        Font(Res.font.onest_regular, FontWeight.Normal),
        Font(Res.font.onest_semibold, FontWeight.SemiBold),
        Font(Res.font.onest_bold, FontWeight.Bold)
    )
} else {
    FontFamily(
        Font(Res.font.figtree_regular, FontWeight.Normal),
        Font(Res.font.figtree_semibold, FontWeight.SemiBold),
        Font(Res.font.figtree_bold, FontWeight.Bold)
    )
}

@Composable
fun appTypography(): Typography {
    val display = displayFamily()
    val text = textFamily()
    val displayStyle = TextStyle(fontFamily = display, letterSpacing = (-0.02).em)
    return Typography(
        displayLarge = displayStyle.copy(fontSize = 52.sp, lineHeight = 55.sp),
        displayMedium = displayStyle.copy(fontSize = 44.sp, lineHeight = 46.sp),
        displaySmall = displayStyle.copy(fontSize = 34.sp, lineHeight = 36.sp),
        headlineLarge = displayStyle.copy(fontSize = 30.sp, lineHeight = 33.sp),
        headlineMedium = displayStyle.copy(fontSize = 28.sp, lineHeight = 31.sp),
        headlineSmall = displayStyle.copy(fontSize = 24.sp, lineHeight = 27.sp),
        titleLarge = displayStyle.copy(fontSize = 22.sp, lineHeight = 26.sp),
        titleMedium = TextStyle(fontFamily = display, fontSize = 17.sp, lineHeight = 22.sp),
        titleSmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            lineHeight = 20.sp
        ),
        bodyLarge = TextStyle(fontFamily = text, fontSize = 16.sp, lineHeight = 23.sp),
        bodyMedium = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            lineHeight = 20.sp
        ),
        bodySmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            lineHeight = 17.sp
        ),
        labelLarge = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            lineHeight = 18.sp
        ),
        labelMedium = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            lineHeight = 16.sp
        ),
        labelSmall = TextStyle(
            fontFamily = text,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.12.em
        )
    )
}
