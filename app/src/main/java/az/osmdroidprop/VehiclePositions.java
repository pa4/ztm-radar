package az.osmdroidprop;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class VehiclePositions {
    Map<String, Vehicle> vehicleMap;
    String type;


    VehiclePositions(String type) {
        this.type = type;
        this.vehicleMap = new HashMap<>();
    }


    Map<String, Vehicle> fetchAll() throws IOException, JSONException {
        Map<String, Vehicle> vehicleMap = new LinkedHashMap<>();
        String url = "https://api.um.warszawa.pl/api/action/busestrams_get/";
        String resource_id = "f2e5503e-927d-4ad3-9500-4ab9e55deb59";
        String apikey = "_YOUR_API_KEY_GOES_HERE_";

        url += "?resource_id=" + resource_id
                + "&apikey=" + apikey
                + "&type=" + (this.type.equals("Bus") ? 1 : 2);

        URL obj = new URL(url);
        HttpURLConnection con = (HttpURLConnection) obj.openConnection();
        // optional default is GET
        con.setRequestMethod("GET");
        //add request header
        con.setRequestProperty("User-Agent", "Mozilla/5.0");
        BufferedReader in = new BufferedReader(
                new InputStreamReader(con.getInputStream()));
        String inputLine;
        StringBuffer response = new StringBuffer();
        while ((inputLine = in.readLine()) != null) {
            response.append(inputLine);
        }
        in.close();
        JSONObject jObject = new JSONObject(response.toString());
        JSONArray jArray = (JSONArray) jObject.get("result");

        for (int i=0; i < jArray.length(); i++)
        {
            JSONObject item = jArray.getJSONObject(i);
            Double lat = item.has("Lat") ? item.getDouble("Lat") : null;
            Double lon = item.has("Lon") ? item.getDouble("Lon") : null;
            String brigade = item.has("Brigade") ? item.getString("Brigade") : null;
            String line = item.has("Lines") ? item.getString("Lines") : null;
            String time = item.has("Time") ? item.getString("Time") : null;
            vehicleMap.put(line, new Vehicle(brigade, lat, line, lon, time, this.type));
        }
        return vehicleMap;
    }
}
