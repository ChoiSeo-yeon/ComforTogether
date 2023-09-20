package com.example.comfortogether;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.os.VibrationEffect;
import android.os.Vibrator;
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
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.List;


public class LineDetecter {
    private boolean isInitialized = false;
    private static String tag = "OpenCV";
    private Mat oldLines;

    public LineDetecter() {
        if (OpenCVLoader.initDebug()) {
            isInitialized = true;
            Log.d(tag, "OpenCV initialized");
        } else {
            Log.e(tag, "OpenCV Initializing Fail");
        }
    }

    public Bitmap DetectingLine(@NonNull Bitmap bitmap) {
        Mat frame = new Mat();
        Utils.bitmapToMat(bitmap, frame);

        // 이미지 가공
        Imgproc.resize(frame, frame, new Size(0, 0), 0.3, 0.3, Imgproc.INTER_AREA);

        // 컬러 컨버팅
        Mat hsv = new Mat();
        Imgproc.cvtColor(frame, hsv, Imgproc.COLOR_BGR2HSV);

        // 라인 디텍팅
        Scalar lowerYellow1 = new Scalar(0, 52, 175);
        Scalar upperYellow1 = new Scalar(0, 255, 255);
        Scalar lowerYellow2 = new Scalar(0, 52, 175);
        Scalar upperYellow2 = new Scalar(0, 255, 255);

        Mat mask1 = new Mat();
        Mat mask2 = new Mat();
        Mat mask  = new Mat();
        Mat edge  = new Mat();

        Core.inRange(hsv, lowerYellow1, upperYellow1, mask1);
        Core.inRange(hsv, lowerYellow2, upperYellow2, mask2);

        // mask 더하기
        // 근데 python에선 mask = mask1 + mask2로 돼있어 맞는지 모름
        Core.add(mask1, mask2, mask);
        Imgproc.Canny(mask, edge, 150, 300);

        // 엣지를 비트맵으로 바꿔 반환
        Utils.matToBitmap(edge, bitmap);
        return bitmap;
    }

    public boolean DrawLines(Mat image, Mat lines, Scalar color, float thickness) {
        if (lines == this.oldLines || lines.empty())
            return false;

        this.oldLines = lines;
        return true;
    }

    public boolean HoughLines(Bitmap bitmap,
                              double rho,
                              double theta,
                              int    threshold,
                              double minLineLen,
                              double maxLineGap) {
        Mat image     = new Mat();
        Mat grayImage = new Mat();
        Mat lines     = new Mat();

        Utils.bitmapToMat(bitmap, image);

        Imgproc.cvtColor(image, grayImage, Imgproc.COLOR_BGR2GRAY);
        Imgproc.HoughLinesP(grayImage, lines, rho, theta, threshold,
                minLineLen, maxLineGap);

        if (lines.empty())
            return false;

        Mat lineImg = new Mat(image.rows(), image.cols(), CvType.CV_8UC3);
        return DrawLines(lineImg, lines, new Scalar(0, 255, 0), 5);
    }

    public void LineDetecting(Bitmap bitmap, Vibrator vibrator) {
        boolean isDetect = this.HoughLines(bitmap, 2.0, Math.PI/180,
                90, 120, 150);

        if (isDetect)
            vibrator.vibrate(VibrationEffect.createOneShot(300, 100));
    }
}
