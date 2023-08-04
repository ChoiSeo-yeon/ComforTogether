package com.example.comfortogether;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.util.Log;

import androidx.annotation.NonNull;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;


public class LineDetecter {
    private boolean isInitialized = false;
    private static String tag = "OpenCV";
    private Scalar colorBlobLower = new Scalar(0, 52, 175);
    private Scalar colorBlobUpper = new Scalar(0, 255, 255);

    public LineDetecter() {
        if (OpenCVLoader.initDebug()) {
            isInitialized = true;
            Log.d(tag, "OpenCV initialized");
        } else {
            Log.e(tag, "OpenCV Initializing Fail");
        }
    }

    public Bitmap DetectingLine(@NonNull Bitmap bitmap) throws Exception {
        if (!isInitialized)
            throw new Exception();

        Mat rgba = new Mat();
        Utils.bitmapToMat(bitmap, rgba);

        Mat hsv = new Mat();
        Imgproc.cvtColor(rgba, hsv, Imgproc.COLOR_RGB2HSV);

        Mat mask = new Mat();
        Core.inRange(hsv, colorBlobLower, colorBlobUpper, mask);

        List<MatOfPoint> contours = new ArrayList<>();
        Mat hierarchy = new Mat();
        Imgproc.findContours(mask, contours, hierarchy, Imgproc.RETR_EXTERNAL, Imgproc.CHAIN_APPROX_SIMPLE);

        double maxArea = -1;
        MatOfPoint maxContour = null;

        for (MatOfPoint contour : contours) {
            double area = Imgproc.contourArea(contour);

            if (area > maxArea) {
                maxArea = area;
                maxContour = contour;
            }
        }

        Bitmap resBitmap = Bitmap.createBitmap(bitmap.getWidth(), bitmap.getHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(resBitmap);
        canvas.drawColor(0, PorterDuff.Mode.CLEAR);

        if (maxContour != null) {
            Moments moments = Imgproc.moments(maxContour);
            double centerX = moments.m10 / moments.m00;
            double centerY = moments.m01 / moments.m00;

            Paint paint = new Paint();
            paint.setColor(0xFF0000FF);
            paint.setStrokeWidth(2);
            paint.setStyle(Paint.Style.STROKE);

            List<Point> contourPoints = maxContour.toList();
            Point prevPoint = contourPoints.get(contourPoints.size() - 1);

            for (Point point : contourPoints) {
                canvas.drawLine((float) prevPoint.x, (float) prevPoint.y, (float) point.x,
                        (float) point.y, paint);
                prevPoint = point;
            }
        }

        rgba.release();
        hsv.release();
        mask.release();
        hierarchy.release();

        if (maxContour == null)
            throw new Exception();

        return resBitmap;
    }
}
