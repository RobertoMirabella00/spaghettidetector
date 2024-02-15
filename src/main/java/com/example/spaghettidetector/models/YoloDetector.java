package com.example.spaghettidetector.models;


import android.content.Context;
import android.graphics.Bitmap;

import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;
import org.pytorch.torchvision.TensorImageUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class YoloDetector {

    private static final String MODEL_PATH = "qat_script.pt";
    private static final int IMAGE_SIZE = 224; // Adjust based on your model's input size
    private static final int NUM_CLASSES = 2; // Assuming binary classification

    private Module model;

    public YoloDetector(Context context) {

        try {
            File modelFile = File.createTempFile("best", ".torchscript", context.getCacheDir());
            copyInputStreamToFile(context.getAssets().open("best.torchscript"), modelFile);

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

    public float[] detectImage(Bitmap inputImage) {
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(inputImage, PrePostProcessor.mInputWidth, PrePostProcessor.mInputHeight, true);
        final Tensor inputTensor = TensorImageUtils.bitmapToFloat32Tensor(resizedBitmap, PrePostProcessor.NO_MEAN_RGB, PrePostProcessor.NO_STD_RGB);
        Tensor outputTensor = model.forward(IValue.from(inputTensor)).toTensor();

        final float[] outputs = outputTensor.getDataAsFloatArray();

        // Process the object detection results as needed
        return outputs;
    }

}
