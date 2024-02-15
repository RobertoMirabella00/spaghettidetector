package com.example.spaghettidetector.ui.detect;

import static android.app.Activity.RESULT_OK;

import static com.example.spaghettidetector.Utils.*;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageAnalysis;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.LifecycleOwner;

import java.io.FileWriter;
import java.io.IOException;

import com.example.spaghettidetector.MainActivity;
import com.example.spaghettidetector.R;
import com.example.spaghettidetector.databinding.FragmentDetectBinding;
import com.example.spaghettidetector.models.PrePostProcessor;
import com.example.spaghettidetector.models.PyTorchClassifier;
import com.example.spaghettidetector.models.Result;
import com.example.spaghettidetector.models.YoloDetector;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.concurrent.Executor;

public class DetectFragment extends Fragment implements View.OnClickListener, ImageAnalysis.Analyzer {
    private final long DELAY = 2000;
    private ListenableFuture<ProcessCameraProvider> provider;
    private FragmentDetectBinding binding;
    private Button classificationButton;
    private Button detectionButton;
    private PreviewView pview;
    private ImageAnalysis imageAn;
    private ImageCapture imageCapt;
    private TextView resultTextView;
    private ImageView imageView;
    private Bitmap latestImage;
    private boolean isClassifing = false;
    private boolean isDetecting = false;
    private PyTorchClassifier pyTorchClassifier;
    private YoloDetector detector;
    private long time = 0;
    private String temperaturePlate,temperatureNozzle,printVelocity;
    private static final int REQUEST_IMAGE_PICK = 100;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentDetectBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        classificationButton = binding.classificationButton;
        detectionButton = binding.detectionButton;
        pview = binding.previewView;
        resultTextView = binding.textResult;
        imageView = binding.detectedView;

        pyTorchClassifier = ((MainActivity) requireActivity()).getClassifier();
        detector = ((MainActivity) getActivity()).getDetector();

        provider = ProcessCameraProvider.getInstance(requireContext());
        provider.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = provider.get();
                startCamera(cameraProvider);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, getExecutor());

        // Set click listeners for the buttons
        classificationButton.setOnClickListener(this);
        detectionButton.setOnClickListener(this);

        return root;
    }

    public void onClassificationButtonClick(View view) {

        if (imageView != null) {
            imageView.setVisibility(View.GONE);
        }

        if (pview != null) {
            //pview.setVisibility(View.VISIBLE);
        }

        if (!isClassifing) {
            isDetecting=false;
            showInputDialog();
            startClassification();

        } else {
            stopClassification();
        }
    }

    public void onDetectionButtonClick(View view) {
        if (pview != null) {
            //pview.setVisibility(View.GONE);
        }

        if (imageView != null) {
            imageView.setVisibility(View.VISIBLE);
        }
        if (isClassifing)
            stopClassification();
        isDetecting = !isDetecting;
        //openGallery();
    }
    @Override
    public void onClick(View view) {
        if (view.getId() == classificationButton.getId())
            onClassificationButtonClick(view);
        else if (view.getId() == detectionButton.getId())
            onDetectionButtonClick(view);

    }

    private void showInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.layout_dialog_input_data, null);

        EditText editTextTemperaturePlate = dialogView.findViewById(R.id.editTextTemperaturePlate);
        EditText editTextTemperatureNozzle = dialogView.findViewById(R.id.editTextTemperatureNozzle);
        EditText editTextPrintVelocity = dialogView.findViewById(R.id.editTextPrintVelocity);

        builder.setView(dialogView)
                .setTitle("Inserisci i dati")
                .setPositiveButton("Salva", (dialog, which) -> {
                    temperaturePlate = editTextTemperaturePlate.getText().toString();
                    temperatureNozzle = editTextTemperatureNozzle.getText().toString();
                    printVelocity = editTextPrintVelocity.getText().toString();
                })
                .setNegativeButton("Annulla", (dialog, which) -> {
                });

        builder.create().show();
    }


    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_IMAGE_PICK);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            // Handle the selected image
            Bitmap selectedImage = getBitmapFromGallery(data);
            if (selectedImage != null) {

                // Perform detection on the selected image
                detect(selectedImage);
            }
        }
    }

    private Bitmap getBitmapFromGallery(Intent data) {
        try {
            android.net.Uri imageUri = data.getData();

            return MediaStore.Images.Media.getBitmap(requireActivity().getContentResolver(), imageUri);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private void detect(Bitmap image) {
        float[] detected = detector.detectImage(image);

        float imgScaleX = (float) image.getWidth() / PrePostProcessor.mInputWidth;
        float imgScaleY = (float) image.getHeight() / PrePostProcessor.mInputHeight;
        float ivScaleX = (float) image.getWidth() / PrePostProcessor.mInputWidth;
        float ivScaleY = (float) image.getHeight() / PrePostProcessor.mInputHeight;

        final ArrayList<Result> results = PrePostProcessor.outputsToNMSPredictions(detected, imgScaleX, imgScaleY, ivScaleX, ivScaleY, 0, 0);

        Bitmap modifiedImage = draw(image, results);

        if (modifiedImage != null && modifiedImage.getWidth() > 0 && modifiedImage.getHeight() > 0) {
            requireActivity().runOnUiThread(() -> imageView.setImageBitmap(Bitmap.createScaledBitmap(modifiedImage, imageView.getWidth(), imageView.getHeight(), false)));
        } else {
            Log.e("ERRORE","immagine generata non valida");
        }
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void startCamera(ProcessCameraProvider cameraProvider) {
        cameraProvider.unbindAll();
        CameraSelector camSelector = new CameraSelector.Builder().requireLensFacing(CameraSelector.LENS_FACING_BACK).build();

        Preview preview = new Preview.Builder().build();
        preview.setSurfaceProvider(pview.getSurfaceProvider());

        imageCapt = new ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build();
        imageAn = new ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build();
        imageAn.setAnalyzer(getExecutor(), this);

        cameraProvider.bindToLifecycle((LifecycleOwner) this, camSelector, preview, imageCapt, imageAn);
    }

    private Executor getExecutor() {
        return ContextCompat.getMainExecutor(requireContext());
    }

    public void startClassification() {
        isClassifing = true;
        resultTextView.setText(""); // Clear previous results
        classificationButton.setText("Stop");
    }

    public void stopClassification() {
        isClassifing = false;
        classificationButton.setText("Classifica");

        // Check if an anomaly was detected during the last classification
        boolean isAnomaly = classify(pview.getBitmap());

        // Save data to file
        saveToJsonFile(requireContext(),temperaturePlate,temperatureNozzle,printVelocity, isAnomaly);

        resultTextView.setText("");
    }

    private boolean classify(Bitmap image) {
        boolean isAnomaly = false;
        if (image != null) {
            isAnomaly = pyTorchClassifier.classifyImage(image);
            // Update the resultTextView based on the classification result
            if (isAnomaly) {
                resultTextView.setText("Anomalia!");
            } else {
                resultTextView.setText("Tutto Bene!");
            }
        }
        return isAnomaly;
    }



    @Override
    public void analyze(@NonNull ImageProxy image) {
        Bitmap bitmap = pview.getBitmap();

        if (bitmap != null) {
            latestImage = bitmap;
            if (isDetecting && time < System.currentTimeMillis()) {
                detect(latestImage);
                time = System.currentTimeMillis() + DELAY;
            }
            if (isClassifing && time < System.currentTimeMillis()) {
                classify(latestImage);
                time = System.currentTimeMillis() + DELAY;
            }
        }

        image.close();
    }
}