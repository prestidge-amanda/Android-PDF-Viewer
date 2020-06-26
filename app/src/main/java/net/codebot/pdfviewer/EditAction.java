package net.codebot.pdfviewer;

import android.graphics.Paint;
import android.graphics.Path;

public class EditAction {
    private int pageIndex;
    // 1 - annotate, 2 - highlight, 3 - erase
    private int action;
    private Paint paint;
    private Path path;
    private int opposite;
    private PathData points;

    EditAction(int pageIndex, int action, int opposite, Paint paint, Path path, PathData points){
            if (action == 1 || action == 2){
                this.action=3;
                this.pageIndex=pageIndex;
                this.paint=paint;
                this.opposite=action;
                this.path=path;
                this.points=points;
            }else{
                this.action=opposite;
                this.pageIndex=pageIndex;
                this.paint=paint;
                this.opposite=3;
                this.path=path;
                this.points=points;
            }

    }

    public int getAction() {
        return action;
    }

    public int getOpposite() {
        return opposite;
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public Paint getPaint() {
        return paint;
    }

    public Path getPath() {
        return path;
    }

    public PathData getPoints(){
        return points;
    }
}
