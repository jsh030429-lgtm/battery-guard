package app.batteryguard

import app.batteryguard.logic.ChargeRateTracker
import app.batteryguard.logic.LimitAdvisor
import app.batteryguard.logic.Suggestion
import app.batteryguard.vehicle.CarBattery
import app.batteryguard.vehicle.Voice
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class Tab { Charge, Score, Report }

sealed interface ChargePhase {
    data object Waiting : ChargePhase
    data class Suggesting(val suggestion: Suggestion) : ChargePhase
    data class Applying(val percent: Int) : ChargePhase
    data class Applied(val percent: Int) : ChargePhase
    data class Failed(val percent: Int, val message: String) : ChargePhase
    data object Kept : ChargePhase
}

/**
 * 화면 상태와 흐름: 충전기 연결 → 한도 제안(음성) → 승인 → 차량에 적용 → 충전 진행 표시
 */
class AppState(val car: CarBattery, private val voice: Voice) {

    var tab by mutableStateOf(Tab.Charge)
    var phase by mutableStateOf<ChargePhase>(ChargePhase.Waiting)
        private set
    val rate = ChargeRateTracker()
    var demoSpeed by mutableStateOf(false)
        private set

    fun toggleDemoSpeed() {
        demoSpeed = !demoSpeed
        rate.timeScale = if (demoSpeed) DEMO_SCALE.toDouble() else 1.0
    }

    companion object {
        const val DEMO_SCALE = 60
    }

    init {
        car.onPlugged = ::onPlugged
        car.onSocChanged = rate::onSoc
    }

    /** 충전기가 연결되면 필요한 잔량을 계산해 제안한다 */
    fun onPlugged() {
        if (phase is ChargePhase.Suggesting || phase is ChargePhase.Applying) return
        val s = LimitAdvisor.suggest(SampleData.AVG_DAILY_KM, car.capacityKWh)
        tab = Tab.Charge
        phase = ChargePhase.Suggesting(s)
        rate.reset(car.soc)
        voice.say("충전이 시작됐어요. 평소 주행이면 ${s.neededPercent}퍼센트면 충분해요. ${s.limitPercent}퍼센트로 맞출까요?")
    }

    fun accept(percent: Int) {
        phase = ChargePhase.Applying(percent)
        car.setChargeLimit(percent) { applied, error ->
            if (applied != null) {
                // 충전 속도는 한도를 적용한 시점부터 잰다 (제안 화면에 머문 시간은 빼야 속도가 맞음)
                rate.reset(car.soc)
                phase = ChargePhase.Applied(applied)
                voice.say("충전 한도를 ${applied}퍼센트로 맞췄어요.")
            } else {
                phase = ChargePhase.Failed(percent, error ?: "알 수 없는 오류")
                voice.say("차량에 바로 적용하지 못했어요. 차량 충전 설정에서 ${percent}퍼센트로 맞춰 주세요.")
            }
        }
    }

    fun keep() {
        phase = ChargePhase.Kept
    }

    fun backToWaiting() {
        phase = ChargePhase.Waiting
    }
}

/** 기록 기능이 완성되기 전까지 쓰는 예시 데이터 (화면에 "예시 데이터"로 표시) */
object SampleData {
    const val AVG_DAILY_KM = 38
}
