package com.brunschen.christian.graphic.android;

import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;

import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Rectangle;

public class AndroidFont implements Font {

  public Typeface typeface;
  public float size;
  
  private Paint paint;

  public AndroidFont(Typeface typeface, float size) {
    this.typeface = typeface;
    this.size = size;
    this.paint = new Paint();
    this.paint.setTypeface(typeface);
    this.paint.setTextSize(size);
  }

  @Override
  public Font atSize(double size) {
    return new AndroidFont(this.typeface, (float) size);
  }

  @Override
  public Rectangle measureString(String s) {
    return measureString(paint, s);
  }
  
  public static Rectangle measureString(Paint paint, String s) {
    double w = paint.measureText(s);
    return new Rectangle(0.0, paint.ascent(), w, paint.descent() - paint.ascent());
  }
  
  @Override
  public Rectangle getBoundingBox(String s) {
    android.graphics.Path androidPath = new android.graphics.Path();
    paint.getTextPath(s, 0, s.length(), 0, 0, androidPath);
    RectF bounds = new RectF();
    androidPath.computeBounds(bounds, true);
    return new Rectangle(bounds.left, bounds.top, bounds.width(), bounds.height());
  }
}
