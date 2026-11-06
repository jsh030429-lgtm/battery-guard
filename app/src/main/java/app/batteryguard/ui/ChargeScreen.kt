package app.batteryguard.ui

import app.batteryguard.AppState
import app.batteryguard.ChargePhase
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

/**
 * 충전 화면. 위쪽 절반은 기본 차량 앱 [충전] 화면과 같은 형식(큰 잔량 + 배터리 막대),
 * 아래쪽은 배터리 지킴이의 제안·결과를 한 줄 제목 + 버튼/수치 한 줄로 보여준다.
 */
@Composable
fun ChargeScreen(state: AppState) {
    val car = state.car
    val soc = car.soc
    val phase = state.phase
    val (limit, limitLabel) = when (phase) {
        is ChargePhase.Suggesting -> phase.suggestion.limitPercent to "제안 ${phase.suggestion.limitPercent}%"
        is ChargePhase.Applying -> phase.percent to "${phase.percent}%"
        is ChargePhase.Applied -> phase.percent to "한도 ${phase.percent}%"
        is ChargePhase.Failed -> phase.percent to "제안 ${phase.percent}%"
        else -> null to ""
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 48.dp, vertical = 36.dp)) {
        // 1. 큰 잔량 + 상태 (대기 화면에서 번개 아이콘을 누르면 시연용으로 충전 연결을 흉내 낸다)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(64.dp).background(C.GreenLight, CircleShape)
                    .clickable(enabled = phase == ChargePhase.Waiting) { state.onPlugged() },
                contentAlignment = Alignment.Center,
            ) { BoltIcon(C.Green, 32.dp) }
            Spacer(Modifier.width(20.dp))
            BigValue("${soc ?: "--"}", "%", C.Green, 72.sp)
            Spacer(Modifier.weight(1f))
            val (status, color) = when {
                car.charging == true -> "충전 중" to C.Green
                car.portOpen == true -> "충전기 연결됨" to C.Green
                else -> "충전 대기" to C.Sub
            }
            T(status, 20.sp, color, FontWeight.Medium)
        }
        Spacer(Modifier.height(28.dp))

        // 2. 배터리 막대
        ChargeBar(soc, limit, limitLabel)
        Spacer(Modifier.height(16.dp))
        Notice("배터리 지킴이가 충전할 때마다 오늘 필요한 만큼만 충전하도록 제안해요.", C.Sub)
        Spacer(Modifier.height(28.dp))
        Divider()
        Spacer(Modifier.height(28.dp))

        // 3. 제안·결과
        when (phase) {
            ChargePhase.Waiting -> {
                Title("충전기를 연결하면 ", "오늘 필요한 만큼", "만 제안할게요.")
                Spacer(Modifier.height(8.dp))
                T("평소 주행 패턴과 내비게이션 목적지를 보고 충전 한도를 계산해요.", 18.sp, C.Sub)
                car.lastError?.let { Spacer(Modifier.height(12.dp)); Notice(it, C.Red) }
            }
            is ChargePhase.Suggesting -> {
                val s = phase.suggestion
                Title("평소 주행이면 ${s.neededPercent}%면 충분해요. ", "${s.limitPercent}%로 맞출까요?")
                Spacer(Modifier.height(8.dp))
                val capacity = car.capacityKWh?.let { " · 배터리 %.1f kWh".format(it) } ?: ""
                T("최근 2주 하루 평균 ${s.avgDailyKm} km (예시) · 내일 목적지 없음$capacity · 음성으로 안내했어요", 17.sp, C.Sub)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Btn("${s.limitPercent}%로 맞추기", BtnStyle.Primary, minWidth = 220.dp) { state.accept(s.limitPercent) }
                    if (s.limitPercent < 90) Btn("90%") { state.accept(90) }
                    Btn("그대로 두기") { state.keep() }
                }
            }
            is ChargePhase.Applying -> Title("충전 한도를 ", "${phase.percent}%", "로 맞추고 있어요…")
            is ChargePhase.Applied -> Applied(state, phase.percent)
            is ChargePhase.Failed -> {
                Title("차량 충전 설정에서 ", "${phase.percent}%", "로 맞춰 주세요.", C.Orange)
                Spacer(Modifier.height(8.dp))
                T("바로 적용하지 못했어요. 목표 잔량에 도달하면 알려 드릴게요. (${phase.message})", 17.sp, C.Sub)
                Spacer(Modifier.height(24.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Btn("다시 시도", BtnStyle.Primary) { state.accept(phase.percent) }
                    Btn("닫기") { state.backToWaiting() }
                }
            }
            ChargePhase.Kept -> {
                Title("지금 충전 한도를 유지해요.")
                Spacer(Modifier.height(24.dp))
                Btn("다시 제안 받기") { state.onPlugged() }
            }
        }
    }
}

@Composable
private fun Applied(state: AppState, percent: Int) {
    val soc = state.car.soc
    val rate = state.rate.percentPerHour(soc)
    val finish = when {
        soc != null && soc >= percent -> "완료"
        rate != null && soc != null -> LocalTime.now().plusMinutes(((percent - soc) / rate * 60).roundToInt().toLong()).format(DateTimeFormatter.ofPattern("HH:mm"))
        else -> "계산 중"
    }
    val added = state.rate.startSoc?.let { start -> soc?.minus(start) }?.coerceAtLeast(0) ?: 0
    val kind = when {
        rate == null -> "측정 중"
        state.rate.isFast(rate) -> "급속"
        else -> "완속"
    }

    Row(verticalAlignment = Alignment.CenterVertically) {
        Title("충전 한도를 ", "$percent%", "로 맞췄어요.")
        Spacer(Modifier.weight(1f))
        Btn("확인", BtnStyle.Primary, minWidth = 120.dp) { state.backToWaiting() }
        Spacer(Modifier.width(16.dp))
        Btn("한도 다시 바꾸기") { state.onPlugged() }
    }
    Spacer(Modifier.height(4.dp))
    T("$percent%가 되면 충전이 자동으로 멈춰요. 끝나면 오늘 충전이 기록에 저장돼요.", 17.sp, C.Sub)
    Spacer(Modifier.height(12.dp))
    StatRow(
        Triple("예상 완료", finish, ""),
        Triple("이번 충전", "+$added", "%"),
        Triple("충전 방식 (추정)", kind, ""),
        Triple("충전 속도", rate?.roundToInt()?.toString() ?: "--", "%/시간"),
    )
}
