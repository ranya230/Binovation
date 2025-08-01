package fr.isen.amara.isensmartcompanion.screens

import org.eclipse.paho.client.mqttv3.*
import java.util.*

class MqttClientHelper {
    private lateinit var mqttClient: MqttClient

    fun connectAndSubscribe(topic: String, onMessageReceived: (String) -> Unit) {
        try {
            mqttClient = MqttClient("tcp://test.mosquitto.org:1883", UUID.randomUUID().toString(), null)
            val options = MqttConnectOptions().apply {
                isAutomaticReconnect = true
                isCleanSession = true
            }
            mqttClient.connect(options)

            mqttClient.subscribe(topic) { _, msg ->
                onMessageReceived(msg.payload.decodeToString())
            }
        } catch (e: Exception) {
            println("MQTT Error: ${e.localizedMessage}")
        }
    }

    fun publish(topic: String, message: String) {
        if (::mqttClient.isInitialized && mqttClient.isConnected) {
            val msg = MqttMessage()
            msg.payload = message.toByteArray()
            mqttClient.publish(topic, msg)
        }
    }
}
