package app.batteryguard.vehicle

import ai.pleos.playground.tts.TextToSpeech
import ai.pleos.playground.tts.constant.ErrorType
import ai.pleos.playground.tts.constant.Mode
import ai.pleos.playground.tts.listener.OnInitializeListener
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.util.Locale

/**
 * Pleos TextToSpeech(온디바이스) 음성 안내.
 * 초기화가 끝나기 전에 요청된 문장은 하나만 기억했다가 준비되면 읽는다.
 * (서버 모드는 클라이언트 키가 필요해 쓰지 않는다)
 *
 * 첫마디가 깨지는 문제 대응:
 * - 오디오 출력이 "열리는 순간" 소리가 튀므로, 앱이 떠 있는 동안 무음을 계속 재생해 출력을 미리 열어 둔다.
 * - 화면 전환·차량 신호가 몰리는 순간을 피해 SPEAK_DELAY_MS 뒤에 말한다.
 */
class Voice(context: Context) {

    private val tts = TextToSpeech(context.applicationContext, Mode.ON_DEVICE)
    private val main = Handler(Looper.getMainLooper())
    private var ready = false
    private var pending: String? = null
    private var silence: AudioTrack? = null

    init {
        try {
            tts.initialize(object : OnInitializeListener {
                override fun onSuccess() {
                    ready = true
                    runCatching { tts.setLocale(Locale.KOREA) } // ko-KR 필수
                    pending?.let { speakNow(it) }
                    pending = null
                }

                override fun onFailure(type: ErrorType) {
                    Log.w(TAG, "TTS 초기화 실패: $type")
                }
            })
        } catch (e: Exception) {
            Log.w(TAG, "TTS 초기화 예외", e)
        }
    }

    fun say(text: String) {
        main.removeCallbacksAndMessages(null)
        main.postDelayed({ if (ready) speakNow(text) else pending = text }, SPEAK_DELAY_MS)
    }

    /** 앱이 화면에 보일 때 무음 재생 시작 → 오디오 출력을 열어 둔다 */
    fun warmUp() {
        if (silence != null) return
        silence = runCatching {
            val rate = 48_000
            val frames = rate / 2 // 0.5초 분량의 무음을 반복
            AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(rate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(frames * 2)
                .build()
                .apply {
                    write(ShortArray(frames), 0, frames)
                    setLoopPoints(0, frames, -1)
                    play()
                }
        }.onFailure { Log.w(TAG, "무음 재생 실패", it) }.getOrNull()
    }

    fun coolDown() {
        silence?.let { runCatching { it.stop(); it.release() } }
        silence = null
    }

    fun release() {
        main.removeCallbacksAndMessages(null)
        coolDown()
        runCatching { tts.release() }
    }

    private fun speakNow(text: String) {
        try {
            tts.speak(text, true)
        } catch (e: Exception) {
            Log.w(TAG, "TTS 말하기 예외", e)
        }
    }

    private companion object {
        const val TAG = "BatteryGuard"
        const val SPEAK_DELAY_MS = 800L
    }
}
