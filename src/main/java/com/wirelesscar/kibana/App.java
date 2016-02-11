package com.wirelesscar.kibana;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public class App
{
    DateFormat df = new SimpleDateFormat("yyyy.MM.dd");
    Client client = ClientBuilder.newClient();
    String kibanaHost = System.getProperty("kibana_host");
    String kibanaAddr = String.format("http://%s:8080/logstash-%s/_search", kibanaHost, df.format(new Date()));
    WebTarget target = client.target(kibanaAddr);

    String host = System.getProperty("origin_host");
    long beforeHours = getBeforeHours();
    long oneHour = 60 * 60 * 1000;
    Date from = new Date(System.currentTimeMillis() - beforeHours * oneHour);
    Date to = new Date(System.currentTimeMillis() + oneHour);
    DateFormat df2 = dateFormat();


    private DateFormat dateFormat() {
        DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
        df.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        return df;
    }

    public static void main( String[] args ) throws ParseException {
        App app = new App();
        app.fetchKibana();
    }

    public void fetchKibana() throws ParseException {
        int size = size();
        int counter = 0;
        Date from = this.from;
        long startTime = System.currentTimeMillis();

        System.err.println(String.format("Fetching log entries from %s", kibanaAddr));

        do {
            int fetchNo = 1000;
            JsonObject result = fetchKibana(from, fetchNo);

            JsonArray hitsList = result.get("hits").getAsJsonArray();
            if(hitsList.size() > 0) {
                JsonObject lastElem = hitsList.get(hitsList.size()-1).getAsJsonObject();
                Date timestamp = df2.parse(getTimeStamp(getSource(lastElem)));
                from = new Date(timestamp.getTime()+1);

                counter += iterateResults(hitsList);
            } else {
                break;
            }

            if(System.currentTimeMillis() - startTime > 2500) {
                int percentage = (int) (((counter * 1.0) / (size * 1.0)) * 100);
                System.err.println(String.format("%d/~%d (%d%%)", counter, size, percentage));
                startTime = System.currentTimeMillis();
            }
        } while(size > counter);
    }

    private long getBeforeHours() {
        String property = System.getProperty("before");
        if(property == null) {
            return 24;
        } else {
            return Long.parseLong(property);
        }
    }


    public int size() {
        JsonObject result = fetchKibana(from, 0);
        return result.get("total").getAsInt();

    }

    public JsonObject fetchKibana(Date from, int size) {
        Invocation.Builder request = target.request();
        Response response = request.post(Entity.entity(payload(host, size, from, to), MediaType.APPLICATION_JSON_TYPE));
        String str = response.readEntity(String.class);
        return new JsonParser().parse(str).getAsJsonObject().get("hits").getAsJsonObject();
    }

    public int iterateResults(JsonArray hitsList) {
        int counter = 0;
        for(JsonElement elem : hitsList) {
            printLogEntry(elem.getAsJsonObject());
            counter++;
        }

        return counter;
}

    private void printLogEntry(JsonObject json) {
        JsonObject source = getSource(json);
        String timestamp = getTimeStamp(source);
        String priority = source.get("priority").getAsString();
        String message = source.get("message").getAsString();

        String logMsg = String.format("%s %s %s", timestamp, priority, message);
        System.out.println(logMsg);
    }

    private JsonObject getSource(JsonObject json) {
        return json.get("_source").getAsJsonObject();
    }

    private String getTimeStamp(JsonObject source) {
        return source.get("@timestamp").getAsString();
    }

    private String payload(String host, int size, Date from, Date to) {
        return "{\n" +
            "  \"query\": {\n" +
            "    \"filtered\": {\n" +
            "      \"query\": {\n" +
            "        \"bool\": {\n" +
            "          \"should\": [\n" +
            "            {\n" +
            "              \"query_string\": {\n" +
            "                \"query\": \"origin_host:" + host + "\"\n" +
            "              }\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      },\n" +
            "      \"filter\": {\n" +
            "        \"bool\": {\n" +
            "          \"must\": [\n" +
            "            {\n" +
            "              \"range\": {\n" +
            "                \"@timestamp\": {\n" +
            "                  \"from\": " + from.getTime() + ",\n" +
            "                  \"to\": " + to.getTime() + "\n" +
            "                }\n" +
            "              }\n" +
            "            }\n" +
            "          ]\n" +
            "        }\n" +
            "      }\n" +
            "    }\n" +
            "  },\n" +
            "  \"highlight\": {\n" +
            "    \"fields\": {\n" +
            "      \n" +
            "    },\n" +
            "    \"fragment_size\": 2147483647,\n" +
            "    \"pre_tags\": [\n" +
            "      \"@start-highlight@\"\n" +
            "    ],\n" +
            "    \"post_tags\": [\n" +
            "      \"@end-highlight@\"\n" +
            "    ]\n" +
            "  },\n" +
            "  \"size\": " + size + ",\n" +
            "  \"sort\": [\n" +
            "    {\n" +
            "      \"@timestamp\": {\n" +
            "        \"order\": \"asc\",\n" +
            "        \"ignore_unmapped\": true\n" +
            "      }\n" +
            "    },\n" +
            "    {\n" +
            "      \"@timestamp\": {\n" +
            "        \"order\": \"asc\",\n" +
            "        \"ignore_unmapped\": true\n" +
            "      }\n" +
            "    }\n" +
            "  ]\n" +
            "}";
    }
}
