package com.brunschen.christian.graphic.android;

import static android.view.MotionEvent.ACTION_CANCEL;
import static android.view.MotionEvent.ACTION_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_DOWN;
import static android.view.MotionEvent.ACTION_POINTER_UP;
import static android.view.MotionEvent.ACTION_UP;
import android.R.color;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.RectF;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.EdgeEffect;
import android.widget.OverScroller;

import com.brunschen.christian.graphic.Graphic;
import com.brunschen.christian.graphic.GraphicParent;
import com.brunschen.christian.graphic.Point;
import com.brunschen.christian.graphic.Rectangle;

public class GraphicView extends View implements GraphicParent {  
  @SuppressWarnings("unused")
  private static final String TAG = GraphicView.class.getSimpleName();
  private Graphic graphic;
  private CanvasSurface surface = new CanvasSurface();
  
  private OverScroller scroller;
  private EdgeEffect edgeEffectLeft;
  private EdgeEffect edgeEffectTop;
  private EdgeEffect edgeEffectRight;
  private EdgeEffect edgeEffectBottom;
  private Adjustment adjustment = new Adjustment();
  
  private RectF rect = new RectF();
  private double scale;
  private double minScale;
  
  private GestureDetector gestureDetector;
  private ScaleGestureDetector scaleGestureDetector;
  private boolean eventsToGraphic;
  private GraphicPosition lastGraphicPosition;
    
  public GraphicView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    
    scroller = new OverScroller(context);
    edgeEffectLeft = new EdgeEffect(context);
    edgeEffectTop = new EdgeEffect(context);
    edgeEffectRight = new EdgeEffect(context);
    edgeEffectBottom = new EdgeEffect(context);

    gestureDetector = new GestureDetector(context, new GestureListener());    
    gestureDetector.setIsLongpressEnabled(false);
    
    scaleGestureDetector = new ScaleGestureDetector(context, new ScaleGestureListener());
  }

  public GraphicView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public GraphicView(Context context) {
    this(context, null, 0);
  }

  public void setGraphic(Graphic graphic) {
    boolean invalidate = graphic != this.graphic;
    if (this.graphic != null) {
      this.graphic.setParent(null);
    }
    this.graphic = graphic;
    if (this.graphic != null) {
      this.graphic.setParent(this);
      adjustGraphicFrame();
    }
    if (invalidate) {
      postInvalidateOnAnimation();
    }
  }
  
  @Override
  protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    edgeEffectTop.setSize(w, h);
    edgeEffectBottom.setSize(w, h);
    edgeEffectLeft.setSize(h, w);
    edgeEffectRight.setSize(h, w);
    if (oldw != 0 && oldh != 0) {
      lastGraphicPosition = new GraphicPosition(graphic, oldw, oldh);
    }
    adjustGraphicFrame();
    postInvalidateOnAnimation();
  }

  public void adjustGraphicFrame() {
    if (graphic == null) {
      return;
    }
    
    if (getWidth() == 0 || getHeight() == 0) {
      return;
    }

    Rectangle b = graphic.bounds();
    int width = getWidth();
    int height = getHeight();
    double xScale = width / b.width;
    double yScale = height / b.height;
    minScale = Math.min(xScale, yScale);

    if (lastGraphicPosition != null) {
      int dw = getWidth() - lastGraphicPosition.viewWidth;
      int dh = getHeight() - lastGraphicPosition.viewHeight;
      scale = lastGraphicPosition.scale;
      Rectangle frame = graphic.frame();
      frame.x = lastGraphicPosition.x + lastGraphicPosition.scale * dw / 2.0; 
      frame.y = lastGraphicPosition.y + lastGraphicPosition.scale * dh / 2.0;
      frame.width = graphic.bounds().width * lastGraphicPosition.scale;
      frame.height = graphic.bounds().height * lastGraphicPosition.scale;
      constrainFrame(frame);
      lastGraphicPosition = null;
    } else {
      scale = minScale;
      
      double w = b.width * scale;
      double h = b.height * scale;
      double x = (width - w) / 2;
      double y = (height - h) / 2;
  
      graphic.setFrame(new Rectangle(x, y, w, h));
    }
  }
  
  @Override
  public void computeScroll() {
    super.computeScroll();
    if (scroller.computeScrollOffset()) {
      Rectangle frame = graphic.frame();

      frame.x = scroller.getCurrX();
      frame.y = scroller.getCurrY();
      constrainFrame(frame);

      int v = Math.round(scroller.getCurrVelocity());
      if (adjustment.dx < 0) {
        // X was adjusted to the left - we encountered the left edge
        edgeEffectLeft.onAbsorb(v);
      } else if (adjustment.dx > 0) {
        // X was adjusted to the right - we encountered the right edge
        edgeEffectRight.onAbsorb(v);
      }
      
      if (adjustment.dy < 0) {
        // Y was adjusted towards the top - we encountered the top edge
        edgeEffectTop.onAbsorb(v);
      } else if (adjustment.dy > 0) {
        // Y was adjusted towards the bottom - we encountered the bottom edge
        edgeEffectBottom.onAbsorb(v);
      }

      postInvalidateOnAnimation();
    }
  }

  @Override
  protected void onDraw(Canvas canvas) {
    canvas.drawColor(getResources().getColor(color.background_light));
    if (graphic == null) {
      return;
    }
    
    Rectangle bounds = graphic.bounds();
    Rectangle frame = graphic.frame();

    int saveCount = canvas.save();
    canvas.translate((float) frame.x, (float) frame.y);
    canvas.scale((float) (frame.width / bounds.width), (float) (frame.height / bounds.height));
    canvas.translate((float) -bounds.x, (float) -bounds.y);
    rect.set((float) bounds.x, (float) bounds.y, (float) bounds.getMaxX(), (float) bounds.getMaxY());
    canvas.clipRect(rect);
    surface.setCanvas(canvas);
    graphic.draw(surface);
    canvas.restoreToCount(saveCount);
    
    saveCount = canvas.save();
    edgeEffectTop.draw(canvas);
    canvas.restoreToCount(saveCount);

    saveCount = canvas.save();
    canvas.rotate(180, getWidth() / 2.0f, getHeight() / 2.0f);
    edgeEffectBottom.draw(canvas);
    canvas.restoreToCount(saveCount);

    saveCount = canvas.save();
    canvas.rotate(270);
    canvas.translate(-getHeight(), 0);
    edgeEffectLeft.draw(canvas);
    canvas.restoreToCount(saveCount);

    saveCount = canvas.save();
    canvas.rotate(90);
    canvas.translate(0, -getWidth());
    edgeEffectRight.draw(canvas);
    canvas.restoreToCount(saveCount);

    boolean animate = !(edgeEffectTop.isFinished() && edgeEffectBottom.isFinished()
        && edgeEffectLeft.isFinished() && edgeEffectRight.isFinished());
    if (animate) {
      postInvalidateOnAnimation();
    }
  }

  @Override
  public void repaint(Rectangle r) {
    int x0 = (int) Math.floor(r.getMinX());
    int x1 = (int) Math.ceil(r.getMaxX());
    int y0 = (int) Math.floor(r.getMinY());
    int y1 = (int) Math.ceil(r.getMaxY());
    postInvalidateOnAnimation(x0, y0, x1, y1);
  }

  @Override
  public void repaint() {
    postInvalidateOnAnimation();
  }
  
  private double scrollToVisible(double currentOffset, double currentSize, double targetOffset, double targetSize) {
    double retval = currentOffset;
    if (currentSize >= targetSize) {
      // the entire target size fits in the available size
      if (targetOffset < 0) {
        retval = currentOffset - targetOffset;
      }
      double targetEnd = targetOffset + targetSize;
      if (targetEnd > currentSize) {
        retval = currentOffset - targetEnd + currentSize;
      }
    } else {
      double targetEnd = targetOffset + targetSize;      
      if (targetOffset <= 0) {
        if (currentSize > targetEnd) {
          retval = currentOffset - targetEnd + currentSize;
        }
      } else {
        retval = currentOffset - targetOffset;
      }
    }
    return retval;
  }

  @Override
  public void scrollRectToVisible(Rectangle r) {
    Rectangle frame = graphic.frame();
    double x = scrollToVisible(frame.x, getWidth(), r.x, r.width);
    double y = scrollToVisible(frame.y, getHeight(), r.y, r.height);
    frame.x = x;
    frame.y = y;
    constrainFrame(frame);
    postInvalidateOnAnimation();
  }

  @Override
  public Rectangle visibleRect() {
    Rectangle frame = graphic.frame();
    return new Rectangle(-frame.x, -frame.y, getWidth(), getHeight());
  }

  @Override
  public void revalidate() {
    adjustGraphicFrame();
    requestLayout();
    postInvalidateOnAnimation();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);
    
    double aspect = graphic.bounds().width / graphic.bounds().height; 

    int width;
    int height;

    // Measure Width
    if (widthMode == MeasureSpec.EXACTLY) {
      // Must be this size
      width = widthSize;
      if (heightMode == MeasureSpec.EXACTLY) {
        height = heightSize;
      } else if (heightMode == MeasureSpec.AT_MOST) {
        height = (int) Math.min(heightSize, Math.ceil(width / aspect));
      } else {
        height = (int) Math.max(heightSize, Math.ceil(width / aspect));
      }
    } else if (widthMode == MeasureSpec.AT_MOST) {
      // Can't be bigger than...
      if (heightMode == MeasureSpec.EXACTLY || heightMode == MeasureSpec.AT_MOST) {
        height = heightSize;
        width = widthSize;
      } else {
        height = heightSize;
        width = (int) Math.max(Math.ceil(height * aspect), widthSize);
      }
    } else {
      //Be whatever you want
      height = heightSize;
      width = (int) Math.ceil(height * aspect);
    }

    // MUST CALL THIS
    setMeasuredDimension(width, height);
  }

  @SuppressLint("ClickableViewAccessibility")
  @Override
  public boolean onTouchEvent(MotionEvent event) {
    int action = event.getActionMasked();
    
    Point p = new Point(event.getX(), event.getY());
    if (graphic != null) {
      boolean handled = false;
      Point q = graphic.frameToBounds(p);

      if (action == ACTION_DOWN) {
        if (graphic.bounds().contains(q)) {
          handled = graphic.mouseDown(q);
          eventsToGraphic = handled;
        }
      } else if (action == ACTION_POINTER_DOWN) {
        graphic.mouseUp(q);
        eventsToGraphic = false;        
      } else if (action == ACTION_UP) {
        if (graphic.bounds().contains(q)) {
          handled = graphic.mouseUp(q);
          eventsToGraphic = false;
        }
      } else if (action == ACTION_POINTER_UP) {
        // nothing specific to do
      } else if (action == MotionEvent.ACTION_MOVE) {
        if (eventsToGraphic) {
          if (graphic.bounds().contains(q)) {
            handled = graphic.mouseDragged(q);
          } else {
            handled = graphic.mouseEntered(q);
          }
        } else {
          // nothing to do
        }
      } else if (action == ACTION_CANCEL) {
        if (eventsToGraphic) {
          graphic.mouseUp(q);
          eventsToGraphic = false;
        }
      }
      
      if (!eventsToGraphic) {
        boolean scrollConsumed = gestureDetector.onTouchEvent(event);
        boolean scaleConsumed = scaleGestureDetector.onTouchEvent(event);
        handled |= scrollConsumed || scaleConsumed;
      } else {
        scroller.abortAnimation();
      }
      
      return handled || super.onTouchEvent(event);
    }
    
    return super.onTouchEvent(event);
  }

  public static double clamp(double v, double min, double max) {
    if (max < min) return clamp(v, max, min);
    if (v <= min) return min;
    if (v >= max) return max;
    return v;
  }

  public void saveGraphicFrame(Bundle outState, String prefix) {
    if (graphic != null) {
      Rectangle frame = graphic.frame(); 
      outState.putDouble(prefix + "x", frame.x);
      outState.putDouble(prefix + "y", frame.y);
      outState.putDouble(prefix + "w", frame.width);
      outState.putDouble(prefix + "h", frame.height);
    }
  }

  public void restoreGraphicFrame(Bundle state, String prefix) {
    if (graphic != null && !Double.isNaN(state.getDouble(prefix + "x", Double.NaN))) {
      Rectangle frame = graphic.frame();
      frame.x = state.getDouble(prefix + "x");
      frame.y = state.getDouble(prefix + "y");
      frame.width = state.getDouble(prefix + "width");
      frame.height = state.getDouble(prefix + "height");
    }
  }

  private void constrainFrame(Rectangle frame) {
    double x = frame.x;
    double y = frame.y;

    int width = getWidth();
    int height = getHeight();

    if (frame.width < width) {
      frame.x = (width - frame.width) / 2.0;
    } else {
      frame.x = clamp(frame.x, width - frame.width, 0);
    }

    if (frame.height < height) {
      frame.y = (height - frame.height) / 2.0;
    } else {
      frame.y = clamp(frame.y, height - frame.height, 0);
    }

    adjustment.dx = frame.x - x;
    adjustment.dy = frame.y - y;
  }
  
  public static class GraphicPosition {
    public int viewWidth;
    public int viewHeight;
    public double x;
    public double y;
    public double scale;

    public GraphicPosition(GraphicView graphicView) {
      this(graphicView.graphic, graphicView.getWidth(), graphicView.getHeight());
    }

    public GraphicPosition(Graphic graphic, int viewWidth, int viewHeight) {
      this.x = graphic.frame().x;
      this.y = graphic.frame().y;
      this.scale = graphic.frame().width / graphic.bounds().width;
      this.viewWidth = viewWidth;
      this.viewHeight = viewHeight;
    }
  }
  
  public GraphicPosition getGraphicPosition() {
    return new GraphicPosition(this);
  }
  
  public void setLastGraphicPosition(GraphicPosition graphicPosition) {
    lastGraphicPosition = graphicPosition;
  }

  private void flingScroller(double startX, double startY, double velocityX, double velocityY, double minX, double maxX, double minY, double maxY) {
    scroller.fling(
        (int) Math.round(startX),
        (int) Math.round(startY),
        (int) Math.round(velocityX),
        (int) Math.round(velocityY), 
        (int) Math.floor(minX),
        (int) Math.ceil(maxX), 
        (int) Math.floor(minY), 
        (int) Math.ceil(maxY));
  }
  
  private final static class Adjustment {
    public double dx;
    public double dy;
  }

  private final class ScaleGestureListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
    float previousSpan;

    @Override
    public boolean onScaleBegin(ScaleGestureDetector detector) {
      previousSpan = detector.getCurrentSpan();
      return true;
    }

    @Override
    public boolean onScale(ScaleGestureDetector detector) {
      if (graphic != null) {
        double dScale = detector.getCurrentSpan() / previousSpan;
        float focusX = detector.getFocusX();
        float focusY = detector.getFocusY();
        
        double newScale = Math.max(scale * dScale, minScale);
        dScale = newScale / scale;
        scale = newScale;

        Rectangle frame = graphic.frame();
        double dLeft = focusX - frame.x;
        double dTop = focusY - frame.y;
        double w = frame.width;
        double h = frame.height;
        frame.set(focusX - dLeft * dScale, focusY - dTop * dScale, w * dScale, h * dScale);
        constrainFrame(frame);
      }
      
      previousSpan = detector.getCurrentSpan();
      if (graphic != null) {
        postInvalidateOnAnimation();
        return true;
      } else {
        return false;
      }
    }

    @Override
    public void onScaleEnd(ScaleGestureDetector detector) {
      super.onScaleEnd(detector);
    }
  }

  private final class GestureListener extends GestureDetector.SimpleOnGestureListener {
    @Override
    public boolean onDown(MotionEvent e) {
      scroller.abortAnimation();
      return graphic != null;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float dx, float dy) {
      if (graphic == null) {
        return false;
      }
      
      scroller.abortAnimation();

      Rectangle frame = graphic.frame();
      frame.x -= dx;
      frame.y -= dy;
      constrainFrame(frame);
      
      if (adjustment.dx < 0) {
        // X was adjusted to the left - we encountered the left edge
        edgeEffectLeft.onPull((float) (-adjustment.dx / getWidth()));
        edgeEffectRight.onRelease();
      } else if (adjustment.dx > 0) {
        // X was adjusted to the right - we encountered the right edge
        edgeEffectRight.onPull((float) (adjustment.dx / getWidth()));
        edgeEffectLeft.onRelease();
      }
      
      if (adjustment.dy < 0) {
        // Y was adjusted towards the top - we encountered the top edge
        edgeEffectTop.onPull((float) (-adjustment.dy / getHeight()));
        edgeEffectBottom.onRelease();
      } else if (adjustment.dy > 0) {
        // Y was adjusted towards the bottom - we encountered the bottom edge
        edgeEffectBottom.onPull((float) (adjustment.dy / getHeight()));
        edgeEffectTop.onRelease();
      }

      postInvalidateOnAnimation();
      return true;
    }

    @Override
    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
      Rectangle frame = graphic.frame();
      int width = getWidth();
      int height = getHeight();

      float minX = Math.min(0, width - (float) frame.width);
      float maxX = Math.max(0, width - (float) frame.width);
      float minY = Math.min(0, height - (float) frame.height);
      float maxY = Math.max(0, height - (float) frame.height);
      
      flingScroller(frame.x, frame.y, velocityX, velocityY, minX, maxX, minY, maxY);

      postInvalidateOnAnimation();
      return true;
    }
  }
}
