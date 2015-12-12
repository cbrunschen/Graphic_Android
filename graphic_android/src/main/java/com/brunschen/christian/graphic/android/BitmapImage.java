package com.brunschen.christian.graphic.android;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.DisplayMetrics;

import com.brunschen.christian.graphic.Image;
import com.brunschen.christian.graphic.Surface;

public class BitmapImage implements Image {
  
  protected Bitmap bitmap;

  public BitmapImage(int width, int height) {
    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
  }
  
  public BitmapImage(DisplayMetrics displayMetrics, int width, int height) {
    bitmap = Bitmap.createBitmap(displayMetrics, width, height, Bitmap.Config.ARGB_8888);
  }
  
  @Override
  public int getWidth() {
    return bitmap.getWidth();
  }

  @Override
  public int getHeight() {
    return bitmap.getHeight();
  }

  @Override
  public Surface makeSurface() {
    return new CanvasSurface(new Canvas(bitmap));
  }

  public Bitmap getBitmap() {
    return bitmap;
  }
}
