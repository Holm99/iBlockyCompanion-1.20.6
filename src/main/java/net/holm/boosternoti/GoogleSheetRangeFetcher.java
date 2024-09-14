package net.holm.boosternoti;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public class GoogleSheetRangeFetcher {

    private static final String SPREADSHEET_ID = "1ehG4xdkdWWeRKFNXNdhZsYAWrHi13wkgdOaPhw0idn8"; // Replace with your Spreadsheet ID
    private static Sheets sheetsService;

    // Constructor initializes the Sheets service using GoogleSheetsHelper
    public GoogleSheetRangeFetcher() throws IOException, GeneralSecurityException {
        sheetsService = GoogleSheetsHelper.getSheetsService();
    }

    // Fetch data for a specific range
    public List<List<Object>> getDataFromRange(String range) throws IOException {
        // Get values from the provided range (e.g., "Sheet1!A1:D10")
        ValueRange response = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, range)
                .execute();

        // Return the fetched values
        return response.getValues();
    }

    // Fetch pickaxe data based on enchantment and prestige level
    public void fetchPickaxeData(String enchantment, int prestigeLevel) {
        try {
            // Generate the appropriate range for the enchantment and prestige level
            String range = getRangeForEnchantment(enchantment, prestigeLevel);
            List<List<Object>> values = getDataFromRange(range);

            if (values != null && !values.isEmpty()) {
                // Process the data; for now, just print it to the console
                for (List<Object> row : values) {
                    System.out.println(row);
                }
            } else {
                System.out.println("No data found.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Define the range based on enchantment and prestige level
    private String getRangeForEnchantment(String enchantment, int prestigeLevel) {
        // Define specific ranges for each enchantment
        switch (enchantment.toLowerCase()) {
            case "jackhammer":
                return "Jackhammer!A" + prestigeLevel + ":D" + prestigeLevel; // Range for Jackhammer enchantment
            case "explosive":
                return "Explosive!A" + prestigeLevel + ":D" + prestigeLevel; // Range for Explosive enchantment
            // Add more cases for other enchantments as needed
            default:
                return "DefaultSheet!A1:D1"; // Default range if no enchantment matches
        }
    }
}
