package mala.ixin

interface CommandHandler {
    fun handle(command: String, source: Any)
}
