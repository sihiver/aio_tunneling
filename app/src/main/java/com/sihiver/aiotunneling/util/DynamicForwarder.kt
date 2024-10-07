package com.sihiver.aiotunneling.util

import com.jcraft.jsch.ChannelDirectTCPIP
import com.jcraft.jsch.Session
import jsocks.socks.ProxyServer
import jsocks.socks.server.ServerAuthenticatorNone
import kotlinx.coroutines.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.lang.reflect.Field
import java.net.InetAddress
import java.net.Socket

class DynamicForwarder(private val port: Int, session: Session) : Runnable {

    private inner class SocksProxy(private val session: Session) : ProxyServer(ServerAuthenticatorNone()) {
        override fun onConnect(msg: jsocks.socks.ProxyMessage) {
            coroutineScope.launch {
                handleConnect(msg)
            }
        }

        private fun handleConnect(msg: jsocks.socks.ProxyMessage) {
            LogManager.addLog("Membuka channel direct-tcpip ke ${msg.host}:${msg.port}")
            val channel = session.openChannel("direct-tcpip") as ChannelDirectTCPIP
            channel.setHost(msg.host)
            channel.setPort(msg.port)
            channel.connect()

            LogManager.addLog("Memulai pipe untuk koneksi")
            val socket = getProxySocket()
            if (socket != null) {
                this@DynamicForwarder.startPipe(
                    socket.inputStream,
                    channel.inputStream,
                    channel.outputStream,
                    socket.outputStream
                )
            } else {
                LogManager.addLog("Gagal mendapatkan proxySocket")
            }
        }

        private fun getProxySocket(): Socket? {
            return try {
                val field: Field = ProxyServer::class.java.getDeclaredField("sock")
                field.isAccessible = true
                field.get(this) as? Socket
            } catch (e: Exception) {
                LogManager.addLog("Error mengakses sock: ${e.message}")
                null
            }
        }
    }

    private val proxy = SocksProxy(session)
    private var serverThread: Thread? = null
    private var isRunning = false
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        LogManager.addLog("DynamicForwarder diinisialisasi pada port $port")
        serverThread = Thread(this, "DynamicForwarder")
    }

    override fun run() {
        try {
            LogManager.addLog("Memulai SOCKS proxy pada 127.0.0.1:$port")
            isRunning = true
            proxy.start(port, 5, InetAddress.getByName("127.0.0.1"))
        } catch (e: IOException) {
            LogManager.addLog("Error di DynamicForwarder: ${e.message}")
        } finally {
            isRunning = false
        }
    }

    fun start() {
        if (!isRunning) {
            serverThread?.start()
        }
    }

    fun stop() {
        LogManager.addLog("Menghentikan SOCKS proxy")
        isRunning = false
        proxy.stop()
        serverThread?.interrupt()
        coroutineScope.cancel()  // Cancel semua coroutine
        LogManager.addLog("DynamicForwarder dihentikan")
    }

    private fun startPipe(input1: InputStream, input2: InputStream, output1: OutputStream, output2: OutputStream) {
        val buffer = ByteArray(1024)
        coroutineScope.launch {
            try {
                var read: Int
                while (isActive) {
                    read = input1.read(buffer)
                    if (read == -1) break
                    output1.write(buffer, 0, read)
                    output1.flush()
                }
            } catch (e: IOException) {
                LogManager.addLog("Error in pipe input1 to output1: ${e.message}")
            }
        }

        coroutineScope.launch {
            try {
                var read: Int
                while (isActive) {
                    read = input2.read(buffer)
                    if (read == -1) break
                    output2.write(buffer, 0, read)
                    output2.flush()
                }
            } catch (e: IOException) {
                LogManager.addLog("Error in pipe input2 to output2: ${e.message}")
            }
        }
    }
}