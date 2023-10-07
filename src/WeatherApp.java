import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import netscape.javascript.JSObject;

import java.io.IOException;
import java.util.Scanner;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.json.simple.JSONArray;

// Fetch real-time API Data for GUI to display
public class WeatherApp {
    // use location to fetch data
    public static JSONObject getWeatherData(String locationName){
        // use geolocation API to get info
        JSONArray locationData = getLocationData(locationName);

        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        //build request with coords
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
        "latitude=" + latitude + "&longitude=" + longitude +
        "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=America%2FChicago";

        try{
            // get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            //check status
            if(conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());

            while(scanner.hasNext()){
                resultJson.append(scanner.nextLine());
            }

            scanner.close();
            conn.disconnect();

            // parse data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // get hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);
            if(index < 0)
                throw new Exception("Error! Location not found!");

            // temp
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // weather code / info
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            // build the object for front-end to access
            JSONObject weatherData = new JSONObject();
            
            weatherData.put("temperature", temperature);
            weatherData.put("weather_condition", weatherCondition);
            weatherData.put("humidity", humidity);
            weatherData.put("windspeed", windspeed);
            
            return weatherData;
        }catch(Exception e){
            e.printStackTrace();
        }

        return null;
    }

    // gets coords
    public static JSONArray getLocationData(String locationName){
        // format information to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        // create API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name="+ locationName +"&count=10&language=en&format=json";

        try{
            HttpURLConnection conn = fetchApiResponse(urlString);

            // check response
            if(conn.getResponseCode()!= 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }
            else{
                StringBuilder resultJson = new StringBuilder();
                Scanner scan = new Scanner(conn.getInputStream());

                while(scan.hasNext()){
                    resultJson.append(scan.nextLine());
                }

                scan.close();

                conn.disconnect();

                // turn JSON into JSON object
                JSONParser parser = new JSONParser();
                JSONObject resultsJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

                // get list of location data
                JSONArray locationData = (JSONArray) resultsJsonObj.get("results");
                return locationData;
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        // couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // try to connect
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // get
            conn.setRequestMethod("GET");

            // connect to API
            conn.connect();
            return conn;
        }catch(IOException e){
            e.printStackTrace();
        }

        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        for(int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if(time.equalsIgnoreCase(currentTime))
                return i;
        }

        return -1;
    }

    public static String getCurrentTime(){
        LocalDateTime currentDateTime = LocalDateTime.now();

        // format the date
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        String formattedDateTime = currentDateTime.format(formatter);

        return formattedDateTime;
    }

    // convert weather code to useful info
    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";

        if(weathercode == 0L){
            // clear
            weatherCondition = "Clear";
        } else if(weathercode <= 3L && weathercode > 0L){
            // cloudy
            weatherCondition = "Cloudy";
        } else if((weathercode >= 51L && weathercode <= 67L) || (weathercode >= 80L && weathercode <= 99L)){
            // rainy

            weatherCondition = "Rain";
        } else if(weathercode >= 71L && weathercode <= 77L){
            // snow
            weatherCondition = "Snow";
        }

        return weatherCondition;
    }
}