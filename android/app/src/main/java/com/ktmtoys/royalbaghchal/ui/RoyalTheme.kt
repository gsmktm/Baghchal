package com.ktmtoys.royalbaghchal.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Royal {
    val Navy=Color(0xFF080F1C)
    val Navy2=Color(0xFF0D1B2D)
    val Panel=Color(0xFF13253A)
    val PanelDeep=Color(0xFF0E1D30)
    val Gold=Color(0xFFD8AE6C)
    val GoldLight=Color(0xFFF8DFAD)
    val GoldDark=Color(0xFF9C6B38)
    val Ivory=Color(0xFFF6EAD3)
    val Fog=Color(0xFFADBCCA)
    val Crimson=Color(0xFFA63842)
    val RubyLight=Color(0xFFE87869)
    val Mint=Color(0xFF8EE3BC)
    val Wood=Color(0xFFB7864B)
    val WoodLight=Color(0xFFD9AE76)
    val WoodDark=Color(0xFF744626)
}
private val scheme=darkColorScheme(
    primary=Royal.GoldLight,onPrimary=Royal.Navy,
    secondary=Royal.Mint,onSecondary=Royal.Navy,
    background=Royal.Navy,onBackground=Royal.Ivory,
    surface=Royal.Panel,onSurface=Royal.Ivory,
    surfaceVariant=Royal.PanelDeep,onSurfaceVariant=Royal.Fog,
    outline=Royal.GoldDark,error=Royal.RubyLight
)
private val fonts=Typography(
    headlineLarge=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=38.sp),
    headlineMedium=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=30.sp),
    headlineSmall=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.SemiBold,fontSize=24.sp),
    titleLarge=TextStyle(fontFamily=FontFamily.Serif,fontWeight=FontWeight.Bold,fontSize=22.sp),
    titleMedium=TextStyle(fontFamily=FontFamily.Default,fontWeight=FontWeight.SemiBold,fontSize=17.sp),
    bodyLarge=TextStyle(fontFamily=FontFamily.Default,fontWeight=FontWeight.Normal,fontSize=16.sp),
    bodyMedium=TextStyle(fontFamily=FontFamily.Default,fontSize=14.sp),
    labelMedium=TextStyle(fontFamily=FontFamily.Default,fontWeight=FontWeight.Bold,fontSize=12.sp)
)
@Composable fun RoyalTheme(content:@Composable ()->Unit){
    MaterialTheme(colorScheme=scheme,typography=fonts,content=content)
}
