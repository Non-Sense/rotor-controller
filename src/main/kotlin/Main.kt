import com.charleskorn.kaml.Yaml
import com.fazecast.jSerialComm.SerialPort
import com.github.kwhat.jnativehook.GlobalScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Color
import java.awt.event.MouseEvent
import java.awt.event.MouseListener
import java.awt.event.WindowEvent
import java.awt.event.WindowListener
import java.io.File
import javax.swing.JFrame
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.border.LineBorder
import kotlin.system.exitProcess

class MainWindow(
    flow: SharedFlow<KeyEvent>,
    defaultConfig: Config,
    private val onConfigUpdate: (Config) -> Unit,
    private val onChannelEvent: (ChannelEvent) -> Unit
): JFrame() {

    data class ChannelEvent(
        val channel: Byte,
        val type: EventType
    )

    private var config = defaultConfig
    private var configuringId: Int? = null

    private val pushFlag = defaultConfig.key.indices.map { false }.toMutableList()

    private val keyPanels = (0..3).map { id ->
        KeyPanel(defaultConfig.key.getOrNull(id)?.toKeyCode()?.toDisplayString() ?: "??") { onConfigureClick(id) }
    }

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        title = "rotor controller"
        setSize(600, 600)
        isVisible = true
        layout = null

        keyPanels.forEachIndexed { index, keyPanel ->
            keyPanel.setLocation(50, 110*index + 50)
            contentPane.add(keyPanel)
        }

        CoroutineScope(Dispatchers.Swing).launch {
            flow.onEach {
                handleKeyEvent(it)
            }.collect()
        }

    }

    private fun handleKeyEvent(event: KeyEvent) {
        configuringId?.let { id ->
            keyPanels.getOrNull(id)?.updateLabel(event.code.toDisplayString())
            config = config.copy(
                key = config.key.toMutableList().apply { this[id] = Config.KeyConfig.fromKeyCode(event.code) })
            onConfigUpdate(config)
            configuringId = null
            return
        }

        val pushKeys = config.key.withIndex().filter { it.value.toKeyCode() == event.code }
        if(pushKeys.isEmpty())
            return
        pushKeys.forEach {
            when(event.type) {
                EventType.Push -> {
                    if(pushFlag.getOrNull(it.index) == false) {
                        pushFlag[it.index] = true
                        onKeyPush(it.index)
                    }
                }
                EventType.Release -> {
                    pushFlag[it.index] = false
                    onKeyRelease(it.index)
                }
            }
            keyPanels.getOrNull(it.index)?.updateBackground(event.type == EventType.Push)
        }
    }

    private fun onKeyPush(index: Int) {
        onChannelEvent(ChannelEvent(index.toByte(), EventType.Push))
    }

    private fun onKeyRelease(index: Int) {
        onChannelEvent(ChannelEvent(index.toByte(), EventType.Release))
    }

    private fun onConfigureClick(id: Int) {
        if(configuringId != null)
            return
        configuringId = id
        keyPanels.getOrNull(id)?.updateLabel("??")
    }

    private class KeyPanel(
        defaultLabelText: String,
        onPushConfigure: () -> Unit
    ): JPanel() {
        private val label = JLabel(defaultLabelText).apply {
            setSize(100, 100)
            setLocation(0, 0)
            horizontalAlignment = JLabel.CENTER
            verticalAlignment = JLabel.CENTER
            this.addMouseListener(object: MouseListener {
                override fun mouseClicked(e: MouseEvent?) {
                    onPushConfigure()
                }

                override fun mousePressed(e: MouseEvent?) {
                }

                override fun mouseReleased(e: MouseEvent?) {
                }

                override fun mouseEntered(e: MouseEvent?) {
                }

                override fun mouseExited(e: MouseEvent?) {
                }

            })
        }

        init {
            border = LineBorder(Color.BLACK)
            setSize(100, 100)
            layout = null
            add(label, BorderLayout.NORTH)
        }

        fun updateLabel(text: String) {
            label.text = text
        }

        fun updateBackground(isPush: Boolean) {
            border = if(isPush) {
                LineBorder(Color.RED, 3)
            } else {
                LineBorder(Color.BLACK, 1)
            }
        }
    }

}

fun main() {

    val configFile = File("config.yml")
    val config = if(!configFile.exists()) {
        configFile.writeText(Yaml.default.encodeToString(Config.serializer(), defaultConfig))
        defaultConfig
    } else {
        Yaml.default.decodeFromStream(Config.serializer(), configFile.inputStream())
    }

    if(config.key.size != CONFIG_KEY_NUM) {
        // TODO: dialog
        exitProcess(-1)
    }

    SerialPort.getCommPorts().forEach {
        println("${it.systemPortName} $it")
    }

    if(!GlobalScreen.isNativeHookRegistered()) {
        runCatching {
            GlobalScreen.registerNativeHook()
        }.onFailure {
            it.printStackTrace()
            exitProcess(-1)
        }
    }

    val com = Communicator()
    com.connect(com.getPorts()[1])      // TODO: [[drop down]]
    com.setStrength(1, 16)
//    com.close()

    val listener = KeyHooker()

    GlobalScreen.addNativeKeyListener(listener)
    GlobalScreen.addNativeMouseListener(listener)

    MainWindow(listener.keyEventFlow, config,
        onConfigUpdate = {
            configFile.writeText(Yaml.default.encodeToString(Config.serializer(), it))
        },
        onChannelEvent = {
            val strength: Byte = if(it.type == EventType.Release) 0 else 0xff.toByte()
            com.setStrength(it.channel, strength)
        }
    ).apply {
        addWindowListener(object :WindowListener {
            override fun windowOpened(e: WindowEvent?) {
            }

            override fun windowClosing(e: WindowEvent?) {
                com.close()
                println("port close")
            }

            override fun windowClosed(e: WindowEvent?) {
            }

            override fun windowIconified(e: WindowEvent?) {
            }

            override fun windowDeiconified(e: WindowEvent?) {
            }

            override fun windowActivated(e: WindowEvent?) {
            }

            override fun windowDeactivated(e: WindowEvent?) {
            }

        })
    }

}