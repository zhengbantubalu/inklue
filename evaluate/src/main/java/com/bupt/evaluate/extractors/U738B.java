package com.bupt.evaluate.extractors;

import com.bupt.evaluate.Contours;
import com.bupt.evaluate.Extractor;
import com.bupt.evaluate.PointList;
import com.bupt.evaluate.Strokes;
import com.bupt.evaluate.Points;

//王
public class U738B implements Extractor {
    public static int strokeNum = 4;

    public void extract(Strokes strokes, Contours contours, Points points) {
        for (int i = 0; i < strokeNum; i++) {
            strokes.add(new PointList());
        }
        try {
            //第一横
            strokes.get(0).add(points.get(Points.END).get(0));
            strokes.get(0).add(points.get(Points.END).get(1));
            strokes.get(0).add(points.get(Points.INTER).get(0));
            strokes.get(0).sort(0, true);
            //第二横
            strokes.get(1).add(points.get(Points.END).get(2));
            strokes.get(1).add(points.get(Points.END).get(3));
            strokes.get(1).add(points.get(Points.INTER).get(1));
            strokes.get(1).sort(0, true);
            //第三横
            strokes.get(2).add(points.get(Points.END).get(4));
            strokes.get(2).add(points.get(Points.END).get(5));
            strokes.get(2).add(points.get(Points.INTER).get(2));
            strokes.get(2).sort(0, true);
            //竖
            strokes.get(3).addAll(points.get(Points.INTER));
        } catch (NullPointerException ignored) {
        }
    }
}
