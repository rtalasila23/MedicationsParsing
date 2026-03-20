import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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

                // Prepare prompt for AI service
                String prompt = "Process the medication: " + medicationString + " with RxNormCode: " + rxNormCode;
                String aiResponse = callAIService(prompt);

                // Store the response in a variable (for demonstration)
                System.out.println("AI Response for " + medicationString + ": " + aiResponse);
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
}