package net.codebot.pdfviewer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.util.Log;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.widget.ImageView;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import static android.graphics.Bitmap.createBitmap;

@SuppressLint("AppCompatCustomView")
public class PDFimage extends ImageView implements Observer {

    final String LOGNAME = "pdf_image";

    Model model;
    // drawing path
    Path path = null;
    ArrayList<Path> paths = new ArrayList();
    ArrayList<Paint> paints = new ArrayList<>();
    ArrayList<PathData> points;
    boolean notfirst = false;
    float xFocus=0;
    float yFocus=0;
    float fullWidth;
    float fullHeight;

    // image to display
    Bitmap bitmap;
    Bitmap original;
    Paint paint = new Paint(Color.BLUE);
    PathData pathdata = null;
    ArrayList<Float> pair = null;
    ScaleGestureDetector mScaleDetector;
    private float mScaleFactor=1.f;
    Matrix matrix;

    public PDFimage(Context context) {
        super(context);
        model=Model.getInstance();
        model.addObserver(this);
        model.initObservers();
        paths=new ArrayList<>();
        paints= new ArrayList<>();
         points = new ArrayList<PathData>();
    }

    // capture touch events (down/move/up) to create a path
    // and use that to create a stroke that we can draw
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {

                case MotionEvent.ACTION_DOWN:
                    model.resetRedo();
                if (model.getAnnotate()||model.getHighlight()) {
                    notfirst = false;
                    Log.d(LOGNAME, "Action down");
                    path = new Path();
                    paint = new Paint(paint);
                    path.moveTo(event.getX(), event.getY());
                    pathdata=new PathData();
                    pair = new ArrayList<Float>();
                    pair.add(event.getX());
                    pair.add(event.getY());
                    pathdata.points.add(pair);
                    break;
                }
                if(model.getErase()){
                    for (int i=0;i<points.size();i++){
                        if(checkHit(points.get(i),event.getX(),event.getY())){
                            Log.d("in","in");
                           // Paint erasePaint = new Paint ();
                            Path p = paths.get(i);
                            Paint pai = paints.get(i);
                            PathData pts = points.get(i);
                            this.bitmap=original.copy(original.getConfig(),original.isMutable());
                            paths.remove(i);
                            paints.remove(i);
                            points.remove(i);
                            Log.d("size","system" +paths.size());
                            model.setPaints(paints);
                            model.setPaths(paths);
                            model.setPoints(points);
                            model.addActionErase(p,pai,pts);
                            invalidate();
                            break;
                        }
                    }
                    break;
                }

            case MotionEvent.ACTION_MOVE:
                if (model.getAnnotate()||model.getHighlight()) {
                    path.lineTo(event.getX(), event.getY());
                    if (notfirst) {
                        paths.remove(paths.size() - 1);
                    } else {
                        paints.add(paint);
                        notfirst = true;
                    }
                    pair = new ArrayList<Float>();
                    pair.add(event.getX());
                    pair.add(event.getY());
                    pathdata.points.add(pair);
                    paths.add(path);
                    break;
                }

            case MotionEvent.ACTION_UP:
                if (model.getAnnotate()||model.getHighlight()) {
                    Log.d(LOGNAME, "Action up");
                    if (notfirst) {
                        paths.remove(paths.size() - 1);
                    }else{
                        paints.add(paint);
                    }
                    notfirst = false;
                    paths.add(path);
                    points.add(pathdata);
                    model.setPaints(paints);
                    model.setPaths(paths);
                    model.setPoints(points);
                    model.addAction();
                    break;
                }
        }
        return true;
    }


    private void resetImage(){
        this.bitmap=original.copy(original.getConfig(),original.isMutable());
    }
    // Line defined by two points
    private boolean checkHit(PathData p, float x, float y){
        double distance;
        double numer;
        double denom;
        double y1;
        double y2;
        double x1;
        double x2;
        if(p.points.size()>1) {
            for (int i = 0; i < p.points.size() - 1; i++) {
                y1 = p.points.get(i).get(1);
                y2 = p.points.get(i + 1).get(1);
                x1 = p.points.get(i).get(0);
                x2 = p.points.get(i + 1).get(0);
                numer = ((y2 - y1) * x) - ((x2 - x1) * y) + (x2 * y1) - (y2 * x1);
                numer = Math.abs(numer);
                denom = (y2 - y1) * (y2 - y1);
                denom += (x2 - x1) * (x2 - x1);
                denom = Math.sqrt(denom);
                if (denom != 0) {
                    distance = numer / denom;
                } else if (x1 == x2) {
                    distance = (Math.abs(x1 - x));
                } else {
                    distance = (Math.abs(y1 - y));
                }
                if (distance <= 25) {
                    Log.d("worked", "w");
                    return true;
                }
            }
        }else if (p.points.size()==1){
            x1=p.points.get(0).get(0);
            y1=p.points.get(0).get(1);
            distance=(x-x1)*(x-x1);
            distance += (y-y1)*(y-y1);
            distance = Math.sqrt(distance);
            if (distance<25){
                return true;
            }
        }
        return false;
    }

    // set image as background
    public void setImage(Bitmap bitmap) {
        this.bitmap = bitmap;
        this.original = bitmap.copy(bitmap.getConfig(),bitmap.isMutable());
        paths.clear();
        paints.clear();
        points.clear();
        paths.addAll(model.getPaths());
        paints.addAll(model.getPaints());
        points.addAll(model.getPoints());
    }

    // set brush characteristics
    // e.g. color, thickness, alpha
    public void setBrush(Paint paint) {
        this.paint = paint;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        canvas.save();
        // draw background
        if (bitmap != null) {
            this.setImageBitmap(bitmap);
        }

        // draw lines over it
        for (int i=0;i<paths.size();i++) {
            canvas.drawPath(paths.get(i), paints.get(i));
        }

        model.setPaints(paints);
        model.setPaths(paths);
        model.setPoints(points);
        canvas.restore();
        super.onDraw(canvas);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void update(Observable o, Object arg) {
        // set the correct paint
        Paint p = new Paint();
        if(model.getAnnotate()){
            p.setColor(Color.BLUE);
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(4);
            setBrush(p);
        }if (model.getHighlight()){
            p.setColor(Color.argb(100,255,255,0));
            p.setStyle(Paint.Style.STROKE);
            p.setStrokeWidth(50);
            setBrush(p);
        }
        if(model.getUndoState()){
            EditAction u = model.getUndo();
            Log.d("undo","undo");
            if(u!=null){
                Log.d("undo","null nah");
                if(u.getAction()!=3){
                    paths.remove(paths.size()-1);
                    paints.remove(paints.size()-1);
                    points.remove(points.size()-1);
                    this.bitmap=original.copy(original.getConfig(),original.isMutable());
                }else{
                    paths.add(u.getPath());
                    points.add(u.getPoints());
                    paints.add(u.getPaint());
                }
                invalidate();
            }
        }
        if(model.getRedoState()) {
            EditAction r = model.getRedo();
            if (r != null) {
                if (r.getAction() == 3) {
                    paths.remove(r.getPath());
                    paints.remove(r.getPaint());
                    points.remove(r.getPoints());
                    this.bitmap=original.copy(original.getConfig(),original.isMutable());
                } else {
                    paths.add(r.getPath());
                    points.add(r.getPoints());
                    paints.add(r.getPaint());
                }
                invalidate();
            }
        }
    }
}
