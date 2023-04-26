import kotlinx.serialization.Serializable

@Serializable
data class Config(
    val version: Int = 1,
    val key: List<KeyConfig>,
    val strength: List<Int>
) {
    @Serializable
    data class KeyConfig(
        val type: String,
        val code: Int
    ) {
        companion object {
            fun fromKeyCode(code: KeyCode): KeyConfig {
                return when(code) {
                    is KeyCode.Keyboard -> KeyConfig(type = "k", code = code.code)
                    is KeyCode.Mouse -> KeyConfig(type = "m", code = code.code)
                }
            }
        }

        fun toKeyCode(): KeyCode {
            if(type == "k")
                return KeyCode.Keyboard(code)
            if(type == "m")
                return KeyCode.Mouse(code)
            throw IllegalArgumentException("謎のフォーマット")
        }
    }
}

val defaultConfig = Config(
    key = listOf(
        Config.KeyConfig(
            type = "k",
            code = 17   // W
        ),
        Config.KeyConfig(
            type = "k",
            code = 30   // A
        ),
        Config.KeyConfig(
            type = "k",
            code = 29   // Ctrl
        ),
        Config.KeyConfig(
            type = "m",
            code = 1    // M1
        )
    ),
    strength = listOf(
        255,
        255,
        195,
        195
    )
)
const val CONFIG_KEY_NUM = 4