package net.holm.boosternoti;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

public class CSVFetcher {

    private static final String CSV_URL = "https://raw.githubusercontent.com/Holm99/iblocky-boosternotifications/master/src/main/resources/iBlocky%20calculations%20-%20RawPrestigeData.csv";
    private static final Map<String, List<Double>> enchantCostsCache = new HashMap<>();

    // Fetch the CSV data from GitHub and parse it
    public static void fetchCSVData() {
        try {
            // Create a URI from the URL string and convert it to a URL
            URI csvUri = new URI(CSV_URL);
            URL url = csvUri.toURL();

            // Open a connection to the CSV file
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");

            // Read the CSV data
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            boolean isFirstLine = true; // Skip the header

            // Process each line (representing rows of the CSV)
            while ((inputLine = in.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip the header row
                }

                // Split the line by commas
                String[] data = inputLine.split(",");

                // Skip rows with missing data (indicated by '-')
                if (data[1].equals("-")) continue;

                // Parse the data
                String enchantName = data[0];
                double toMaxFirst = parseDoubleSafely(data[1]);
                double toPrestigeFirst = parseDoubleSafely(data[2]);
                double incrementMax = parseDoubleSafely(data[3]);
                double incrementPrestige = parseDoubleSafely(data[4]);

                // Store parsed data in the cache
                enchantCostsCache.put(enchantName, List.of(toMaxFirst, toPrestigeFirst, incrementMax, incrementPrestige));
            }
            in.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Helper method to safely parse strings into Doubles
    private static double parseDoubleSafely(String value) {
        try {
            return value.equals("-") ? 0.0 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0; // Default to 0 if parsing fails
        }
    }

    // Access the cached enchant costs
    public static Map<String, List<Double>> getEnchantCostsCache() {
        return enchantCostsCache;
    }
}
