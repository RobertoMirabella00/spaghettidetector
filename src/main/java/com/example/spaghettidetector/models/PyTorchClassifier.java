package com.example.spaghettidetector.models;


import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class PyTorchClassifier {

    private static final String MODEL_PATH = "optimized_model.pt";
    private static final int IMAGE_SIZE = 224; // Adjust based on your model's input size
    private static final int NUM_CLASSES = 2; // Assuming binary classification

    private Module model;

    public PyTorchClassifier(Context context) {

        try {
            File modelFile = File.createTempFile("optimized_model", ".pt", context.getCacheDir());
            copyInputStreamToFile(context.getAssets().open(MODEL_PATH), modelFile);

            model = Module.load(modelFile.getAbsolutePath());

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {
        try (FileOutputStream outputStream = new FileOutputStream(file)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, length);
            }
        }
    }

    public boolean classifyImage(Bitmap inputImage) {
        Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(inputImage,TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,TensorImageUtils.TORCHVISION_NORM_STD_RGB);

        Tensor outputTensor = model.forward(IValue.from(inputTensor)).toTensor();

        float[] scores = outputTensor.getDataAsFloatArray();

        Log.d("Tag", String.valueOf(scores[0])+"  "+String.valueOf(scores[1]));

        return scores[0] > scores[1];
    }

}
