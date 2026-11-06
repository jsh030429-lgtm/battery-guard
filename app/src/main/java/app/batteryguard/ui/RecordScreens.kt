package app.batteryguard.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** 월간 관리 점수 (기록 기능 완성 전까지 예시 데이터) */
@Composable
fun ScoreScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("11월 관리 점수", 30.sp, weight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            SampleBadge()
        }
        Spacer(Modifier.height(20.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            BigValue("92", "점", C.Green, 72.sp)
            Spacer(Modifier.width(24.dp))
            T("지난달보다 2점 올랐어요", 20.sp, C.Sub, modifier = Modifier.padding(bottom = 16.dp))
            Spacer(Modifier.weight(1f))
            Trend(listOf("6월" to 78, "7월" to 81, "8월" to 85, "9월" to 88, "10월" to 90, "11월" to 92))
        }
        Spacer(Modifier.height(24.dp))
        Divider()
        ScoreRow("적정 한도로 충전", 0.9f, "18 / 20회")
        ScoreRow("급속충전", 0.05f, "1 / 20회")
        ScoreRow("고잔량(90% 이상) 장시간 주차", 0.2f, "2회", C.Orange)
        ScoreRow("평균 충전 한도", 0.82f, "82%")
        Spacer(Modifier.height(20.dp))
        Notice("90% 이상으로 충전한 뒤 하루 넘게 세워 둔 날이 2번 있었어요. 높은 한도는 장거리 출발 전날에만 써 보세요.")
    }
}

@Composable
private fun ScoreRow(label: String, fraction: Float, value: String, color: Color = C.Green) {
    Column {
        Row(Modifier.fillMaxWidth().height(64.dp), verticalAlignment = Alignment.CenterVertically) {
            T(label, 20.sp, modifier = Modifier.width(320.dp))
            Box(Modifier.weight(1f).height(16.dp).background(C.Panel)) {
                Box(Modifier.fillMaxWidth(fraction).fillMaxHeight().background(color))
            }
            T(value, 20.sp, weight = FontWeight.Medium, modifier = Modifier.width(140.dp).padding(start = 24.dp))
        }
        Divider()
    }
}

@Composable
private fun Trend(points: List<Pair<String, Int>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.Bottom) {
        points.forEachIndexed { i, (month, score) ->
            val last = i == points.lastIndex
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                T("$score", 14.sp, if (last) C.Green else C.Sub, if (last) FontWeight.Bold else FontWeight.Normal)
                Spacer(Modifier.height(4.dp))
                Box(Modifier.width(30.dp).height(((score - 60) * 2.6f).dp).background(if (last) C.Green else C.GreenLight))
                Spacer(Modifier.height(6.dp))
                T(month, 13.sp, C.Sub)
            }
        }
    }
}

/** 관리 이력 리포트 (예시 데이터) */
@Composable
fun ReportScreen() {
    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            T("충전 관리 이력 리포트", 30.sp, weight = FontWeight.Bold)
            Spacer(Modifier.width(12.dp))
            SampleBadge()
            Spacer(Modifier.weight(1f))
            Btn("휴대폰으로 보내기", BtnStyle.Primary) {}
            Spacer(Modifier.width(16.dp))
            Btn("PDF로 내보내기") {}
        }
        Spacer(Modifier.height(8.dp))
        T("2024.03 ~ 2026.11 (33개월) · 차를 팔 때 함께 보여주세요", 18.sp, C.Sub)
        Spacer(Modifier.height(28.dp))
        val items = listOf(
            Triple("총 충전", "412", "회"), Triple("평균 충전 한도", "81", "%"), Triple("월평균 관리 점수", "89", "점"),
            Triple("급속충전 비율 (추정)", "6", "%"), Triple("고잔량 장시간 주차", "0.8", "회/월"), Triple("기록된 주행거리", "41,280", "km"),
        )
        Column(Modifier.border(1.dp, C.Line)) {
            items.chunked(3).forEachIndexed { r, row ->
                if (r > 0) Divider()
                Row(Modifier.height(128.dp), verticalAlignment = Alignment.CenterVertically) {
                    row.forEachIndexed { i, (k, v, u) ->
                        if (i > 0) Box(Modifier.width(1.dp).fillMaxHeight().background(C.Line))
                        Column(Modifier.weight(1f).padding(horizontal = 28.dp)) {
                            T(k, 17.sp, C.Sub)
                            Spacer(Modifier.height(6.dp))
                            BigValue(v, u, size = 40.sp)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(20.dp))
        Notice("배터리 상태(SOH)를 측정한 값이 아니라 충전 습관 기록이에요. 기록은 차량 안에만 저장되고, 내보낼 때만 전달돼요.", C.Sub)
        Spacer(Modifier.height(8.dp))
        T("내보내기는 다음 버전에서 연결돼요", 15.sp, C.Sub)
    }
}
