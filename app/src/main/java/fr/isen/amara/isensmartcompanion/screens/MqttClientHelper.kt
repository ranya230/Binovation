// FICHIER: MqttClientHelper.kt
// PACKAGE: fr.isen.amara.isensmartcompanion.screens
package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import android.util.Log
import kotlinx.coroutines.*
import org.eclipse.paho.client.mqttv3.*
import javax.net.ssl.SSLSocketFactory

/**
 * Helper MQTT simple et propre (compatible avec l'ancien code).
 *
 * - connect() : ouvre la connexion (idempotent)
 * - subscribe(topic) : s'abonne et remonte les messages via un callback
 * - connectAndSubscribe(topic) : connect + subscribe (pratique pour l'ancien code)
 * - publish(topic, payload)
 * - unsubscribe(topic)
 * - disconnect()
 *
 * Broker par défaut : test.mosquitto.org en TLS (port 8883).
 * En DEV, on utilise un SSLSocketFactory qui accepte tous les certificats
 * (TrustAllSocketFactory.socketFactory()). NE PAS UTILISER EN PROD.
 */
class MqttClientHelper(private val context: Context) {

    // === Paramètres broker ===
    private val serverUri = "ssl://test.mosquitto.org:8883"
    private val clientId = MqttClient.generateClientId()

    // === Etat client & coroutines ===
    private var client: MqttClient? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Registre des callbacks par topic
    private val listeners = mutableMapOf<String, (String) -> Unit>()

    /**
     * Connexion idempotente. Appelle [onConnected] sur le Main thread quand prêt.
     */
    fun connect(
        socketFactory: SSLSocketFactory = TrustAllSocketFactory.socketFactory(),
        onConnected: (() -> Unit)? = null
    ) {
        scope.launch {
            try {
                if (client == null) {
                    client = MqttClient(serverUri, clientId, /*persistence*/ null)
                }
                val c = client!!

                if (!c.isConnected) {
                    val options = MqttConnectOptions().apply {
                        isCleanSession = true
                        isAutomaticReconnect = true
                        connectionTimeout = 10
                        keepAliveInterval = 20
                        this.socketFactory = socketFactory // ✅ ICI on passe bien un SSLSocketFactory
                    }

                    // Router tous les messages vers le bon callback de topic
                    c.setCallback(object : MqttCallback {
                        override fun connectionLost(cause: Throwable?) {
                            Log.w("MQTT", "connectionLost: ${cause?.message}")
                        }
                        override fun messageArrived(topic: String?, message: MqttMessage?) {
                            val t = topic.orEmpty()
                            val payload = message?.toString().orEmpty()
                            listeners[t]?.invoke(payload)
                        }
                        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
                    })

                    c.connect(options)
                    Log.d("MQTT", "✅ Connecté à $serverUri")
                }
                withContext(Dispatchers.Main) { onConnected?.invoke() }
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Erreur connect()", e)
            }
        }
    }

    /**
     * Raccourci pour l'ancien code : connecte puis s'abonne au topic.
     */
    fun connectAndSubscribe(
        topic: String,
        qos: Int = 1,
        onMessageArrived: (String) -> Unit
    ) {
        connect(onConnected = { subscribe(topic, qos, onMessageArrived) })
    }

    /**
     * Abonnement (idempotent) avec callback par topic.
     */
    fun subscribe(
        topic: String,
        qos: Int = 1,
        onMessageArrived: (String) -> Unit
    ) {
        listeners[topic] = onMessageArrived
        scope.launch {
            try {
                val c = client
                if (c == null || !c.isConnected) {
                    Log.w("MQTT", "subscribe($topic) ignoré: client non connecté")
                    return@launch
                }
                c.subscribe(topic, qos)
                Log.d("MQTT", "✅ Subscribe $topic")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Erreur subscribe($topic)", e)
            }
        }
    }

    /**
     * Désabonnement (et on enlève le callback associé).
     */
    fun unsubscribe(topic: String) {
        listeners.remove(topic)
        scope.launch {
            try {
                client?.takeIf { it.isConnected }?.unsubscribe(topic)
                Log.d("MQTT", "↩︎ Unsubscribe $topic")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Erreur unsubscribe($topic)", e)
            }
        }
    }

    /**
     * Publication simple (ne fait rien si non connecté).
     */
    fun publish(topic: String, payload: String, qos: Int = 0, retained: Boolean = false) {
        scope.launch {
            try {
                val c = client ?: return@launch
                if (!c.isConnected) return@launch
                val msg = MqttMessage(payload.toByteArray()).apply {
                    this.qos = qos
                    isRetained = retained
                }
                c.publish(topic, msg)
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Erreur publish($topic)", e)
            }
        }
    }

    /**
     * Déconnexion + nettoyage.
     */
    fun disconnect() {
        scope.launch {
            try {
                client?.let { c ->
                    if (c.isConnected) c.disconnect()
                    c.close()
                }
                Log.d("MQTT", "🛑 Déconnecté")
            } catch (e: Exception) {
                Log.e("MQTT", "❌ Erreur disconnect()", e)
            } finally {
                listeners.clear()
                client = null
            }
        }
    }
}
