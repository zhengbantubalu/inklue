package com.bupt.evaluate.processor.specific_extractor;

import com.bupt.evaluate.data.Contours;
import com.bupt.evaluate.data.PointEx;
import com.bupt.evaluate.data.PointList;
import com.bupt.evaluate.data.Points;
import com.bupt.evaluate.data.Strokes;
import com.bupt.evaluate.processor.SpecificExtractor;
import com.bupt.evaluate.util.Constants;

//石
public class U77F3 implements SpecificExtractor {

    public static final int strokeNum = 5;

    public Strokes extractStrokes(Contours contours, Points points) {
        Strokes strokes = new Strokes();
        for (int i = 0; i < strokeNum; i++) {
            strokes.add(new PointList());
        }
        try {
            //从左向右排序
            points.get(Points.END).sort();
            points.get(Points.INTER).sort();
            //横撇拐点
            PointEx pointEx1 = contours.findNearestPoint(
                    new PointEx(0, 0));
            //横
            strokes.get(0).addAll(contours.findMatchContour(
                    pointEx1, points.get(Points.END).get(3)));
            //撇
            strokes.get(1).addAll(contours.findMatchContour(
                    pointEx1, points.get(Points.END).get(0)));
            //口字中间短横
            strokes.get(2).add(points.get(Points.INTER).get(0));
            strokes.get(2).add(points.get(Points.INTER).get(1));
            //口字底部横途径点
            PointEx pointEx2 = contours.findNearestPoint(
                    new PointEx(Constants.IMAGE_SIZE / 2, Constants.IMAGE_SIZE / 2));
            //口字左半
            strokes.get(3).addAll(contours.findMatchContour(
                    points.get(Points.END).get(1), pointEx2));
            //口字右半
            strokes.get(4).addAll(contours.findMatchContour(
                    points.get(Points.END).get(2), pointEx2));
        } catch (NullPointerException ignored) {
        }
        return strokes;
    }
}
