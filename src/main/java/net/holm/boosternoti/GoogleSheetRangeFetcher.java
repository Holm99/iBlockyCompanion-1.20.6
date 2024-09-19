package net.holm.boosternoti;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleSheetRangeFetcher {

    private static final String SPREADSHEET_ID = "1ehG4xdkdWWeRKFNXNdhZsYAWrHi13wkgdOaPhw0idn8"; // Replace with your Spreadsheet ID
    private static Sheets sheetsService;
    private static GoogleSheetRangeFetcher instance; // Singleton instance
    private final Map<String, List<Double>> enchantCostsCache = new HashMap<>(); // Cache for enchant costs
    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#,###.###"); // Formatter for large numbers with decimals

    // Private constructor for Singleton
    GoogleSheetRangeFetcher() throws IOException, GeneralSecurityException {
        sheetsService = GoogleSheetsHelper.getSheetsService();
    }

    // Static method to get the Singleton instance
    public static GoogleSheetRangeFetcher getInstance() throws IOException, GeneralSecurityException {
        if (instance == null) {
            instance = new GoogleSheetRangeFetcher();
            instance.loadEnchantCosts(); // Load enchant costs when the instance is created
        }
        return instance;
    }

    // Fetch data for a specific range
    public List<List<Object>> getDataFromRange(String range) throws IOException {
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        return response.getValues();
    }

    // Test Google Sheets API by fetching some data and caching the results
    public void GoogleSheetsAPI() {
        try {
            // Fetch data for the given range
            List<List<Object>> data = getDataFromRange("RawPrestigeData!A2:E21");

            // Print and cache the fetched data
            if (data != null && !data.isEmpty()) {
                cacheEnchantData(data); // Cache the enchant data

                // Print the cached data for verification
                enchantCostsCache.forEach((enchant, costs) -> {
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Load enchant costs from Google Sheets into the cache
    void loadEnchantCosts() throws IOException {
        List<List<Object>> data = getDataFromRange("RawPrestigeData!A2:E21");

        if (data != null && !data.isEmpty()) {
            cacheEnchantData(data);
        }
    }

    // Caching the enchant data with Double values
    private void cacheEnchantData(List<List<Object>> data) {
        for (List<Object> row : data) {
            String enchantName = row.get(0).toString(); // First column is enchant name

            try {
                // Parse the remaining values as Doubles and cache them
                double toMaxFirst = parseDoubleSafely(row.get(1).toString());
                double toPrestigeFirst = parseDoubleSafely(row.get(2).toString());
                double incrementMax = parseDoubleSafely(row.get(3).toString());
                double incrementPrestige = parseDoubleSafely(row.get(4).toString());

                enchantCostsCache.put(enchantName, List.of(toMaxFirst, toPrestigeFirst, incrementMax, incrementPrestige));
            } catch (Exception e) {
                System.err.println("Error parsing enchant data for " + enchantName + ": " + e.getMessage());
            }
        }
    }

    // Helper method to safely parse strings into Doubles
    private double parseDoubleSafely(String value) {
        try {
            return value.equals("-") ? 0.0 : Double.parseDouble(value);
        } catch (NumberFormatException e) {
            return 0.0; // Default to 0 if parsing fails
        }
    }

    // Access the cached enchant costs
    public Map<String, List<Double>> getEnchantCostsCache() {
        return enchantCostsCache;
    }

    // Helper method to format the costs for display (avoiding scientific notation)
    private String formatCosts(List<Double> costs) {
        StringBuilder formattedCosts = new StringBuilder();
        for (Double cost : costs) {
            formattedCosts.append(DECIMAL_FORMAT.format(cost)).append(", ");
        }
        return formattedCosts.toString().replaceAll(", $", ""); // Remove trailing comma and space
    }
}
