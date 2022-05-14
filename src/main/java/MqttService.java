import java.util.Random;
import java.util.UUID;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttTopic;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings({"BusyWait", "InfiniteLoopStatement"})
public class MqttService implements  MqttCallback{
    MqttClient myClient;
    MqttConnectOptions connOpt;

    static final String M2MIO_THING = UUID.randomUUID().toString();
    static final String BROKER_URL = "tcp://test.mosquitto.org:1883";
//    1883: Unauthenticated & unencrypted

    static final Boolean subscriber = true;
    static final Boolean publisher = true;


    private final Random rnd = new Random();

    private static final Logger log = LoggerFactory.getLogger(MqttService.class);
    public static final String TOPIC = "grupatras/lab/engine/temperature";


    /**
     *
     * connectionLost This callback is invoked upon losing the MQTT connection.
     *
     */
    public void connectionLost(Throwable t) {
        log.info("Connection lost!");
        // code to reconnect to the broker would go here if desired
    }

    /**
     *
     * deliveryComplete This callback is invoked when a message published by this
     * client is successfully received by the broker.
     *
     */
    public void deliveryComplete(IMqttDeliveryToken token) {

    }

    /**
     *
     * messageArrived This callback is invoked when a message is received on a
     * subscribed topic.
     *
     */
    public void messageArrived(String topic, MqttMessage message) {
        log.info("\n");
        log.info("-------------------------------------------------");
        log.info("| Topic:" + topic);
        log.info("| Message: " + new String(message.getPayload()));
        log.info("-------------------------------------------------");
        log.info("\n");
    }



    /**
     *
     * MAIN
     *
     */
    public static void main(String[] args) {
        MqttService MS = new MqttService();
        MS.runClient();
    }

    /**
     *
     * runClient The main functionality of this simple example. Create a MQTT
     * client, connect to broker, pub/sub, disconnect.
     *
     */
    public void runClient() {
        // setup MQTT Client
        connOpt = new MqttConnectOptions();
        connOpt.setCleanSession(true);
        connOpt.setKeepAliveInterval(30);
// connOpt.setUserName(M2MIO_USERNAME);
// connOpt.setPassword(M2MIO_PASSWORD_MD5.toCharArray());

        // Connect to Broker
        try {
            myClient = new MqttClient(BROKER_URL, M2MIO_THING);
            myClient.setCallback(this);
            myClient.connect(connOpt);
        } catch (MqttException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        log.info("Connected to " + BROKER_URL);

        String myTopic = TOPIC;
        MqttTopic topic = myClient.getTopic(myTopic);

        // subscribe to topic if subscriber
        if (subscriber) {
            try {
                int subQoS = 0;
                myClient.subscribe(myTopic, subQoS);
                if (!publisher) {
                    while (true) {
                        Thread.sleep(1000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        // publish messages if publisher
        if (publisher) {
            while (true) {
                double temp = 80 + rnd.nextDouble() * 20.0;
                String val = String.format("T:%04.2f", temp);
                String pubMsg = "{\"value\":" + val + "}";
                int pubQoS = 0;
                MqttMessage message = new MqttMessage(pubMsg.getBytes());
                message.setQos(pubQoS);
                message.setRetained(false);

                // Publish the message
                log.info("Publishing to topic \"" + topic + "\" qos " + pubQoS + "\" value " + val);


                MqttDeliveryToken token;
                try {
                    // publish message to broker
                    token = topic.publish(message);
                    // Wait until the message has been delivered to the broker
                    token.waitForCompletion();
                    Thread.sleep(1000);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        // disconnect
        try {
            // wait to ensure subscribed messages are delivered
            if (subscriber) {
                Thread.sleep(5000);
            }
            myClient.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
