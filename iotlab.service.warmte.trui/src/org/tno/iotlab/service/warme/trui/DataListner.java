package org.tno.iotlab.service.warme.trui;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttMessage;

public class DataListner implements MqttCallback {

    @Override
    public void connectionLost(Throwable arg0) {
        // TODO Auto-generated method stub
        System.err.println("connection lost");
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken arg0) {
        // TODO Auto-generated method stub
        System.err.println("delivery complete");
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        // TODO Auto-generated method stub
        System.err.println("gevonden");
    }
}
