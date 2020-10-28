package activemq;

import org.fusesource.hawtbuf.*;
import org.fusesource.mqtt.client.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import static org.fusesource.hawtbuf.UTF8Buffer.utf8;

class Consumer {

    public static void main(String[] args) throws Exception {
        String host = "localhost";
        int port = 1883;

        MQTT mqtt = new MQTT();
        mqtt.setHost(host, port);

        List<String> stringsarr = new ArrayList<String>();

        //set the stocks
        Scanner sc = new Scanner(System.in);
        System.out.println("How many stocks would you like to subscribe to?");
        while (!sc.hasNextInt()) {
            System.out.println("Input is not a number!");
            sc.nextLine();
        }
        int num = sc.nextInt();

        for (int i = 0; i < num; i++) {
            sc = new Scanner(System.in);
            System.out.println("Please enter Stock number: " + (i + 1));
            stringsarr.add(sc.nextLine());
        }

        CallbackConnection connection = mqtt.callbackConnection();
        //establish the listener
        connection.listener(new org.fusesource.mqtt.client.Listener() {
            public void onConnected() {
            }

            public void onDisconnected() {
            }

            public void onFailure(Throwable value) {
                value.printStackTrace();
                System.exit(-2);
            }

            public void onPublish(UTF8Buffer topic, Buffer msg, Runnable ack) {
                String body = msg.utf8().toString();

                System.out.println(body);

                ack.run();
            }
        });


        //start the connection
        connection.connect(new Callback<Void>() {
            @Override
            public void onSuccess(Void value) {
                Topic[] topics = new Topic[num];

                for (int i = 0; i < num; i++) {
                    topics[i] = new Topic(stringsarr.get(i), QoS.EXACTLY_ONCE);
                }

                for (int i = 0; i < num; i++) {
                    System.out.println(topics[i].toString());
                }


                connection.subscribe(topics, new Callback<byte[]>() {
                    public void onSuccess(byte[] qoses) {
                    }

                    public void onFailure(Throwable value) {
                        value.printStackTrace();
                        System.exit(-2);
                    }
                });
            }

            @Override
            public void onFailure(Throwable value) {
                value.printStackTrace();
                System.exit(-2);
            }
        });

        synchronized (Listener.class) {
            while (true)
                Listener.class.wait();
        }


    }

}