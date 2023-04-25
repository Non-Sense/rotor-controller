import com.fazecast.jSerialComm.SerialPort

class Communicator {

    private var serialPort: SerialPort? = null

    data class PortInfo(
        val displayName: String,
        val portDescriptor: String
    )

    fun getPorts(): List<PortInfo> {
        return SerialPort.getCommPorts().map {
            PortInfo("[${it.systemPortName}] $it",it.systemPortName)
        }
    }

    fun connect(portInfo: PortInfo): Boolean {
        kotlin.runCatching {
            serialPort = SerialPort.getCommPort(portInfo.portDescriptor)
            serialPort?.let {
                it.baudRate = 115200
                it.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING or SerialPort.TIMEOUT_READ_BLOCKING, 100, 100)
                it.openPort()
                if(validate())
                    return true
                it.closePort()
            }
        }
        serialPort = null
        return false
    }

    fun setStrength(channel: Byte, strength: Byte) {
        kotlin.runCatching {
            serialPort?.let {
                val command = "@s".encodeToByteArray().plus(channel).plus(strength)
                it.writeBytes(command, command.size.toLong())

                val receive = ByteArray(4)
                it.readBytes(receive, 4L)
                println(receive.decodeToString())
            }
        }.onFailure {
            it.printStackTrace()
        }
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

    fun close() {
        kotlin.runCatching {
            serialPort?.closePort()
        }
    }


}