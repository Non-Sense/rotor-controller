import com.fazecast.jSerialComm.SerialPort
import kotlinx.coroutines.*

class Communicator {

    private var serialPort: SerialPort? = null

    var onErrorDisconnected = {}

    data class PortInfo(
        val displayName: String,
        val portDescriptor: String
    ) {
        override fun toString(): String {
            return displayName
        }
    }

    fun getPorts(): List<PortInfo> {
        return SerialPort.getCommPorts().map {
            PortInfo("[${it.systemPortName}] $it", it.systemPortName)
        }
    }

    fun connect(portInfo: PortInfo): Boolean {
        kotlin.runCatching {
            serialPort = SerialPort.getCommPort(portInfo.portDescriptor)
            serialPort?.let {
                it.baudRate = 115200
                it.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_READ_BLOCKING, 100, 100)
                it.openPort()
                if(validate()) {
                    startCheckKeepAlive()
                    return true
                }
                it.closePort()
            }
        }
        serialPort = null
        return false
    }

    private var job: Job? = null
    private fun startCheckKeepAlive() {
        job?.cancel()
        job = CoroutineScope(Dispatchers.Default).launch {
            while(this.isActive) {
                delay(100)
                if(!keepAlive())
                    onError()
            }
        }
    }

    fun setStrength(channel: Byte, strength: Byte) {
        kotlin.runCatching {
            serialPort?.let {
                if(!it.isOpen)
                    return
                val command = "@s".encodeToByteArray().plus(channel).plus(strength)
                if(it.writeBytes(command, command.size.toLong()) <= 0)
                    onError()

                val receive = ByteArray(4)
                if(it.readBytes(receive, 4L) <= 0)
                    onError()
                println(receive.decodeToString())
            }
        }.onFailure {
            it.printStackTrace()
        }
    }

    private fun onError() {
        job?.cancel()
        job = null
        close()
        onErrorDisconnected()
        println("error")
    }

    private fun validate(): Boolean {
        serialPort?.let {
            val command = "@?".encodeToByteArray()
            val exceptReceive = "*!".encodeToByteArray()
            it.writeBytes(command, command.size.toLong())
            val receive = ByteArray(2)
            it.readBytes(receive, 2L)
            return receive.contentEquals(exceptReceive)
        }

        return false
    }

    private fun keepAlive(): Boolean {
        serialPort?.let {
            val command = "@k".encodeToByteArray()
            val exceptReceive = "kkkk".encodeToByteArray()
            if(it.writeBytes(command, command.size.toLong()) <= 0)
                return false
            val receive = ByteArray(4)
            if(it.readBytes(receive, 4L) <= 0)
                return false
            return receive.contentEquals(exceptReceive)
        }
        return false
    }

    fun close() {
        job?.cancel()
        job = null
        if(serialPort?.isOpen == false)
            return
        kotlin.runCatching {
            serialPort?.closePort()
        }
    }


}