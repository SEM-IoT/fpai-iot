package org.tno.mqtt.connection;

import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.tno.mqtt.connection.MqttConnection.Config;

import aQute.bnd.annotation.component.Activate;
import aQute.bnd.annotation.component.Component;
import aQute.bnd.annotation.component.Deactivate;
import aQute.bnd.annotation.metatype.Configurable;
import aQute.bnd.annotation.metatype.Meta;

@Component(designateFactory = Config.class, provide = MqttConnection.class, immediate = true)
public class MqttConnection {

    private final static Logger logger = LoggerFactory.getLogger(MqttConnection.class);

    @Meta.OCD
    interface Config {
        @Meta.AD(deflt = "tcp://192.168.1.3:1883")
        String mqttURL();
    }

    private MqttClient mqttClient;
    private String mqttUri;

    @Activate
    public void activate(BundleContext bundleContext, Map<String, Object> properties) {
        try {
            Config config = Configurable.createConfigurable(Config.class, properties);
            mqttUri = config.mqttURL();

            mqttClient = new MqttClient(config.mqttURL(), "test");

            logger.info("Mqtt Connection initialzed");

        } catch (Throwable e) {
            logger.error("Mqtt Connection not initialezed");
        }
    }

    @Deactivate
    public void deactivate() {
    }

    public synchronized void publishMqtt(String input, String postTopic) {

        try {
            mqttClient.connect();
        } catch (MqttSecurityException e) {
            logger.error("Security Connection Exception in mqtt com", e);
        } catch (MqttException e) {
            logger.error("Mqtt Connection Exception in mqtt com", e);
        }
        MqttMessage message = new MqttMessage(input.getBytes());
        MqttTopic topic = mqttClient.getTopic(postTopic);

        // Wait until the message has been delivered to the server
        try {
            MqttDeliveryToken token = topic.publish(message);
            token.waitForCompletion();
            logger.debug("Message received by mqtt bus");
            mqttClient.disconnect();
        } catch (MqttSecurityException e) {
            logger.error("Sequrity Exception in mqtt com", e);
        } catch (MqttException e) {
            logger.error("Mqtt Exception in mqtt com", e);
        }
    }
}
