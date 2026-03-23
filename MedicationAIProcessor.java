import org.json.JSONObject;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class MedicationAIProcessor {
    private static final String DB_URL = "jdbc:mysql://localhost:3306/your_database_name";
    private static final String DB_USER = "your_username";
    private static final String DB_PASSWORD = "your_password";

    public static void main(String[] args) {
        try {
            Connection connection = DriverManager.getConnection(DB_URL, DB_USER, DB_PASSWORD);
            String query = "SELECT MedicationString, RxNormCode FROM MedicationData";
            PreparedStatement preparedStatement = connection.prepareStatement(query);
            ResultSet resultSet = preparedStatement.executeQuery();

            while (resultSet.next()) {
                String medicationString = resultSet.getString("MedicationString");
                String rxNormCode = resultSet.getString("RxNormCode");

                if (rxNormCode == null || rxNormCode.trim().isEmpty()) {
                    System.out.println("Invalid RxNormCode for: " + medicationString);
                    continue;
                }

                // Prepare prompt for AI service with assumptions
                String prompt = "Process the medication: " + medicationString + " with RxNormCode: " + rxNormCode + ". Return JSON with fields: AI_Drugname, AI_Strength, AI_Formulation, AI_Route, AI_Assumptions";
                String aiResponse = callAIService(prompt);

                // Parse AI response and load into database
                System.out.println("AI Response for " + medicationString + ": " + aiResponse);
                parseAndStoreAIResponse(connection, medicationString, rxNormCode, aiResponse);
            }

            resultSet.close();
            preparedStatement.close();
            connection.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static String callAIService(String prompt) throws Exception {
        URL url = new URL("https://api.youraiservice.com/v1/process");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        String jsonInputString = String.format("{\"prompt\":\"%s\"}", prompt);

        try (OutputStream os = connection.getOutputStream()) {
            byte[] input = jsonInputString.getBytes("utf-8");
            os.write(input, 0, input.length);
        }

        int responseCode = connection.getResponseCode();
        if (responseCode == HttpURLConnection.HTTP_OK) {
            StringBuilder response = new StringBuilder();
            BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"));
            String responseLine;
            while ((responseLine = reader.readLine()) != null) {
                response.append(responseLine.trim());
            }
            return response.toString();
        } else {
            return "AI service failed with response code: " + responseCode;
        }
    }

    private static void parseAndStoreAIResponse(Connection connection, String medicationString, String rxNormCode, String aiResponse) {
        try {
            JSONObject jsonObject = new JSONObject(aiResponse);
            
            // Extract parsed medication data from JSON
            String aiDrugname = jsonObject.optString("AI_Drugname", "");
            String aiStrength = jsonObject.optString("AI_Strength", "");
            String aiFormulation = jsonObject.optString("AI_Formulation", "");
            String aiRoute = jsonObject.optString("AI_Route", "");
            String aiAssumptions = jsonObject.optString("AI_Assumptions", "");

            // Insert into ParsedMedications table
            insertParsedMedication(connection, medicationString, rxNormCode, aiDrugname, aiStrength, aiFormulation, aiRoute, aiAssumptions);

        } catch (Exception e) {
            System.err.println("Error parsing AI response for " + medicationString + ": " + e.getMessage());
        }
    }

    private static void insertParsedMedication(Connection connection, String medicationString, String rxNormCode, 
                                               String aiDrugname, String aiStrength, String aiFormulation, 
                                               String aiRoute, String aiAssumptions) {
        String sql = "INSERT INTO ParsedMedications (MedicationString, RxNormCode, AI_Drugname, AI_Strength, AI_Formulation, AI_Route, AI_Assumptions) VALUES (?, ?, ?, ?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
            pstmt.setString(1, medicationString);
            pstmt.setString(2, rxNormCode);
            pstmt.setString(3, aiDrugname);
            pstmt.setString(4, aiStrength);
            pstmt.setString(5, aiFormulation);
            pstmt.setString(6, aiRoute);
            pstmt.setString(7, aiAssumptions);
            
            int rowsInserted = pstmt.executeUpdate();
            if (rowsInserted > 0) {
                System.out.println("Successfully inserted parsed data for: " + medicationString);
            }
        } catch (Exception e) {
            System.err.println("Error inserting parsed medication data: " + e.getMessage());
        }
    }
}