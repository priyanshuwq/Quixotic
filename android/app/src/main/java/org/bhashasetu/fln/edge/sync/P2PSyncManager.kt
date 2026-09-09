package org.bhashasetu.fln.edge.sync

import android.content.Context
import android.net.wifi.p2p.*
import android.os.Handler
import android.os.Looper
import java.io.*
import java.net.ServerSocket
import java.net.Socket

class P2PSyncManager(private val context: Context) {
    private var serverSocket: ServerSocket? = null
    private var isRunning = false
    private val handler = Handler(Looper.getMainLooper())

    fun startSyncServer(port: Int = 8080, onSyncComplete: (String) -> Unit) {
        Thread {
            try {
                serverSocket = ServerSocket(port)
                isRunning = true
                while (isRunning) {
                    val clientSocket = serverSocket?.accept() ?: continue
                    handleClient(clientSocket, onSyncComplete)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    private fun handleClient(socket: Socket, onSyncComplete: (String) -> Unit) {
        Thread {
            try {
                val inputStream = ObjectInputStream(socket.getInputStream())
                val request = inputStream.readObject() as SyncRequest
                val response = SyncResponse(
                    studentId = request.studentId,
                    audioFiles = emptyList(),
                    textSummaries = emptyList(),
                    timestamp = System.currentTimeMillis()
                )
                val outputStream = ObjectOutputStream(socket.getOutputStream())
                outputStream.writeObject(response)
                outputStream.flush()
                socket.close()
                handler.post { onSyncComplete("Sync completed for: ${request.studentId}") }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }.start()
    }

    fun stopSyncServer() {
        isRunning = false
        serverSocket?.close()
    }

    data class SyncRequest(val studentId: String, val lastSyncTimestamp: Long) : java.io.Serializable
    data class SyncResponse(val studentId: String, val audioFiles: List<String>, val textSummaries: List<String>, val timestamp: Long) : java.io.Serializable
}
