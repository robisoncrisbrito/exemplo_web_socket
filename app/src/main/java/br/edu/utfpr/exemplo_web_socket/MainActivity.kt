package br.edu.utfpr.exemplo_web_socket

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString

class MainActivity : AppCompatActivity() {

    private lateinit var etMessage: EditText
    private lateinit var btnSend: Button
    private lateinit var btnConnect: Button
    private lateinit var btnDisconnect: Button
    private lateinit var tvLog: TextView

    private var webSocket: WebSocket? = null
    private val okHttpClient = OkHttpClient()

    // URL do servidor WebSocket de eco
    private val ECHO_SERVER_URL = "wss://echo.websocket.org" // ou "ws://echo.websocket.org" para não seguro

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        btnConnect = findViewById(R.id.btnConnect)
        btnDisconnect = findViewById(R.id.btnDisconnect)
        tvLog = findViewById(R.id.tvLog)

        btnConnect.setOnClickListener {
            connectWebSocket()
        }

        btnDisconnect.setOnClickListener {
            disconnectWebSocket()
        }

        btnSend.setOnClickListener {
            val message = etMessage.text.toString()
            if (message.isNotEmpty()) {
                sendMessage(message)
                etMessage.text.clear()
            }
        }

        // Inicialmente, desabilitar botões que dependem da conexão
        btnSend.isEnabled = false
        btnDisconnect.isEnabled = false

    }

    private fun connectWebSocket() {
        if (webSocket != null) {
            logToScreen("Já conectado ou tentando conectar.")
            return
        }

        val request = Request.Builder()
            .url(ECHO_SERVER_URL)
            .build()

        logToScreen("Tentando conectar a $ECHO_SERVER_URL...")

        webSocket = okHttpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                this@MainActivity.webSocket = webSocket // Armazena a instância do WebSocket
                logToScreenAndUpdateUI("Conectado!", true)
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                logToScreen("Recebido (texto): $text")
            }

            override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
                super.onMessage(webSocket, bytes)
                logToScreen("Recebido (bytes): ${bytes.hex()}")
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosing(webSocket, code, reason)
                logToScreen("Fechando: $code / $reason")
                // Aqui você pode tentar fechar o WebSocket do lado do cliente se necessário
                // webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                logToScreenAndUpdateUI("Desconectado: $code / $reason", false)
                this@MainActivity.webSocket = null // Limpa a referência
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                logToScreenAndUpdateUI("Falha na conexão: ${t.message}", false)
                this@MainActivity.webSocket = null // Limpa a referência
            }
        })
    }

    private fun sendMessage(message: String) {
        webSocket?.let {
            if (it.send(message)) {
                logToScreen("Enviado: $message")
            } else {
                logToScreen("Falha ao enviar: $message (fila cheia ou WebSocket fechado)")
            }
        } ?: logToScreen("Não conectado para enviar mensagem.")
    }

    private fun disconnectWebSocket() {
        webSocket?.close(1000, "Desconexão solicitada pelo usuário.")
        // O callback onClosed será chamado quando a desconexão for concluída.
    }

    private fun logToScreen(message: String) {
        // Usar lifecycleScope para garantir que a atualização da UI ocorra no thread principal
        lifecycleScope.launch(Dispatchers.Main) {
            val currentLog = tvLog.text.toString()
            tvLog.text = "$currentLog\n$message"
        }
    }

    private fun logToScreenAndUpdateUI(message: String, isConnected: Boolean) {
        lifecycleScope.launch(Dispatchers.Main) {
            logToScreen(message)
            btnConnect.isEnabled = !isConnected
            btnDisconnect.isEnabled = isConnected
            btnSend.isEnabled = isConnected
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Importante fechar o cliente OkHttp para liberar recursos,
        // especialmente se você tiver timeouts longos ou muitas conexões.
        // O fechamento do WebSocket individual é tratado no disconnectWebSocket.
        okHttpClient.dispatcher.executorService.shutdown()
        // Se você quiser cancelar todas as chamadas imediatamente:
        // okHttpClient.dispatcher.cancelAll()
    }


}