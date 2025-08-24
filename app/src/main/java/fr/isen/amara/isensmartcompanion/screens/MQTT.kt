// MqttClient.kt (SAFE)
package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

fun connectToMQTT(
    context: Context,
    onDistanceReceived: (Int) -> Unit,
    serverUri: String = "ssl://test.mosquitto.org:8883",
    topic: String = "Distance",
    userName: String? = "stm32U5",
    password: CharArray? = null,
    useInsecureTrustAllTls: Boolean = false
) {
    val appCtx = context.applicationContext // ✅ évite les leaks
    val clientId = MqttClient.generateClientId()
    val mqttClient = MqttAndroidClient(appCtx, serverUri, clientId)

    val options = MqttConnectOptions().apply {
        isCleanSession = true
        isAutomaticReconnect = true
        connectionTimeout = 10
        keepAliveInterval = 20
        userName?.let { this.userName = it }
        password?.let { this.password = it }
        if (useInsecureTrustAllTls) {
            try { socketFactory = TrustAllSocketFactory.socketFactory() } catch (_: Exception) {}
        }
    }

    mqttClient.setCallback(object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            Log.w("MQTT", "Connexion perdue: ${cause?.message}")
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            try {
                val text = String(message?.payload ?: return, Charsets.UTF_8)
                val mm = parseDistance(text) ?: run {
                    Log.w("MQTT", "Payload non parsable: '$text'"); return
                }
                onDistanceReceived(mm.toInt())
            } catch (e: Exception) {
                Log.e("MQTT", "messageArrived error", e) // ✅ on log, pas de crash
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
    })

    try {
        mqttClient.connect(options, null, object : IMqttActionListener {
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                mqttClient.subscribe(topic, 1, null, object : IMqttActionListener {
                    override fun onSuccess(asyncActionToken: IMqttToken?) {
                        Log.d("MQTT", "Connecté et abonné à $topic")
                    }
                    override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                        Log.e("MQTT", "Échec subscribe: ${exception?.message}", exception)
                    }
                })
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                Log.e("MQTT", "Erreur de connexion MQTT : ${exception?.message}", exception)
            }
        })
    } catch (e: Exception) {
        Log.e("MQTT", "connect() a échoué", e) // ✅ pas de crash
    }
}
