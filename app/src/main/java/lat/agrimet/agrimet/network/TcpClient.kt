package lat.agrimet.agrimet.network

import io.ktor.network.selector.*
import io.ktor.network.sockets.*
import io.ktor.utils.io.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CancellationException

class TcpClient(
    private val host: String,
    private val port: Int,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val selector = SelectorManager(Dispatchers.IO)
    private var socket: Socket? = null
    private var input: ByteReadChannel? = null
    private var output: ByteWriteChannel? = null

    // Asynchronous  flux of the socket variable
    private val _incoming = MutableSharedFlow<String>(extraBufferCapacity = 64)
    val incoming: SharedFlow<String> = _incoming.asSharedFlow()

    // Conexión con reintentos exponenciales
    fun start() = scope.launch {
        var attempt = 0
        while (isActive) {
            try {
                socket = aSocket(selector).tcp().connect(host, port) {
                    // Opcional: ajustar opciones del socket
                    // socketTimeout = 15_000
                    // keepAlive = true
                    // noDelay = true
                }
                input = socket!!.openReadChannel()
                output = socket!!.openWriteChannel(autoFlush = true)
                attempt = 0 // resetea el contador al conectar

                // Lector asíncrono
                readLoop()
            } catch (e: CancellationException) {
                break
            } catch (e: Throwable) {
                // Falló la conexión o se cayó: backoff exponencial
                attempt++
                val wait = (minOf(60, (1 shl (attempt - 1))).seconds)
                // Loguea y reintenta
                // println("TCP: desconectado (${e.message}). Reintentando en $wait…")
                delay(wait)
            } finally {
                closeQuietly()
            }
        }
    }

    private suspend fun readLoop() {
        val ch = input ?: return
        while (!ch.isClosedForRead) {
            val line = ch.readUTF8Line() ?: break
            _incoming.emit(line)
        }
        // Si sales del bucle, forzamos excepción para que el start() reintente
        throw IllegalStateException("Socket cerrado por remoto")
    }

    // Enviar una línea (se añade \n)
    suspend fun sendLine(text: String) {
        val out = output ?: throw IllegalStateException("No conectado")
        out.writeStringUtf8(text)
        out.writeStringUtf8("\n")
        out.flush()
    }

    fun stop() {
        scope.cancel()
        closeQuietly()
    }

    private fun closeQuietly() {
        //try { output?.close() } catch (_: Throwable) {}
        try { output?.close() } catch (_: Throwable) {}
        try { input?.cancel() } catch (_: Throwable) {}
        try { socket?.close() } catch (_: Throwable) {}
        output = null; input = null; socket = null
    }
}