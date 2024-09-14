package net.holm.boosternoti;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

import java.io.InputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collections;

public class GoogleSheetsHelper {

    private static Sheets sheetsService;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    public static Sheets getSheetsService() throws IOException, GeneralSecurityException {
        if (sheetsService == null) {
            // Load the service account key file from the resources folder
            InputStream serviceAccountStream = GoogleSheetsHelper.class.getClassLoader().getResourceAsStream("iblocky-clientmod-0a0ce72a1adb.json");

            if (serviceAccountStream == null) {
                throw new IOException("Resource not found: iblocky-clientmod-0a0ce72a1adb.json");
            }

            ServiceAccountCredentials credentials = (ServiceAccountCredentials) ServiceAccountCredentials.fromStream(serviceAccountStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));

            sheetsService = new Sheets.Builder(
                    new NetHttpTransport(),
                    JSON_FACTORY,
                    new HttpCredentialsAdapter(credentials))
                    .setApplicationName("iBlocky Prison Mod")
                    .build();
        }
        return sheetsService;
    }
}
