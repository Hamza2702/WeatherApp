import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Scanner;

/*
    Retrieve weather data from API
    Fetch latest weather data from external API and return it
    GUI will display data to user
*/
public class WeatherApp {
    // Fetch weather data for given location
    public static JSONObject getWeatherData(String locationName){
        // Get location coordinates using geolocation API
        JSONArray locationData = getLocationData(locationName);

        // Extract latitude and longitude data
        JSONObject location = (JSONObject) locationData.get(0);
        double latitude = (double) location.get("latitude");
        double longitude = (double) location.get("longitude");

        // Build API request URL with location coordinates
        String urlString = "https://api.open-meteo.com/v1/forecast?" +
                "latitude=" + latitude + "&longitude=" + longitude +
                "&hourly=temperature_2m,relativehumidity_2m,weathercode,windspeed_10m&timezone=Europe%2FLondon";

        try{
            // Call api and get response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check for response status
            // 200 = successful connection
            if (conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            }

            // Store resulting json data
            StringBuilder resultJson = new StringBuilder();
            Scanner scanner = new Scanner(conn.getInputStream());
            while (scanner.hasNext()){
                // Read and store into string builder
                resultJson.append(scanner.nextLine());
            }

            // Close scanner
            scanner.close();

            // Close url connection
            conn.disconnect();

            // Parse through data
            JSONParser parser = new JSONParser();
            JSONObject resultJsonObj = (JSONObject) parser.parse(String.valueOf(resultJson));

            // Retrieve hourly data
            JSONObject hourly = (JSONObject) resultJsonObj.get("hourly");

            // Current hour's data
            // Index of current data
            JSONArray time = (JSONArray) hourly.get("time");
            int index = findIndexOfCurrentTime(time);

            // Get temperature
            JSONArray temperatureData = (JSONArray) hourly.get("temperature_2m");
            double temperature = (double) temperatureData.get(index);

            // Get weather code
            JSONArray weathercode = (JSONArray) hourly.get("weathercode");
            String weatherCondition = convertWeatherCode((long) weathercode.get(index));

            // Get humidity
            JSONArray relativeHumidity = (JSONArray) hourly.get("relativehumidity_2m");
            long humidity = (long) relativeHumidity.get(index);

            // Get windspeed
            JSONArray windspeedData = (JSONArray) hourly.get("windspeed_10m");
            double windspeed = (double) windspeedData.get(index);

            // Build the weather json data object to access in frontend
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

    // Retrieves geographic coordinates for given location name
    public static JSONArray getLocationData(String locationName){
        // Replace whitespace in location name to "+" to adhere to API's request format
        locationName = locationName.replaceAll(" ", "+");

        // Build API url with location parameter
        String urlString = "https://geocoding-api.open-meteo.com/v1/search?name=" +
                locationName + "&count=10&language=en&format=json";

        try{
            // Call API and get a response
            HttpURLConnection conn = fetchApiResponse(urlString);

            // Check response status
            // 200 = successful connectiton
            if (conn.getResponseCode() != 200){
                System.out.println("Error: Could not connect to API");
                return null;
            } else {
                // Store API results
                StringBuilder resultJson = new StringBuilder();
                Scanner scanner = new Scanner(conn.getInputStream());

                // Read and store the resulting json data into string builder
                while (scanner.hasNextLine()){
                    resultJson.append(scanner.nextLine());
                }

                // Close scanner
                scanner.close();

                // Close URL connection
                conn.disconnect();

                // Parse JSON string into a JSON obj
                JSONParser parser = new JSONParser();
                JSONObject jsonObject = (JSONObject) parser.parse(String.valueOf(resultJson));

                // Get the list of location data the API generated from the location name
                JSONArray locationData = (JSONArray) jsonObject.get("results");
                return locationData;
            }

        } catch(Exception e){
            e.printStackTrace();
        }

        // Couldn't find location
        return null;
    }

    private static HttpURLConnection fetchApiResponse(String urlString){
        try{
            // Create connection
            URL url = new URL(urlString);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();

            // Set request method to get
            conn.setRequestMethod("GET");

            // Connect to API
            conn.connect();
            return conn;
        } catch(IOException e){
            e.printStackTrace();
        }
        // Couldn't make connection
        return null;
    }

    private static int findIndexOfCurrentTime(JSONArray timeList){
        String currentTime = getCurrentTime();

        // Iterate through time list and see which one matches our current time
        for (int i = 0; i < timeList.size(); i++){
            String time = (String) timeList.get(i);
            if (time.equalsIgnoreCase(currentTime)){
                // Return index
                return i;
            }
        }
        return 0;
    }

    public static String getCurrentTime(){
        // Get current date and time
        LocalDateTime currentDataTime = LocalDateTime.now();

        // Format data to be 2023-09-02T00:00 (Read in API)
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH':00'");

        // Format and print current data and time
        String formattedDateTime = currentDataTime.format(formatter);

        return formattedDateTime;
    }

    // Convert the weather code to make it readable
    private static String convertWeatherCode(long weathercode){
        String weatherCondition = "";
        if (weathercode == 0L){
            weatherCondition = "Clear";
        } else if (weathercode <= 3L && weathercode > 0L){
            weatherCondition = "Cloudy";
        } else if ((weathercode >= 5L && weathercode <= 67L)
                || (weathercode >= 80L && weathercode <= 99L)){
            // Rain
            weatherCondition = "Rain";
        } else if (weathercode >= 71L && weathercode <= 77L){
            // Snow
            weatherCondition = "Snow";
        }
        return weatherCondition;
    }
}