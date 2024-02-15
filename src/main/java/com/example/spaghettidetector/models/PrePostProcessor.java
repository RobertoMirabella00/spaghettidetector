package com.example.spaghettidetector.models;

import android.graphics.Rect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PrePostProcessor {
    public static float[] NO_MEAN_RGB = new float[]{0.0f, 0.0f, 0.0f};
    public static float[] NO_STD_RGB = new float[]{1.0f, 1.0f, 1.0f};

    // model input image size
    public static int mInputWidth = 224;
    public static int mInputHeight = 224;

    public static int mOutputRow = 1029;
    public static int mOutputColumn = 7;
    public static float mThreshold = 0.30f;
    public static int mNmsLimit = 15;

    static ArrayList<Result> nonMaxSuppression(ArrayList<Result> boxes, int limit, float threshold) {

        Collections.sort(boxes,
                new Comparator<Result>() {
                    @Override
                    public int compare(Result o1, Result o2) {
                        return o1.score.compareTo(o2.score);
                    }
                });

        ArrayList<Result> selected = new ArrayList<>();
        boolean[] active = new boolean[boxes.size()];
        Arrays.fill(active, true);
        int numActive = active.length;

        boolean done = false;
        for (int i = 0; i < boxes.size() && !done; i++) {
            if (active[i]) {
                Result boxA = boxes.get(i);
                selected.add(boxA);
                if (selected.size() >= limit) break;

                for (int j = i + 1; j < boxes.size(); j++) {
                    if (active[j]) {
                        Result boxB = boxes.get(j);
                        if (IOU(boxA.rect, boxB.rect) > threshold) {
                            active[j] = false;
                            numActive -= 1;
                            if (numActive <= 0) {
                                done = true;
                                break;
                            }
                        }
                    }
                }
            }
        }
        return selected;
    }

    /**
     * Computes intersection-over-union overlap between two bounding boxes.
     */
    static float IOU(Rect a, Rect b) {
        float areaA = (a.right - a.left) * (a.bottom - a.top);
        if (areaA <= 0.0) return 0.0f;

        float areaB = (b.right - b.left) * (b.bottom - b.top);
        if (areaB <= 0.0) return 0.0f;

        float intersectionMinX = Math.max(a.left, b.left);
        float intersectionMinY = Math.max(a.top, b.top);
        float intersectionMaxX = Math.min(a.right, b.right);
        float intersectionMaxY = Math.min(a.bottom, b.bottom);
        float intersectionArea = Math.max(intersectionMaxY - intersectionMinY, 0) *
                Math.max(intersectionMaxX - intersectionMinX, 0);
        return intersectionArea / (areaA + areaB - intersectionArea);
    }

    public static ArrayList<Result> outputsToNMSPredictions(float[] outputs, float imgScaleX, float imgScaleY, float ivScaleX, float ivScaleY, float startX, float startY) {
        ArrayList<Result> results = new ArrayList<>();
        for (int i = 0; i < mOutputRow; i++) {
            float x = outputs[i];
            float y = outputs[mOutputRow + i];
            float w = outputs[2 * mOutputRow + i];
            float h = outputs[3 * mOutputRow + i];

            float left = imgScaleX * (x - w / 2);
            float top = imgScaleY * (y - h / 2);
            float right = imgScaleX * (x + w / 2);
            float bottom = imgScaleY * (y + h / 2);

            float max = outputs[4 * mOutputRow + i];
            int cls = 0;
            for (int j = 4; j < mOutputColumn; j++) {
                if (outputs[j * mOutputRow + i] > max) {
                    max = outputs[j * mOutputRow + i];
                    cls = j - 4;
                }
            }

            if (max > mThreshold) {
                Rect rect = new Rect((int) (startX + ivScaleX * left), (int) (startY + top * ivScaleY), (int) (startX + ivScaleX * right), (int) (startY + ivScaleY * bottom));
                Result result = new Result(cls, max, rect);
                results.add(result);
            }
        }
        return nonMaxSuppression(results, mNmsLimit, mThreshold);
    }
}