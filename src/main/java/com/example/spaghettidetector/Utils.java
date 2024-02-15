package com.example.spaghettidetector;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.Log;

import com.example.spaghettidetector.models.Result;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Utils {
    private static Gson gson = new Gson();

    public static void saveToJsonFile(Context context, String temperaturePlate, String temperatureNozzle, String printVelocity, boolean isAnomaly) {
        String filename = "data.json";

        try {
            JsonObject newDataObject = new JsonObject();

            // Formatta la data corrente nel formato desiderato (ad esempio, "yyyyMMdd")
            String currentDate = new SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(new Date());

            newDataObject.addProperty("date", currentDate);
            newDataObject.addProperty("temperaturePlate", temperaturePlate);
            newDataObject.addProperty("temperatureNozzle", temperatureNozzle);
            newDataObject.addProperty("printVelocity", printVelocity);
            newDataObject.addProperty("anomaly", isAnomaly);

            // Read existing data from file
            JsonArray existingData = readExistingData(context, filename);
            if (existingData == null) {
                existingData = new JsonArray();
            }
            // Aggiungi la nuova entry ai dati esistenti
            existingData.add(newDataObject);

            // Scrivi i dati aggiornati nel file
            writeDataToFile(context, filename, existingData);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private static void writeDataToFile(Context context, String filename, JsonArray data) throws IOException {
        try (FileOutputStream fos = context.openFileOutput(filename, Context.MODE_PRIVATE)) {
            fos.write(gson.toJson(data).getBytes());
            Log.d("Utils", "Data written to file: " + gson.toJson(data));
        }
    }

    private static JsonArray readExistingData(Context context, String filename) throws IOException {
        JsonArray existingData = new JsonArray();

        File file = new File(context.getFilesDir(), filename);
        if (!file.exists()) {
            file.createNewFile();
            Log.d("Utils", "File created: " + file.getAbsolutePath());
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(context.openFileInput(filename)))) {
            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
            existingData = gson.fromJson(builder.toString(), JsonArray.class);
            Log.d("Utils", "Data read from file: " + gson.toJson(existingData));
        }

        return existingData;
    }





    public static void deleteData(Context context, String date, String temperaturePlate, String temperatureNozzle, String printVelocity, boolean isAnomaly) {
        try {
            FileInputStream fis = context.openFileInput("data.json");
            InputStreamReader isr = new InputStreamReader(fis);
            BufferedReader bufferedReader = new BufferedReader(isr);

            StringBuilder builder = new StringBuilder();
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                builder.append(line);
            }

            String jsonData = builder.toString();
            JSONArray jsonArray = new JSONArray(jsonData);

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);

                // Confronta tutti i campi
                if (jsonObject.getString("date").equals(date) &&
                        jsonObject.getString("temperaturePlate").equals(temperaturePlate) &&
                        jsonObject.getString("temperatureNozzle").equals(temperatureNozzle) &&
                        jsonObject.getString("printVelocity").equals(printVelocity) &&
                        jsonObject.getBoolean("anomaly") == isAnomaly) {
                    jsonArray.remove(i);
                    break;
                }
            }

            // Write the updated data back to the file
            FileOutputStream fos = context.openFileOutput("data.json", Context.MODE_PRIVATE);
            fos.write(jsonArray.toString().getBytes());
            fos.close();
        } catch (IOException | JSONException e) {
            e.printStackTrace();
        }
    }


    static public Bitmap draw(Bitmap originalImage, ArrayList<Result> detected) {
        if (originalImage == null || originalImage.getWidth() <= 0 || originalImage.getHeight() <= 0) {
            return null;
        }

        Bitmap modifiedBitmap = originalImage.copy(Bitmap.Config.ARGB_8888, true);

        if (detected != null && !detected.isEmpty()) {
            Canvas canvas = new Canvas(modifiedBitmap);
            Paint paint = new Paint();
            paint.setStyle(Paint.Style.STROKE);
            paint.setColor(Color.RED);
            paint.setStrokeWidth(3);

            for (Result result : detected) {
                paint.setStrokeWidth(3);
                paint.setStyle(Paint.Style.STROKE);
                canvas.drawRect(result.rect, paint);
            }
        }

        return modifiedBitmap;
    }

}
