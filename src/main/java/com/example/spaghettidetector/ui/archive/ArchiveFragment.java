package com.example.spaghettidetector.ui.archive;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.spaghettidetector.R;
import com.example.spaghettidetector.Utils;
import com.example.spaghettidetector.databinding.FragmentArchiveBinding;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class ArchiveFragment extends Fragment {

    private LinearLayout linearLayoutProfiles;
    private FragmentArchiveBinding binding;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentArchiveBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        linearLayoutProfiles = binding.linearLayoutProfiles;

        readDataFromFile();

        return root;
    }

    private void readDataFromFile() {
        try {
            FileInputStream fis = requireContext().openFileInput("data.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }

            String jsonData = builder.toString();
            // Log per verificare se il JSON viene letto correttamente
            Log.d("ArchiveFragment", "JSON data: " + jsonData);

            // Parse JSON data and display profiles
            displayProfiles(jsonData);

            fis.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void displayProfiles(String jsonData) {
        try {
            JSONArray jsonArray = new JSONArray(jsonData);
            Log.d("ArchiveFragment", "Number of items in JSON array: " + jsonArray.length());

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                try {
                    String timestamp = jsonObject.getString("date");
                    String temperaturePlate = jsonObject.getString("temperaturePlate");
                    String temperatureNozzle = jsonObject.getString("temperatureNozzle");
                    String printVelocity = jsonObject.getString("printVelocity");
                    boolean anomaly = jsonObject.getBoolean("anomaly");

                    Log.d("ArchiveFragment", "Item " + i + " - Timestamp: " + timestamp +
                            ", Temperature Plate: " + temperaturePlate +
                            ", Temperature Nozzle: " + temperatureNozzle +
                            ", Print Velocity: " + printVelocity +
                            ", Anomaly: " + anomaly);

                    // Create a TextView to display the profile information
                    TextView profileTextView = new TextView(requireContext());
                    profileTextView.setText("Timestamp: " + timestamp +
                            "\nTemperature Plate: " + temperaturePlate +
                            "\nTemperature Nozzle: " + temperatureNozzle +
                            "\nPrint Velocity: " + printVelocity +
                            "\nAnomaly: " + anomaly);

                    // Create a Button to delete the profile
                    Button deleteButton = new Button(requireContext());
                    deleteButton.setText("Delete");
                    deleteButton.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            // Implement deletion logic when the button is clicked
                            Utils.deleteData(requireContext(), timestamp, temperaturePlate, temperatureNozzle, printVelocity, anomaly);
                            // Refresh the UI after deletion
                            linearLayoutProfiles.removeAllViews();
                            readDataFromFile();
                        }
                    });

                    // Add the TextView and Button to the layout
                    linearLayoutProfiles.addView(profileTextView);
                    linearLayoutProfiles.addView(deleteButton);
                } catch (JSONException e) {
                    Log.e("ArchiveFragment", "Error parsing JSON for item " + i, e);
                }
            }
        } catch (JSONException e) {
            Log.e("ArchiveFragment", "Error parsing JSON array", e);
        }
    }

    private void displayProfile(ProfileData profileData) {
        // Create a TextView to display the profile information
        TextView profileTextView = new TextView(requireContext());
        profileTextView.setText("Timestamp: " + profileData.getTimestamp() +
                "\nTemperature Plate: " + profileData.getTemperaturePlate() +
                "\nTemperature Nozzle: " + profileData.getTemperatureNozzle() +
                "\nPrint Velocity: " + profileData.getPrintVelocity() +
                "\nAnomaly: " + profileData.isAnomaly());

        // Create a Button to delete the profile
        Button deleteButton = new Button(requireContext());
        deleteButton.setText("Delete");
        deleteButton.setOnClickListener(view -> {
            // Implement deletion logic when the button is clicked
            Utils.deleteData(requireContext(), profileData.getTimestamp(), profileData.getTemperaturePlate(), profileData.getTemperatureNozzle(), profileData.getPrintVelocity(), profileData.isAnomaly());
            // Refresh the UI after deletion
            linearLayoutProfiles.removeAllViews();
            readDataFromFile();
        });

        // Add the TextView and Button to the layout
        linearLayoutProfiles.addView(profileTextView);
        linearLayoutProfiles.addView(deleteButton);
    }

    // Model class to represent profile data
    private static class ProfileData {
        private final String timestamp;
        private final String temperaturePlate;
        private final String temperatureNozzle;
        private final String printVelocity;
        private final boolean anomaly;

        public ProfileData(String timestamp, String temperaturePlate, String temperatureNozzle, String printVelocity, boolean anomaly) {
            this.timestamp = timestamp;
            this.temperaturePlate = temperaturePlate;
            this.temperatureNozzle = temperatureNozzle;
            this.printVelocity = printVelocity;
            this.anomaly = anomaly;
        }

        public String getTimestamp() {
            return timestamp;
        }

        public String getTemperaturePlate() {
            return temperaturePlate;
        }

        public String getTemperatureNozzle() {
            return temperatureNozzle;
        }

        public String getPrintVelocity() {
            return printVelocity;
        }

        public boolean isAnomaly() {
            return anomaly;
        }
    }
}
