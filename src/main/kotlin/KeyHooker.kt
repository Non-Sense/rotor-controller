import com.github.kwhat.jnativehook.keyboard.NativeKeyEvent
import com.github.kwhat.jnativehook.keyboard.NativeKeyListener
import com.github.kwhat.jnativehook.mouse.NativeMouseEvent
import com.github.kwhat.jnativehook.mouse.NativeMouseListener
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch


data class KeyEvent(
    val type: EventType,
    val code: KeyCode
)

enum class EventType {
    Push,
    Release
}

sealed class KeyCode {
    data class Keyboard(
        val code: Int,
    ): KeyCode()

    data class Mouse(
        val code: Int,
    ): KeyCode()

    fun toDisplayString(): String {
        return when(this) {
            is Keyboard -> NativeKeyEvent.getKeyText(code)
            is Mouse -> "M$code"
        }
    }
}

class KeyHooker: NativeKeyListener, NativeMouseListener {

    private val _pushEventFlow = MutableSharedFlow<KeyEvent>()
    val keyEventFlow = _pushEventFlow.asSharedFlow()

    private fun NativeKeyEvent.toDisplayString(): String {
        return NativeKeyEvent.getKeyText(this.keyCode)
    }

    @OptIn(DelicateCoroutinesApi::class)
    private fun sendEvent(event: KeyEvent) {
        GlobalScope.launch {
            _pushEventFlow.emit(event)
        }
    }

    override fun nativeKeyPressed(nativeEvent: NativeKeyEvent?) {
        nativeEvent ?: return
        sendEvent(
            KeyEvent(
                EventType.Push,
                KeyCode.Keyboard(nativeEvent.keyCode)
            )
        )
    }

    override fun nativeKeyReleased(nativeEvent: NativeKeyEvent?) {
        nativeEvent ?: return
        sendEvent(
            KeyEvent(
                EventType.Release,
                KeyCode.Keyboard(nativeEvent.keyCode)
            )
        )
    }

    private fun NativeMouseEvent.toDisplayString(): String {
        return "M${this.button}"
    }

    override fun nativeMousePressed(nativeEvent: NativeMouseEvent?) {
        nativeEvent ?: return
        sendEvent(
            KeyEvent(
                EventType.Push,
                KeyCode.Mouse(nativeEvent.button)
            )
        )
    }

    override fun nativeMouseReleased(nativeEvent: NativeMouseEvent?) {
        nativeEvent ?: return
        sendEvent(
            KeyEvent(
                EventType.Release,
                KeyCode.Mouse(nativeEvent.button)
            )
        )
    }
}