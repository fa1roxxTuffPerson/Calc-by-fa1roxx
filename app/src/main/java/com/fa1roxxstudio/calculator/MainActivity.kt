package com.fa1roxxstudio.calculator

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            PixelTheme { CalculatorScreen() }
        }
    }
}

@Composable
fun PixelTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val context = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> darkColorScheme(
            primary = Color(0xFFA8C7FA), onPrimary = Color(0xFF062E6F),
            secondaryContainer = Color(0xFF0B3A85), onSecondaryContainer = Color(0xFFD3E3FD),
            tertiaryContainer = Color(0xFF7A1B16), onTertiaryContainer = Color(0xFFF9DEDC),
            surface = Color(0xFF131314), onSurface = Color(0xFFE6E6EA),
            surfaceContainerHigh = Color(0xFF2B2D31),
        )
        else -> lightColorScheme(
            primary = Color(0xFF0B57D0), onPrimary = Color.White,
            secondaryContainer = Color(0xFFD3E3FD), onSecondaryContainer = Color(0xFF0842A0),
            tertiaryContainer = Color(0xFFFAD2CF), onTertiaryContainer = Color(0xFF8C1D18),
            surface = Color(0xFFF6F8FC), onSurface = Color(0xFF1B1B1F),
            surfaceContainerHigh = Color(0xFFE4E8F0),
        )
    }
    MaterialTheme(colorScheme = scheme, content = content)
}

private enum class Kind { NUMBER, OPERATOR, CLEAR, EQUALS }
private data class KeyDef(val label: String, val code: String, val kind: Kind = Kind.NUMBER)

private val keyRows = listOf(
    listOf(KeyDef("AC", "AC", Kind.CLEAR), KeyDef("⌫", "BS", Kind.OPERATOR), KeyDef("%", "%", Kind.OPERATOR), KeyDef("÷", "÷", Kind.OPERATOR)),
    listOf(KeyDef("7", "7"), KeyDef("8", "8"), KeyDef("9", "9"), KeyDef("×", "×", Kind.OPERATOR)),
    listOf(KeyDef("4", "4"), KeyDef("5", "5"), KeyDef("6", "6"), KeyDef("−", "−", Kind.OPERATOR)),
    listOf(KeyDef("1", "1"), KeyDef("2", "2"), KeyDef("3", "3"), KeyDef("+", "+", Kind.OPERATOR)),
    listOf(KeyDef("±", "NEG"), KeyDef("0", "0"), KeyDef(",", "."), KeyDef("=", "=", Kind.EQUALS)),
)

@Composable
fun CalculatorScreen() {
    val calc = remember { Calculator() }
    val colors = MaterialTheme.colorScheme

    Column(
        Modifier
            .fillMaxSize()
            .background(colors.surface)
            .systemBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(start = 8.dp, end = 8.dp, bottom = 20.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End,
        ) {
            val top = if (calc.done) display(calc.prev) + " =" else display(calc.expr).ifEmpty { "0" }
            val bottom = display(calc.result)
            DisplayLine(top, if (calc.done) 26 else 72, if (calc.done) 16 else 28, colors.onSurface.copy(alpha = if (calc.done) 0.6f else 1f))
            DisplayLine(bottom, if (calc.done) 72 else 30, if (calc.done) 28 else 18, colors.onSurface.copy(alpha = if (calc.done) 1f else 0.6f))
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            keyRows.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    row.forEach { key ->
                        val (container, content) = when (key.kind) {
                            Kind.NUMBER -> colors.surfaceContainerHigh to colors.onSurface
                            Kind.OPERATOR -> colors.secondaryContainer to colors.onSecondaryContainer
                            Kind.CLEAR -> colors.tertiaryContainer to colors.onTertiaryContainer
                            Kind.EQUALS -> colors.primary to colors.onPrimary
                        }
                        CalcKey(
                            label = key.label,
                            container = container,
                            content = content,
                            fontSize = if (key.kind == Kind.NUMBER || key.kind == Kind.CLEAR) 28.sp else 32.sp,
                            modifier = Modifier.weight(1f),
                        ) { calc.press(key.code) }
                    }
                }
            }
        }
    }
}

@Composable
private fun DisplayLine(text: String, maxSp: Int, minSp: Int, color: Color) {
    val size = (maxSp - maxOf(0, text.length - 8) * maxSp / 18).coerceAtLeast(minSp)
    Text(
        text = text,
        color = color,
        fontSize = size.sp,
        fontWeight = FontWeight.Light,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Clip,
        modifier = Modifier.fillMaxWidth(),
        textAlign = TextAlign.End,
    )
}

@Composable
private fun CalcKey(
    label: String,
    container: Color,
    content: Color,
    fontSize: TextUnit,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val radius by animateDpAsState(if (pressed) 16.dp else 28.dp, label = "keyRadius")

    Box(
        modifier = modifier
            .height(76.dp)
            .clip(RoundedCornerShape(radius))
            .background(container)
            .clickable(interactionSource = source, indication = null, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(label, color = content, fontSize = fontSize, fontWeight = FontWeight.Normal)
    }
}
