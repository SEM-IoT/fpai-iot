package org.tno.iotlab.service.warme.trui;

import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;

public class DataListner implements MqttCallback {

    @Override
    public void connectionLost(Throwable arg0) {
        // TODO Auto-generated method stub
        System.err.println("connection lost");
    }

    @Override
    public void deliveryComplete(MqttDeliveryToken arg0) {
        // TODO Auto-generated method stub
        System.err.println("delivery complete");
    }

    @Override
    public void messageArrived(MqttTopic arg0, MqttMessage arg1) throws Exception {
        // TODO Auto-generated method stub
        System.err.println("gevonden");
    }

}
