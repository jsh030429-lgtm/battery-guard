package app.batteryguard

import app.batteryguard.ui.BoltIcon
import app.batteryguard.ui.C
import app.batteryguard.ui.ChargeScreen
import app.batteryguard.ui.ReportIcon
import app.batteryguard.ui.ReportScreen
import app.batteryguard.ui.ScoreIcon
import app.batteryguard.ui.ScoreScreen
import app.batteryguard.ui.T
import app.batteryguard.vehicle.CarBattery
import app.batteryguard.vehicle.Voice
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {

    private lateinit var car: CarBattery
    private lateinit var voice: Voice
    private lateinit var state: AppState

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        car = CarBattery(this)
        voice = Voice(this)
        state = AppState(car, voice)
        car.start()

        setContent {
            // 화면 폭이 늘 DESIGN_WIDTH(dp)가 되도록 배율을 맞춘다 → 차량 화면 해상도와 상관없이 같은 비율로 보임
            BoxWithConstraints(Modifier.fillMaxSize().background(C.Bg)) {
                val px = constraints.maxWidth.toFloat()
                CompositionLocalProvider(LocalDensity provides Density(px / DESIGN_WIDTH, 1f)) {
                    Row(Modifier.fillMaxSize()) {
                        SideMenu(state)
                        Box(Modifier.width(1.dp).fillMaxHeight().background(C.Line))
                        Box(Modifier.weight(1f)) {
                            when (state.tab) {
                                Tab.Charge -> ChargeScreen(state)
                                Tab.Score -> ScoreScreen()
                                Tab.Report -> ReportScreen()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        voice.warmUp()
    }

    override fun onStop() {
        voice.coolDown()
        super.onStop()
    }

    override fun onDestroy() {
        car.stop()
        voice.release()
        super.onDestroy()
    }

    private companion object {
        const val DESIGN_WIDTH = 1150f
    }
}

/** 기본 차량 앱과 같은 왼쪽 메뉴: 선택된 항목은 검정 바탕 + 흰 글자 */
@Composable
private fun SideMenu(state: AppState) {
    Column(Modifier.width(230.dp).fillMaxHeight()) {
        Column(Modifier.padding(start = 28.dp, top = 32.dp, bottom = 24.dp)) {
            T("배터리 지킴이", 22.sp, weight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            // 누르면 시연 배속(×60)을 켜고 끈다
            T(
                if (state.demoSpeed) "프로토타입 · 시연 배속 ×${AppState.DEMO_SCALE}" else "프로토타입", 13.sp, C.Orange, FontWeight.Medium,
                Modifier.border(1.dp, C.Orange).clickable { state.toggleDemoSpeed() }.padding(horizontal = 8.dp, vertical = 1.dp)
            )
        }
        MenuItem("충전", state.tab == Tab.Charge, { BoltIcon(it, 24.dp) }) { state.tab = Tab.Charge }
        MenuItem("관리 점수", state.tab == Tab.Score, { ScoreIcon(it, 24.dp) }) { state.tab = Tab.Score }
        MenuItem("리포트", state.tab == Tab.Report, { ReportIcon(it, 24.dp) }) { state.tab = Tab.Report }
    }
}

@Composable
private fun MenuItem(label: String, selected: Boolean, icon: @Composable (Color) -> Unit, onClick: () -> Unit) {
    val fg = if (selected) Color.White else Color.Black
    Row(
        Modifier.fillMaxWidth().height(68.dp)
            .background(if (selected) Color.Black else C.Bg)
            .clickable(onClick = onClick)
            .padding(start = 28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(fg)
        Spacer(Modifier.width(16.dp))
        T(label, 21.sp, fg, FontWeight.Medium)
    }
}
