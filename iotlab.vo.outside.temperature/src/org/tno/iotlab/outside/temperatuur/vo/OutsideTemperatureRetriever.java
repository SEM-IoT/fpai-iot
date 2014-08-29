package org.tno.iotlab.outside.temperatuur.vo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class OutsideTemperatureRetriever {

    private String readAll(Reader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
            sb.append((char) cp);
        }
        return sb.toString();
    }

    public String readJsonFromUrl(String url) throws IOException {
        InputStream is = new URL(url).openStream();
        try {
            BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));

            String jsonText = readAll(rd);
            return jsonText;
        } finally {
            is.close();
        }
    }

    public Map<Date, Double> retreiveData() {
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Map<Date, Double> tempMap = new HashMap<Date, Double>();
        try {
            String json = readJsonFromUrl("http://api.openweathermap.org/data/2.5/forecast?q=Delft,nl&mode=json&units=metric");
            String[] splits = json.split("\"temp\"");
            for (String split : splits) {

                int indexTimeStamp = split.indexOf("dt_txt");
                if (indexTimeStamp != -1) {
                    indexTimeStamp += 9;
                    int eind = split.indexOf("}", indexTimeStamp) - 1;
                    String datum = split.substring(indexTimeStamp, eind);
                    Date date = formatter.parse(datum);

                    Double temperature = Double.parseDouble(split.substring(1, split.indexOf(",")));

                    tempMap.put(date, temperature);
                }

            }

        } catch (Exception e) {
            System.err.println("Error in retrieval of openweathermap data");
        }
        return tempMap;
    }

}
