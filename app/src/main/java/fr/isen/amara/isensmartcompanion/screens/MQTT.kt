package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import android.util.Log
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*

fun connectToMQTT(context: Context, onDistanceReceived: (Int) -> Unit) {
    val serverUri = "ssl://test.mosquitto.org:8883"
    val clientId = MqttClient.generateClientId()
    val topic = "Distance"

    val mqttClient = MqttAndroidClient(context, serverUri, clientId)
    val options = MqttConnectOptions().apply {
        isCleanSession = true
    }

    mqttClient.setCallback(object : MqttCallback {
        override fun connectionLost(cause: Throwable?) {
            Log.d("MQTT", "Connexion perdue")
        }

        override fun messageArrived(topic: String?, message: MqttMessage?) {
            message?.payload?.let {
                val json = String(it)
                val regex = Regex("\"distance\"\\s*:\\s*(\\d+)")
                val match = regex.find(json)
                val distance = match?.groupValues?.get(1)?.toIntOrNull()

                distance?.let {
                    onDistanceReceived(it)
                }
            }
        }

        override fun deliveryComplete(token: IMqttDeliveryToken?) {}
    })

    mqttClient.connect(options, null, object : IMqttActionListener {
        override fun onSuccess(asyncActionToken: IMqttToken?) {
            mqttClient.subscribe(topic, 1)
            Log.d("MQTT", "Connecté et abonné à $topic")
        }

        override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
            Log.e("MQTT", "Erreur de connexion MQTT : ${exception?.message}")
        }
    })
}
