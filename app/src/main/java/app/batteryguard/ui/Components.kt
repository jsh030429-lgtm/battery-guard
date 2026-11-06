package app.batteryguard.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * 색상은 Pleos Connect 기본 차량 앱의 [충전] 화면에서 그대로 따온 값.
 * (흰 배경, 검정 글자, 초록 배터리 막대 + 연초록 여유분, 빨간 한도 표시, 주황 안내 문구)
 */
object C {
    val Bg = Color.White
    val Panel = Color(0xFFF7F7F7)
    val Text = Color.Black
    val Sub = Color(0xFF7C7C7C)
    val Line = Color(0xFFE3E3E3)
    val LineStrong = Color(0xFFA6A6A6)
    val Green = Color(0xFF00BA13)
    val GreenLight = Color(0xFFCCF1D0)
    val Red = Color(0xFFF62E24)
    val Orange = Color(0xFFFF7A1A)
}

/** 한글이 단어 중간에서 줄바꿈되지 않도록 어절 단위로 끊는다 (Android 13 이상에서 적용) */
val KoreanText = TextStyle(lineBreak = LineBreak(LineBreak.Strategy.HighQuality, LineBreak.Strictness.Normal, LineBreak.WordBreak.Phrase))

@Composable
fun T(text: String, size: TextUnit, color: Color = C.Text, weight: FontWeight = FontWeight.Normal, modifier: Modifier = Modifier) =
    Text(text, color = color, fontSize = size, fontWeight = weight, lineHeight = size * 1.4f, modifier = modifier, style = KoreanText)

/** "앞부분 <강조> 뒷부분" 형태의 제목 */
@Composable
fun Title(before: String, accent: String = "", after: String = "", accentColor: Color = C.Green, size: TextUnit = 30.sp) {
    Text(
        buildAnnotatedString {
            append(before)
            withStyle(SpanStyle(color = accentColor)) { append(accent) }
            append(after)
        },
        color = C.Text, fontSize = size, fontWeight = FontWeight.Bold, lineHeight = size * 1.35f, style = KoreanText,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) = Box(modifier.fillMaxWidth().height(1.dp).background(C.Line))

enum class BtnStyle { Primary, Normal, Ghost }

/** 기본 차량 앱과 같은 모서리 없는 버튼: 주요 = 검정 채움, 보조 = 흰 바탕 + 회색 테두리 */
@Composable
fun Btn(text: String, style: BtnStyle = BtnStyle.Normal, minWidth: Dp = 150.dp, onClick: () -> Unit) {
    val bg = if (style == BtnStyle.Primary) Color.Black else C.Bg
    val fg = when (style) {
        BtnStyle.Primary -> Color.White
        BtnStyle.Normal -> C.Text
        BtnStyle.Ghost -> C.Sub
    }
    Box(
        Modifier
            .widthIn(min = minWidth)
            .height(60.dp)
            .background(bg)
            .then(if (style == BtnStyle.Normal) Modifier.border(1.dp, C.LineStrong) else Modifier)
            .clickable(onClick = onClick)
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center,
    ) { Text(text, color = fg, fontSize = 20.sp, fontWeight = FontWeight.Medium, maxLines = 1, softWrap = false) }
}

/** 주황 느낌표 + 안내 문구 (기본 충전 화면의 "배터리 수명 최적화를 위해…" 줄과 같은 형식) */
@Composable
fun Notice(text: String, color: Color = C.Orange) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(20.dp).background(color, CircleShape), contentAlignment = Alignment.Center) {
            T("!", 14.sp, Color.White, FontWeight.Bold)
        }
        Spacer(Modifier.width(10.dp))
        T(text, 17.sp, color, FontWeight.Medium)
    }
}

/** 큰 숫자 + 작은 단위 (예: 54 %) */
@Composable
fun BigValue(value: String, unit: String, color: Color = C.Text, size: TextUnit = 34.sp) {
    Row(verticalAlignment = Alignment.Bottom) {
        T(value, size, color, FontWeight.Medium)
        T(" $unit", size * 0.5f, if (color == C.Text) C.Sub else color, modifier = Modifier.padding(bottom = (size.value * 0.12f).dp))
    }
}

/** 값 여러 개를 세로선으로 나눠 한 줄에 보여준다 (테슬라 충전 화면의 "33 km/hr  +1 km  32/32 A" 줄처럼) */
@Composable
fun StatRow(vararg stats: Triple<String, String, String>) {
    Row(Modifier.fillMaxWidth().height(96.dp), verticalAlignment = Alignment.CenterVertically) {
        stats.forEachIndexed { i, (label, value, unit) ->
            if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight(0.7f).background(C.Line))
            Column(Modifier.weight(1f).padding(start = if (i == 0) 0.dp else 28.dp)) {
                T(label, 16.sp, C.Sub)
                Spacer(Modifier.height(4.dp))
                BigValue(value, unit)
            }
        }
    }
}

/**
 * 배터리 막대: 초록 = 현재 잔량, 연초록 = 한도까지 남은 충전량, 빨간 세로선 = 충전 한도
 * 위쪽에 현재 잔량(상자)과 한도 값을 표시한다. 기본 차량 앱 [충전] 화면과 같은 구성.
 */
@Composable
fun ChargeBar(soc: Int?, limit: Int?, limitLabel: String) {
    val s = (soc ?: 0).coerceIn(0, 100)
    val l = limit?.coerceIn(0, 100)
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val w = maxWidth
        Column {
            Box(Modifier.fillMaxWidth().height(44.dp)) {
                // 현재 잔량 상자
                Box(
                    Modifier
                        .offset(x = (w * (s / 100f) - 40.dp).coerceIn(0.dp, w - 80.dp))
                        .width(80.dp)
                        .border(1.dp, C.Line)
                        .background(C.Bg)
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center,
                ) { BigValue("${soc ?: "--"}", "%", size = 22.sp) }
                // 한도 값
                if (l != null) {
                    Box(Modifier.offset(x = (w * (l / 100f) - 60.dp).coerceIn(0.dp, w - 120.dp)).width(120.dp), contentAlignment = Alignment.CenterEnd) {
                        T(limitLabel, 20.sp, C.Red, FontWeight.Medium)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            Box(Modifier.fillMaxWidth().height(64.dp).background(C.Panel)) {
                if (l != null && l > s) Box(Modifier.fillMaxWidth(l / 100f).fillMaxHeight().background(C.GreenLight))
                Box(Modifier.fillMaxWidth(s / 100f).fillMaxHeight().background(C.Green))
                // 한도 위쪽은 충전되지 않는 구간 → 기본 충전 화면처럼 세로 빗금
                if (l != null && l < 100) {
                    Canvas(Modifier.fillMaxWidth().fillMaxHeight()) {
                        val step = 7.dp.toPx()
                        var x = size.width * l / 100f + step
                        while (x < size.width) {
                            drawLine(C.LineStrong, androidx.compose.ui.geometry.Offset(x, 0f), androidx.compose.ui.geometry.Offset(x, size.height), strokeWidth = 1.dp.toPx())
                            x += step
                        }
                    }
                }
                if (l != null) Box(Modifier.offset(x = (w * (l / 100f) - 3.dp).coerceAtLeast(0.dp)).width(5.dp).fillMaxHeight().background(C.Red))
            }
        }
    }
}

@Composable
fun SampleBadge() {
    T("예시 데이터", 14.sp, C.Orange, FontWeight.Medium, Modifier.border(1.dp, C.Orange).padding(horizontal = 8.dp, vertical = 1.dp))
}

// ---------- 아이콘 (기본 앱과 같은 단색 검정 아이콘) ----------

@Composable
fun BoltIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val p = Path().apply {
            moveTo(w * 0.62f, 0f); lineTo(w * 0.18f, w * 0.58f); lineTo(w * 0.48f, w * 0.58f)
            lineTo(w * 0.36f, w); lineTo(w * 0.82f, w * 0.4f); lineTo(w * 0.52f, w * 0.4f); close()
        }
        drawPath(p, color)
    }
}

@Composable
fun ScoreIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        val bw = w * 0.22f
        listOf(0.55f, 0.35f, 0.1f).forEachIndexed { i, top ->
            drawRect(color, androidx.compose.ui.geometry.Offset(w * 0.06f + i * (bw + w * 0.1f), w * top), androidx.compose.ui.geometry.Size(bw, w * (0.95f - top)))
        }
    }
}

@Composable
fun ReportIcon(color: Color, size: Dp = 26.dp) {
    Canvas(Modifier.size(size)) {
        val w = this.size.width
        drawRect(color, androidx.compose.ui.geometry.Offset(w * 0.15f, 0f), androidx.compose.ui.geometry.Size(w * 0.7f, w))
        listOf(0.25f, 0.45f, 0.65f).forEach { y ->
            drawRect(if (color == Color.White) Color.Black else Color.White, androidx.compose.ui.geometry.Offset(w * 0.28f, w * y), androidx.compose.ui.geometry.Size(w * 0.44f, w * 0.07f))
        }
    }
}
