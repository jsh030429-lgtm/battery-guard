package app.batteryguard.vehicle

import ai.pleos.playground.vehicle.Vehicle
import ai.pleos.playground.vehicle.api.EvBattery
import ai.pleos.playground.vehicle.listener.EvBatteryStateOfChargeListener
import ai.pleos.playground.vehicle.listener.EvChargePortOpenListener
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Pleos Vehicle SDK(EvBattery)를 감싼 클래스.
 * - 잔량: registerEvBatteryStateOfCharge 구독
 * - 충전구: registerEvChargePortOpen 구독
 * - 충전 스위치: 구독 API가 없어 getEvChargeSwitchState를 주기적으로 조회
 * SDK 콜백은 다른 스레드에서 오므로 모든 상태 변경은 메인 스레드에서 한다.
 */
class CarBattery(context: Context) {

    var soc by mutableStateOf<Int?>(null)
        private set
    var portOpen by mutableStateOf<Boolean?>(null)
        private set
    var charging by mutableStateOf<Boolean?>(null)
        private set
    /** 배터리 용량 (kWh). SDK는 Wh 단위로 준다. */
    var capacityKWh by mutableStateOf<Float?>(null)
        private set
    var lastError by mutableStateOf<String?>(null)
        private set

    /** 충전기가 연결됐을 때(충전구 열림 또는 충전 시작) 한 번 호출 */
    var onPlugged: () -> Unit = {}
    /** 잔량이 바뀔 때마다 호출 */
    var onSocChanged: (Int) -> Unit = {}

    private val vehicle = Vehicle(context.applicationContext)
    private val main = Handler(Looper.getMainLooper())
    private var battery: EvBattery? = null
    private var plugged = false

    private val socListener = object : EvBatteryStateOfChargeListener {
        override fun onEvBatteryStateOfChargeUpdated(value: Int?) = onMain {
            soc = value
            value?.let(onSocChanged)
        }
        override fun onFailed(e: Exception) = report("잔량 구독", e)
    }

    private val portListener = object : EvChargePortOpenListener {
        override fun onEvChargePortOpenUpdated(value: Boolean?) = onMain {
            portOpen = value
            updatePlugged()
        }
        override fun onFailed(e: Exception) = report("충전구 구독", e)
    }

    private val pollSwitch = object : Runnable {
        override fun run() {
            battery?.getEvChargeSwitchState(
                { value -> onMain { charging = value; updatePlugged() } },
                { e -> report("충전 상태", e) }
            )
            main.postDelayed(this, POLL_MS)
        }
    }

    fun start(): Boolean = try {
        vehicle.initialize()
        val b = vehicle.getEvBattery()
        battery = b
        b.registerEvBatteryStateOfCharge(socListener)
        b.registerEvChargePortOpen(portListener)
        @Suppress("DEPRECATION")
        b.getEvBatteryCapacity({ wh -> onMain { capacityKWh = wh?.div(1000f) } }, { e -> report("배터리 용량", e) })
        main.post(pollSwitch)
        true
    } catch (e: Exception) {
        report("Vehicle 초기화", e)
        false
    }

    fun setChargeLimit(percent: Int, onResult: (applied: Int?, error: String?) -> Unit) {
        val b = battery ?: return onResult(null, "차량에 연결되지 않았어요")
        try {
            b.setEvChargePercentLimit(
                percent.toFloat(),
                { value -> onMain { onResult(value?.toInt() ?: percent, null) } },
                { e -> onMain { onResult(null, e.message ?: e.javaClass.simpleName) } }
            )
        } catch (e: Exception) {
            onResult(null, e.message ?: e.javaClass.simpleName)
        }
    }

    fun stop() {
        main.removeCallbacks(pollSwitch)
        battery?.let { b ->
            runCatching { b.unregisterEvBatteryStateOfCharge(socListener) }
            runCatching { b.unregisterEvChargePortOpen(portListener) }
        }
        runCatching { vehicle.release() }
        battery = null
    }

    private fun updatePlugged() {
        val now = portOpen == true || charging == true
        if (now && !plugged) onPlugged()
        plugged = now
    }

    private fun onMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) block() else main.post(block)
    }

    private fun report(what: String, e: Exception) {
        Log.w(TAG, "$what 실패", e)
        onMain { lastError = "$what 실패: ${e.message ?: e.javaClass.simpleName}" }
    }

    private companion object {
        const val TAG = "BatteryGuard"
        const val POLL_MS = 1_500L
    }
}
