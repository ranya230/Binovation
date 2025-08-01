package fr.isen.amara.isensmartcompanion.screens

import android.content.Context
import org.eclipse.paho.client.mqttv3.*

class MqttClientHelper {
    private val brokerUrl = "tcp://broker.hivemq.com:1883"
    private val clientId = MqttClient.generateClientId()
    private val mqttClient = MqttClient(brokerUrl, clientId, null)

    fun connectAndSubscribe(topic: String, onMessage: (String) -> Unit) {
        val options = MqttConnectOptions()
        options.isCleanSession = true

        mqttClient.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {}
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                onMessage(message.toString())
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {}
        })

        mqttClient.connect(options)
        mqttClient.subscribe(topic)
    }

    fun publish(topic: String, message: String) {
        if (mqttClient.isConnected) {
            mqttClient.publish(topic, MqttMessage(message.toByteArray()))
        }
    }
}
