package app.batteryguard.logic

import kotlin.math.ceil

/** 충전 한도 제안 결과 */
data class Suggestion(
    /** 평소 주행에 필요한 잔량 (%) */
    val neededPercent: Int,
    /** 제안하는 충전 한도 (%) */
    val limitPercent: Int,
    val avgDailyKm: Int,
)

/**
 * 필요한 잔량을 계산해 충전 한도를 제안한다.
 *
 * 필요한 잔량 = 여유 잔량 + 하루 평균 주행에 쓰는 잔량 × 다음 충전까지의 날 수
 * 제안 한도 = max(배터리에 좋은 기본 한도 80%, 필요한 잔량) → 5% 단위로 올림
 * (차량이 받는 한도 값은 50~100%, 5% 단위: checkEvChargePercentLimitCapabilities로 확인)
 */
object LimitAdvisor {
    const val DEFAULT_LIMIT = 80
    private const val RESERVE_PERCENT = 40.0
    private const val DAYS_UNTIL_NEXT_CHARGE = 3
    private const val DEFAULT_KM_PER_KWH = 5.0
    private const val DEFAULT_CAPACITY_KWH = 77.4

    fun suggest(avgDailyKm: Int, capacityKWh: Float?, kmPerKWh: Double = DEFAULT_KM_PER_KWH): Suggestion {
        val capacity = capacityKWh?.toDouble()?.takeIf { it > 10 } ?: DEFAULT_CAPACITY_KWH
        val dailyPercent = avgDailyKm / kmPerKWh / capacity * 100
        val needed = roundUp5(RESERVE_PERCENT + dailyPercent * DAYS_UNTIL_NEXT_CHARGE)
        val limit = maxOf(DEFAULT_LIMIT, needed).coerceIn(50, 100)
        return Suggestion(needed.coerceIn(50, 100), limit, avgDailyKm)
    }

    private fun roundUp5(value: Double) = (ceil(value / 5) * 5).toInt()
}

/**
 * 잔량이 오르는 속도로 급속·완속을 추정한다. (SDK에 급속/완속 구분 신호가 없음)
 * 시간당 30% 넘게 오르면 급속으로 본다.
 */
class ChargeRateTracker {
    /**
     * 시연 배속. 시연 영상에서는 잔량을 몇 초마다 1%씩 올리므로, 그 시간을 이 배수만큼 늘려 실제 시간으로 환산한다.
     * 1이면 실제 시간 그대로. (화면 상단에 "시연 배속"으로 표시)
     */
    var timeScale = 1.0
    private var firstSoc: Int? = null
    private var firstAt = 0L
    var startSoc: Int? = null
        private set

    fun reset(soc: Int?) {
        firstSoc = soc
        firstAt = System.currentTimeMillis()
        startSoc = soc
    }

    fun onSoc(soc: Int) {
        if (firstSoc == null) reset(soc)
    }

    /** 시간당 잔량 상승 (%), 아직 계산할 수 없으면 null */
    fun percentPerHour(currentSoc: Int?): Double? {
        val from = firstSoc ?: return null
        val now = currentSoc ?: return null
        val hours = (System.currentTimeMillis() - firstAt) / 3_600_000.0 * timeScale
        if (now <= from || hours <= 0) return null
        return (now - from) / hours
    }

    fun isFast(rate: Double?) = rate != null && rate > 30
}
