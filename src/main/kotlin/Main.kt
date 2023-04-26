import com.charleskorn.kaml.Yaml
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
import java.util.*
import javax.swing.*
import javax.swing.border.LineBorder
import kotlin.system.exitProcess

class MainWindow(
    flow: SharedFlow<KeyEvent>,
    defaultConfig: Config,
    private val portInfoGetter: () -> List<Communicator.PortInfo>,
    private val onConfigUpdate: (Config) -> Unit,
    private val onChannelEvent: (ChannelEvent) -> Unit,
    private val onConnect: (Communicator.PortInfo) -> Boolean,
    private val onClose: () -> Unit
): JFrame() {

    data class ChannelEvent(
        val channel: Byte,
        val type: EventType,
        val strength: Byte
    )

    private var config = defaultConfig
    private var configuringId: Int? = null

    private val pushFlag = defaultConfig.key.indices.map { false }.toMutableList()

    private val keyPanels: List<KeyPanel>

    private val dropDown: JComboBox<Communicator.PortInfo>
    private val connectButton: JButton

    private val sliders: List<JSlider>

    private var isConnected = false

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        title = "rotor controller"
        setSize(600, 600)
        isVisible = true
        layout = null

        keyPanels = (0..3).map { id ->
            KeyPanel(defaultConfig.key.getOrNull(id)?.toKeyCode()?.toDisplayString() ?: "??") { onConfigureClick(id) }
        }

        dropDown = JComboBox(portInfoGetter().toTypedArray()).apply {
            setSize(200, 30)
            setLocation(50, 30)
        }
        contentPane.add(dropDown)

        connectButton = JButton("接続").apply {
            setSize(70, 30)
            setLocation(255, 30)
            addActionListener {
                if(isConnected) {
                    onClose()
                    onDisconnected()
                } else {
                    tryConnect((dropDown.selectedItem) as Communicator.PortInfo)
                }
            }
        }
        contentPane.add(connectButton)

        sliders = (0..3).map { index ->
            val max = if(index>=2) 195 else 255     // 170???
            val min = if(index>=2) 60 else 70
            JSlider(min, max).apply {
                majorTickSpacing = (max-min)/5
                paintTicks = true
                setSize(275, 50)
                setLocation(180, index*110 + 120)
                labelTable = Hashtable<Int, JLabel>().apply {
                    put(min, JLabel("よわい"))
                    put(max, JLabel("つよい"))
                }
                val v = (defaultConfig.strength.getOrNull(index) ?: max)
                value = if(v in (0..max)) v else max
                paintLabels = true
            }.also {
                contentPane.add(it)
            }
        }

        keyPanels.forEachIndexed { index, keyPanel ->
            keyPanel.setLocation(50, 110*index + 70)
            contentPane.add(keyPanel)
        }

        CoroutineScope(Dispatchers.Swing).launch {
            flow.onEach {
                handleKeyEvent(it)
            }.collect()
        }
        this.repaint()
    }

    fun onDisconnected() {
        isConnected = false
        connectButton.text = "接続"
        dropDown.isEnabled = true
        repaint()
    }

    fun getSliderValues(): List<Int> {
        return sliders.map { it.value }
    }

    fun getCurrentConfig(): Config = config

    fun reconnect() {
        tryConnect(dropDown.selectedItem as Communicator.PortInfo, false)
    }

    private fun tryConnect(portInfo: Communicator.PortInfo, showDialog: Boolean = true) {
        if(!onConnect(portInfo)) {
            isConnected = false
            if(showDialog)
                JOptionPane.showMessageDialog(null, "なんか！接続できなかった！！", "ごめん！！", JOptionPane.ERROR_MESSAGE)
            return
        }
        isConnected = true
        connectButton.text = "切断"
        dropDown.isEnabled = false
        (0..3).forEach { onKeyRelease(it) }
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
        onChannelEvent(ChannelEvent(index.toByte(), EventType.Push, sliders[index].value.toByte()))
    }

    private fun onKeyRelease(index: Int) {
        onChannelEvent(ChannelEvent(index.toByte(), EventType.Release, 0))
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
        JOptionPane.showMessageDialog(
            null,
            "なんか！設定ファイルがぶっ壊れてる！！\n設定消えちゃうけどconfig.yml消してみて！！",
            "ごめん！！",
            JOptionPane.ERROR_MESSAGE
        )
        exitProcess(-1)
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

    val listener = KeyHooker()

    GlobalScreen.addNativeKeyListener(listener)
    GlobalScreen.addNativeMouseListener(listener)

    val mainWindow = MainWindow(
        flow = listener.keyEventFlow,
        defaultConfig = config,
        portInfoGetter = {
            com.getPorts()
        },
        onConnect = {
            com.close()
            com.connect(it)
        },
        onClose = {
            com.close()
        },
        onConfigUpdate = {
            configFile.writeText(Yaml.default.encodeToString(Config.serializer(), it))
        },
        onChannelEvent = {
            //val strength: Byte = if(it.type == EventType.Release) 0 else 168.toByte()
            com.setStrength(it.channel, it.strength)
        }
    )

    com.onErrorDisconnected = {
        mainWindow.onDisconnected()
        mainWindow.reconnect()
    }

    mainWindow.addWindowListener(object: WindowListener {
        override fun windowOpened(e: WindowEvent?) {
        }

        override fun windowClosing(e: WindowEvent?) {
            com.close()
            val values = mainWindow.getSliderValues()
            val newConfig = mainWindow.getCurrentConfig().copy(strength = values)
            configFile.writeText(Yaml.default.encodeToString(Config.serializer(), newConfig))
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