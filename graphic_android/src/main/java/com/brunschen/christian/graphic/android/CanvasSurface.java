package com.brunschen.christian.graphic.android;

import java.util.Stack;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Paint.Style;
import android.graphics.Path.Direction;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.Log;

import com.brunschen.christian.graphic.AffineTransform;
import com.brunschen.christian.graphic.Color;
import com.brunschen.christian.graphic.Ellipse;
import com.brunschen.christian.graphic.Font;
import com.brunschen.christian.graphic.Image;
import com.brunschen.christian.graphic.Path;
import com.brunschen.christian.graphic.Path.Step;
import com.brunschen.christian.graphic.RadialGradient;
import com.brunschen.christian.graphic.Rectangle;
import com.brunschen.christian.graphic.StrokeStyle;
import com.brunschen.christian.graphic.Surface;

public class CanvasSurface extends Surface {
  private static final String TAG = CanvasSurface.class.getSimpleName();
  
  public Canvas canvas;
  private Paint paint = new Paint();
  private Matrix matrix = new Matrix();
  double scale;
  private Stack<Double> scales = new Stack<Double>();

  public CanvasSurface() {
  }

  public CanvasSurface(Canvas canvas) {
    this();
    setCanvas(canvas);
  }

  public void setCanvas(Canvas canvas) {
    this.canvas = canvas;
    scales.clear();
    scale = 2.0;
    paint.reset();
    paint.setFlags(Paint.ANTI_ALIAS_FLAG | Paint.SUBPIXEL_TEXT_FLAG | Paint.LINEAR_TEXT_FLAG);
  }
  
  @Override
  public void setFont(Font font) {
    super.setFont(font);
    if (font instanceof AndroidFont) {
      AndroidFont androidFont = (AndroidFont) font;
      paint.setTypeface(androidFont.typeface);
      paint.setTextSize(androidFont.size);
    } else {
      Log.w(TAG, "Unable to set font " + font);
    }
  }
  
  @Override
  public void transform(AffineTransform t) {
    scale *= t.xx;
    canvas.concat(toMatrix(t));
  }

  @Override
  public double getScale() {
    return scale;
  }

  @Override
  public void save() {
    canvas.save();
    scales.push(scale);
  }
  
  @Override
  public void restore() {
    canvas.restore();
    scale = scales.pop();
  }

  @Override
  public void scale(double sx, double sy) {
    scale *= sx;
    canvas.scale((float) sx, (float) sy);
  }

  @Override
  public void translate(double tx, double ty) {
    canvas.translate((float) tx, (float) ty); 
  }

  @Override
  public void rotate(double theta) {
    canvas.rotate((float) (180.0 * theta / Math.PI));
  }

  @Override
  public void fill(Rectangle r) {
    paint.setStyle(Style.FILL);
    canvas.drawRect(toRectF(r), paint);
  }

  @Override
  public void fill(Ellipse c) {
    paint.setStyle(Style.FILL);
    canvas.drawArc(toRectF(c), 0, 360, false, paint);
  }

  @Override
  public void fill(Path path) {
    paint.setStyle(Style.FILL);
    drawPath(path);
  }

  @Override
  public void stroke(Rectangle r) {
    paint.setStyle(Style.STROKE);
    canvas.drawRect(toRectF(r), paint);
  }

  @Override
  public void stroke(Ellipse c) {
    paint.setStyle(Style.STROKE);
    canvas.drawArc(toRectF(c), 0, 360, false, paint);
  }

  @Override
  public void stroke(Path path) {
    paint.setStyle(Style.STROKE);
    drawPath(path);
  }

  private void drawPath(Path path) {
    canvas.drawPath(toAndroidPath(path), paint);
  }
  
  @Override
  public void drawImage(Image image, double scale) {
    if (image instanceof BitmapImage) {
      Bitmap bitmap = ((BitmapImage) image).bitmap;
      matrix.setScale((float) scale, (float) scale);
      canvas.drawBitmap(bitmap, matrix, paint);
    }
  }

  private static int toAndroidColor(Color c) {
    int r = (int) Math.min(255, Math.max(0, 256 * c.r));
    int g = (int) Math.min(255, Math.max(0, 256 * c.g));
    int b = (int) Math.min(255, Math.max(0, 256 * c.b));
    int a = (int) Math.min(255, Math.max(0, 256 * c.a));
    return android.graphics.Color.argb(a, r, g, b);
  }
  
  @Override
  public void setColor(Color c) {
    paint.setShader(null);
    paint.setColor(toAndroidColor(c));
  }
  
  @Override
  public void setGradient(RadialGradient gradient) {
    if (gradient.cached == null || !(gradient.cached instanceof android.graphics.RadialGradient)) {
      float[] fractions = new float[gradient.distances.length];
      for (int i = 0; i < gradient.distances.length; i++) {
        fractions[i] = (float) gradient.distances[i];
      }
      
      int[] colors = new int[gradient.colors.length];
      for (int i = 0; i < gradient.colors.length; i++) {
        colors[i] = toAndroidColor(gradient.colors[i]);
      }
      
      gradient.cached = new android.graphics.RadialGradient((float) gradient.x, (float) gradient.y, (float) gradient.r,
          colors, fractions, Shader.TileMode.CLAMP);
    }
    android.graphics.RadialGradient androidGradient = (android.graphics.RadialGradient) gradient.cached;
    paint.setShader(androidGradient);
  }

  @Override
  public void setStrokeStyle(StrokeStyle strokeStyle) {
    paint.setStrokeWidth((float) strokeStyle.width); 
  }

  @Override
  public Image makeImage(int width, int height) {
    return new BitmapImage(width, height);
  }

  @Override
  public void clip(Path clip) {
    canvas.clipPath(toAndroidPath(clip));
  }

  @Override
  public void clip(Rectangle r) {
    canvas.clipRect((float) r.getMinX(), (float) r.getMinY(), (float) r.getMaxX(), (float) r.getMaxY());
  }

  @Override
  public void clip(Ellipse e) {
    android.graphics.Path p = new android.graphics.Path();
    p.addOval(toRectF(e), Direction.CCW);
    p.close();
    canvas.clipPath(p);
  }

  @Override
  public void drawString(String s, double x, double y) {
    paint.setStyle(Style.FILL);
    canvas.drawText(s, (float) x, (float) y, paint);
  }

  @Override
  public Rectangle measureString(String s) {
    if (font == null) {
      return Rectangle.empty();
    }
    if (font instanceof AndroidFont) {
      return AndroidFont.measureString(paint, s);
    } else {
      return font.measureString(s);
    }
  }

  @Override
  public void setTextAlignment(int alignment) {
    super.setTextAlignment(alignment);
    paint.setTextAlign(alignment == TEXT_ALIGNMENT_LEFT ? Align.LEFT
        : alignment == TEXT_ALIGNMENT_CENTER ? Align.CENTER
        : alignment == TEXT_ALIGNMENT_RIGHT ? Align.RIGHT
        : Align.LEFT);
  }
  
  @Override
  public Rectangle getBoundingBox(String s) {
    android.graphics.Path androidPath = new android.graphics.Path();
    paint.getTextPath(s, 0, s.length(), 0, 0, androidPath);
    RectF bounds = new RectF();
    androidPath.computeBounds(bounds, true);
    return new Rectangle(bounds.left, bounds.top, bounds.width(), bounds.height());
  }

  private static Matrix toMatrix(AffineTransform t) {
    Matrix m = new Matrix();
    m.setValues(new float[] {
        (float) t.xx, (float) t.xy, (float) t.tx,
        (float) t.yx, (float) t.yy, (float) t.ty,
        0, 0, 1
    });
    return m;
  }
  
  protected static final int toArgb(Color c) {
    int r = (int) Math.max(255, Math.min(0, 256 * c.r));
    int g = (int) Math.max(255, Math.min(0, 256 * c.g));
    int b = (int) Math.max(255, Math.min(0, 256 * c.b));
    int a = (int) Math.max(255, Math.min(0, 256 * c.a));
    return android.graphics.Color.argb(a, r, g, b);
  }
  
  protected android.graphics.Path toAndroidPath(Path path) {
    if (path.cached == null || !(path.cached instanceof android.graphics.Path)) {
      android.graphics.Path p = new android.graphics.Path();
      
      for (Path.Step step : path.steps) {
        double[] c = step.coords;
        switch(step.kind) {
        case Step.MOVE_TO:
          p.moveTo((float) c[0], (float) c[1]);
          break;
        case Step.LINE_TO:
          p.lineTo((float) c[0], (float) c[1]);
          break;
        case Step.ARC:
          RectF oval = new RectF((float) c[0], (float) c[1], (float) (c[0] + c[2]), (float) (c[1] + c[3]));
          if (c[5] >= 360.0) {
            p.addOval(oval, Direction.CW);
          } else {
            p.arcTo(oval, (float) (360 - (c[4] % 360)) % 360, (float) -c[5]);
          }
          break;
        case Step.QUADRATIC:
          p.quadTo((float) c[0], (float) c[1], (float) c[2], (float) c[3]);
          break;
        case Step.CUBIC:
          p.cubicTo((float) c[0], (float) c[1], (float) c[2], (float) c[3], (float) c[4], (float) c[5]);
          break;
        case Step.CLOSE_PATH:
          p.close();
          break;
        }
      }

      path.cached = p;
    }
    return (android.graphics.Path) path.cached;
  }

  private RectF toRectF(Rectangle r) {
    return new RectF((float) r.getMinX(), (float) r.getMinY(), (float) r.getMaxX(), (float) r.getMaxY());
  }

  private RectF toRectF(Ellipse r) {
    return new RectF((float) r.getMinX(), (float) r.getMinY(), (float) r.getMaxX(), (float) r.getMaxY());
  }

  @Override
  public boolean skip(double x, double y, double width, double height) {
    return canvas.quickReject((float) x, (float) y, (float) (x + width), (float) (y + height), Canvas.EdgeType.AA);
  }
}
