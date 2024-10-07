package com.sihiver.aiotunneling.util

import com.jcraft.jsch.*
import com.sihiver.aiotunneling.ui.account.Profile
import com.sihiver.aiotunneling.ui.home.SSHState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.concurrent.thread

object Connection {
    private var session: Session? = null
    private var serverSocket: ServerSocket? = null
    private val activeConnections = ConcurrentHashMap<Socket, Long>()
    private const val cleanupInterval = 60000L // 1 menit
    private var isRunning = true

    suspend fun startTunnel(profile: Profile, sshState: SSHState) {
        withContext(Dispatchers.IO) {
            val jsch = JSch()
            try {
                LogManager.addLog("Memulai sesi SSH ke ${profile.server}:${profile.port}")
                session = jsch.getSession(profile.username, profile.server, profile.port)
                session?.setPassword(profile.password)
                session?.setConfig("StrictHostKeyChecking", "no")
                session?.setConfig("TCPKeepAlive", "yes")
                session?.setConfig("ServerAliveInterval", "60")
                session?.setConfig("ServerAliveCountMax", "3")
                session?.timeout = 30000

                // Menambahkan konfigurasi tambahan berdasarkan sshState
                if (sshState.usePyloadSSL) {
                    LogManager.addLog("Menggunakan Pyload SSL")
                    // Tambahkan konfigurasi untuk Pyload SSL
                }
                if (sshState.slowDNS) {
                    LogManager.addLog("Menggunakan SlowDNS")
                    // Tambahkan konfigurasi untuk SlowDNS
                }
                if (sshState.v2ray) {
                    LogManager.addLog("Menggunakan V2Ray")
                    // Tambahkan konfigurasi untuk V2Ray
                }

                connectSshSession()

                // Jalankan SOCKS5 proxy
                startSocks5Proxy()

                // Jalankan thread pembersihan
                isRunning = true
                thread {
                    while (isRunning) {
                        Thread.sleep(cleanupInterval)
                        cleanupInactiveConnections()
                    }
                }

            } catch (e: Exception) {
                LogManager.addLog("Error memulai SSH Tunnel: ${e.message}")
            }
        }
    }

    private fun connectSshSession() {
        try {
            if (session?.isConnected != true) {
                session?.connect()
                LogManager.addLog("SSH tunnel terhubung ke ${session?.host}:${session?.port}")
                
                // Log HostKey information
                val jsch = JSch()
                val hkr = session?.hostKeyRepository
                val hks = hkr?.hostKey
                if (hks != null) {
                    LogManager.addLog("Host keys in ${hkr.knownHostsRepositoryID}")
                    for (hk in hks) {
                        LogManager.addLog("${hk.host} ${hk.type} ${hk.getFingerPrint(jsch)}")
                    }
                } else {
                    LogManager.addLog("No host keys found")
                }
                
                // Log session's HostKey
                val sessionHostKey = session?.hostKey
                LogManager.addLog("HostKey: ${sessionHostKey?.host} ${sessionHostKey?.type} ${sessionHostKey?.getFingerPrint(jsch)}")
            }
        } catch (e: Exception) {
            LogManager.addLog("Gagal terhubung ke SSH server: ${e.message}")
            Thread.sleep(5000)
            connectSshSession()
        }
    }

    private fun startSocks5Proxy() {
        serverSocket = ServerSocket(1080)
        LogManager.addLog("SOCKS5 proxy berjalan di port 1080")
        isRunning = true
        thread {
            while (isRunning) {
                try {
                    val clientSocket = serverSocket?.accept() ?: break
                    activeConnections[clientSocket] = System.currentTimeMillis()
                    thread { handleClient(clientSocket) }
                } catch (e: IOException) {
                    LogManager.addLog("Kesalahan saat menerima koneksi: ${e.message}")
                }
            }
        }
    }

    private fun handleClient(clientSocket: Socket) {
        val totalBytesTransferred = AtomicLong(0)

        try {
            clientSocket.soTimeout = 30000 // Set timeout 30 detik
            val input = clientSocket.getInputStream()
            val output = clientSocket.getOutputStream()

            // Handle SOCKS5 Handshake
            if (!handleSocks5Handshake(input, output)) {
                LogManager.addLog("Handshake SOCKS5 gagal")
                return
            }

            // Read SOCKS5 Request
            val version = input.read()
            if (version != 5) {
                LogManager.addLog("Versi SOCKS5 tidak didukung: $version")
                output.write(byteArrayOf(5, 0xFF.toByte()))
                output.flush()
                return
            }

            val cmd = input.read()
            if (cmd != 1) { // CONNECT command
                LogManager.addLog("Perintah SOCKS5 tidak didukung: $cmd")
                output.write(byteArrayOf(5, 7, 0, 1, 0, 0, 0, 0, 0, 0))
                output.flush()
                return
            }

            val rsv = input.read()
            if (rsv != 0) {
                LogManager.addLog("Reserved byte SOCKS5 tidak nol: $rsv")
                return
            }

            val addressType = input.read()
            val targetAddress = readTargetAddress(input, addressType)
            val targetPort = readTargetPort(input)

            // Kirim respons SOCKS5 sukses
            output.write(byteArrayOf(5, 0, 0, addressType.toByte()) + buildBoundAddressBytes(targetAddress, addressType) + buildBoundPortBytes(targetPort))
            output.flush()

            // Forward data through SSH
            forwardThroughSSH(clientSocket, targetAddress, targetPort, totalBytesTransferred)

        } catch (e: IOException) {
            if (e.message?.contains("Connection reset") == true || e.message?.contains("Broken pipe") == true) {
                LogManager.addLog("Koneksi terputus: ${clientSocket.inetAddress.hostAddress}:${clientSocket.port}")
            } else {
                LogManager.addLog("Kesalahan saat menangani klien: ${e.message}")
            }
        } catch (e: Exception) {
            LogManager.addLog("Kesalahan tidak terduga saat menangani klien: ${e.message}")
        } finally {
            closeClientSocket(clientSocket)
        }
    }

    private fun handleSocks5Handshake(input: InputStream, output: OutputStream): Boolean {
        val version = input.read()
        if (version == -1) {
            LogManager.addLog("Koneksi ditutup sebelum handshake selesai")
            return false
        }
        if (version != 5) {
            LogManager.addLog("Versi SOCKS tidak didukung: $version")
            output.write(byteArrayOf(5, 0xFF.toByte()))
            output.flush()
            return false
        }

        val methodsCount = input.read()
        if (methodsCount == -1) {
            LogManager.addLog("Koneksi ditutup saat membaca jumlah metode")
            return false
        }
        val methods = ByteArray(methodsCount)
        val bytesRead = input.read(methods)
        if (bytesRead != methodsCount) {
            LogManager.addLog("Tidak dapat membaca semua metode")
            return false
        }

        // Pilih metode "No Authentication" (0) jika tersedia
        if (methods.contains(0.toByte())) {
            output.write(byteArrayOf(5, 0))
            output.flush()
            LogManager.addLog("Handshake SOCKS5 berhasil, memilih metode No Authentication")
            return true
        } else {
            LogManager.addLog("Tidak ada metode autentikasi yang didukung")
            output.write(byteArrayOf(5, 0xFF.toByte()))
            output.flush()
            return false
        }
    }

    private fun readTargetAddress(input: InputStream, addressType: Int): String {
        return when (addressType) {
            1 -> { // IPv4
                val ipBytes = readFully(input, 4)
                ipBytes.joinToString(".") { it.toUByte().toString() }
            }
            3 -> { // Domain name
                val domainLength = input.read()
                if (domainLength <= 0) throw IOException("Panjang domain tidak valid: $domainLength")
                val domainBytes = readFully(input, domainLength)
                String(domainBytes)
            }
            4 -> { // IPv6
                val ipBytes = readFully(input, 16)
                buildString {
                    for (i in ipBytes.indices step 2) {
                        if (i > 0) append(":")
                        val segment = ((ipBytes[i].toInt() and 0xFF) shl 8) or (ipBytes[i + 1].toInt() and 0xFF)
                        append(String.format("%x", segment))
                    }
                }
            }
            else -> throw IOException("Tipe alamat tidak didukung: $addressType")
        }
    }

    private fun readTargetPort(input: InputStream): Int {
        val portBytes = readFully(input, 2)
        return ((portBytes[0].toInt() and 0xFF) shl 8) or (portBytes[1].toInt() and 0xFF)
    }

    private fun readFully(input: InputStream, length: Int): ByteArray {
        val data = ByteArray(length)
        var offset = 0
        while (offset < length) {
            val bytesRead = input.read(data, offset, length - offset)
            if (bytesRead == -1) throw IOException("End of stream reached with $offset bytes read; expected $length bytes")
            offset += bytesRead
        }
        return data
    }

    private fun buildBoundAddressBytes(address: String, addressType: Int): ByteArray {
        return when (addressType) {
            1 -> { // IPv4
                val parts = address.split(".").map { it.toInt() }
                byteArrayOf(parts[0].toByte(), parts[1].toByte(), parts[2].toByte(), parts[3].toByte())
            }
            3 -> { // Domain name
                val domainBytes = address.toByteArray()
                byteArrayOf(domainBytes.size.toByte()) + domainBytes
            }
            4 -> { // IPv6
                val parts = address.split(":")
                val ipBytes = ByteArray(16)
                for (i in parts.indices) {
                    val part = parts[i].toInt(16)
                    ipBytes[i * 2] = ((part shr 8) and 0xFF).toByte()
                    ipBytes[i * 2 + 1] = (part and 0xFF).toByte()
                }
                ipBytes
            }
            else -> ByteArray(0)
        }
    }

    private fun buildBoundPortBytes(port: Int): ByteArray {
        return byteArrayOf((port shr 8).toByte(), (port and 0xFF).toByte())
    }

    private fun forwardThroughSSH(clientSocket: Socket, targetAddress: String, targetPort: Int, totalBytesTransferred: AtomicLong) {
        var channel: ChannelDirectTCPIP? = null

        try {
            channel = session?.openChannel("direct-tcpip") as ChannelDirectTCPIP
            channel.setHost(targetAddress)
            channel.setPort(targetPort)
            channel.connect(10000)

            val buffer = ByteArray(32768)

            val clientToServer = thread {
                try {
                    clientSocket.getInputStream().use { input ->
                        channel.outputStream.use { output ->
                            while (!clientSocket.isClosed && channel.isConnected) {
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                output.flush()
                                totalBytesTransferred.addAndGet(bytesRead.toLong())
                            }
                        }
                    }
                } catch (e: InterruptedIOException) {
                    LogManager.addLog("Thread klien ke server diinterupsi")
                } catch (e: IOException) {
                    LogManager.addLog("Kesalahan saat meneruskan data dari klien ke server: ${e.message}")
                }
            }

            val serverToClient = thread {
                try {
                    channel.inputStream.use { input ->
                        clientSocket.getOutputStream().use { output ->
                            while (!clientSocket.isClosed && channel.isConnected) {
                                val bytesRead = input.read(buffer)
                                if (bytesRead == -1) break
                                output.write(buffer, 0, bytesRead)
                                output.flush()
                                totalBytesTransferred.addAndGet(bytesRead.toLong())
                            }
                        }
                    }
                } catch (e: InterruptedIOException) {
                    LogManager.addLog("Thread server ke klien diinterupsi")
                } catch (e: IOException) {
                    LogManager.addLog("Kesalahan saat meneruskan data dari server ke klien: ${e.message}")
                }
            }

            clientToServer.join()
            serverToClient.join()

        } catch (e: Exception) {
            LogManager.addLog("Kesalahan saat membuat koneksi SSH ke $targetAddress:$targetPort: ${e.message}")
        } finally {
            channel?.disconnect()
            clientSocket.close()
            activeConnections.remove(clientSocket)
        }
    }

    private fun closeClientSocket(clientSocket: Socket) {
        try {
            if (!clientSocket.isClosed) {
                clientSocket.close()
            }
        } catch (e: IOException) {
            LogManager.addLog("Kesalahan saat menutup socket klien: ${e.message}")
        } finally {
            activeConnections.remove(clientSocket)
        }
    }

    private fun cleanupInactiveConnections() {
        val currentTime = System.currentTimeMillis()
        activeConnections.entries.removeIf { (socket, lastActivity) ->
            if (currentTime - lastActivity > 300000) { // 5 menit tidak aktif
                try {
                    socket.close()
                } catch (e: IOException) {
                    LogManager.addLog("Kesalahan saat menutup socket tidak aktif: ${e.message}")
                }
                true
            } else {
                false
            }
        }
    }

    suspend fun stopTunnel() {
        withContext(Dispatchers.IO) {
            LogManager.addLog("Menghentikan SOCKS5 proxy")
            serverSocket?.close()
            LogManager.addLog("Memutuskan sesi SSH")
            session?.disconnect()
            LogManager.addLog("SSH Tunnel dihentikan")
            isRunning = false
        }
    }
}