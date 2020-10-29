package activemq;

import com.google.gson.Gson;
import org.fusesource.hawtbuf.AsciiBuffer;
import org.fusesource.hawtbuf.Buffer;
import org.fusesource.hawtbuf.UTF8Buffer;
import org.fusesource.mqtt.client.FutureConnection;
import org.fusesource.mqtt.client.MQTT;
import org.fusesource.mqtt.client.QoS;

import javax.net.ssl.HttpsURLConnection;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

class Producer {

    public static void main(String[] args) throws Exception {
        //set host and port
        String host = "localhost";
        int port = 1883;

        String key;

        List<String> stringsarr = new ArrayList<>();

        //Key wird ausgelesen
        Scanner in = new Scanner(new FileReader("key.txt"));

        StringBuilder sb = new StringBuilder();
        while (in.hasNext()) {
            sb.append(in.next());
        }
        in.close();
        key = sb.toString();

        //create MQTT instance
        MQTT mqtt = new MQTT();
        mqtt.setHost(host, port);


        //set the number of
        Scanner sc = new Scanner(System.in);
        System.out.println("How many stocks would you like to publish?");
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

        //start connections
        FutureConnection connection = mqtt.futureConnection();
        connection.connect().await();

        //loop to send stock prices every 10 seconds
        for (int i = 0; i < 18; i++) {
            for (int j = 0; j < num; j++) {
                Producer producer = new Producer();
                Buffer msg = new AsciiBuffer(producer.buildMessage(stringsarr.get(j), key));

                UTF8Buffer topic = new UTF8Buffer(stringsarr.get(j));

                if (msg.length != 0) {
                    connection.publish(topic, msg, QoS.EXACTLY_ONCE, true);
                    System.out.println("Nachricht " + msg + " in topic: " + topic.toString() + " gesendet!");
                }
            }
            Thread.sleep(10000);
        }

        //disconnect the connections
        connection.disconnect().await();

        System.out.println("System beendet!");

        System.exit(0);
    }

    //method to get the price from the api
    public String getPrice(String name, String key) {
        try {
            //create the URL with the given stock name
            URL url = new URL("https://finnhub.io/api/v1/quote?symbol=" + name + "&token=" + key);

            //create the connection and open it to get and save the response
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();

            //make the JSON data usable
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";

            //get the property from the JSON data
            Gson gson = new Gson();
            Properties data = gson.fromJson(result, Properties.class);
            String p = data.getProperty("c");

            return p;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return "";
    }

    //method to get the currency from the api
    public List getCurrency(String name, String key) {
        try {
            //create the URL with the given stock name
            URL url = new URL("https://finnhub.io/api/v1/stock/profile2?symbol=" + name + "&token=" + key);

            //create the connection and open it to get and save the response
            HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("accept", "application/json");
            InputStream responseStream = connection.getInputStream();

            //make the JSON data usable
            Scanner s = new Scanner(responseStream).useDelimiter("\\A");
            String result = s.hasNext() ? s.next() : "";

            //get the property from the JSON data
            Gson gson = new Gson();
            Properties data = gson.fromJson(result, Properties.class);
            String currency = data.getProperty("currency");
            String companyName = data.getProperty("name");
            String country = data.getProperty("country");

            List propertiesList = new ArrayList();
            propertiesList.add(currency);
            propertiesList.add(companyName);
            propertiesList.add(country);

            return propertiesList;

        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new ArrayList();
    }

    //method to build the String which will be send
    public String buildMessage(String name, String key2) {

        String message = "";
        String key = key2;
        Producer producer = new Producer();

        String currency = "";
        String companyName = "";
        String country = "";

        //get the currency for the stock
        List properties = producer.getCurrency(name, key);
        if (properties.size() == 3 && properties.get(0) != null && properties.get(1) != null && properties.get(2) != null) {
            currency = properties.get(0).toString();
            companyName = properties.get(1).toString();
            country = properties.get(2).toString();
        }

        //get the price for the stock
        String price = producer.getPrice(name, key);

        //get the current date
        Date date = java.util.Calendar.getInstance().getTime();

        //build the string
        if (currency != "" && price != "" && companyName != "" && country != "") {
            message = date + "   " + companyName + " (" + name + ") " + "from " + country + " is worth " + price + "$ original currency is: " + currency;
        }

        return message;
    }
}