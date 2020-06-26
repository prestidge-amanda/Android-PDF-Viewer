package net.codebot.pdfviewer;

import android.graphics.Paint;
import android.graphics.Path;
import android.util.Log;

import java.util.*;

/** Adapted model from:
 * MVC2 Model
 * Created by J. J. Hartmann on 11/19/2017.
 * Email: j3hartma@uwaterloo.ca
 * Copyright 2017
 */

class Model extends Observable
{
    // Create static instance of this mModel
    private static Model ourInstance;
    static Model getInstance()
    {
        if (ourInstance == null) {
            ourInstance = new Model();
        }
        return ourInstance;
    }

    // Private Variables
    private int pageIndex;
    private int maxPage=-1;
    private boolean annotate;
    private boolean highlight;
    private boolean click;
    private boolean erase;
    private boolean undo;
    private boolean redo;
    private float zoom;
    private float maxZoom;
    ArrayList<ArrayList<Path>> paths = new ArrayList<ArrayList<Path>>();
    ArrayList<ArrayList<PathData>> points = new  ArrayList<ArrayList<PathData>>();
    ArrayList<ArrayList<Paint>> paints = new ArrayList<ArrayList<Paint>>();
    Deque<EditAction> undoStack = new ArrayDeque<EditAction>();
    Deque<EditAction> redoStack = new ArrayDeque<EditAction>();
    /**
     * Model Constructor:
     * - Init member variables
     */
    Model() {
        annotate=false;
        highlight=false;
        erase=false;
        pageIndex = 0;
        undo=false;
        click=true;
        zoom=1f;
        redo=false;
    }

    public void setAnnotate(){
        annotate=true;
        highlight=false;
        click=false;
        erase=false;
        setChanged();
        notifyObservers();
    }

    public void setMaxZoom(float num){
        maxZoom=num;
    }

    public void setZoom(float num){
        zoom=num;
    }

    public boolean getClick(){
        return click;
    }

    public float getZoom(){
        return zoom;
    }

    public void clearDraw(){
        annotate=false;
        highlight=false;
        erase=false;
        click=true;
    }

    public void setHighlight(){
        highlight=true;
        click=false;
        annotate=false;
        erase=false;
        setChanged();
        notifyObservers();
    }

    public void setErase(){
        highlight=false;
        annotate=false;
        click=false;
        erase=true;
        setChanged();
        notifyObservers();
    }

    public boolean getRedoState(){
        return redo;
    }

    public boolean getUndoState(){
        return undo;
    }

    public void setRedoState(boolean b){
        redo=b;
        setChanged();
        notifyObservers();
    }

    public void setUndoState(boolean b){
        undo=b;
        setChanged();
        notifyObservers();
    }

    public boolean getAnnotate(){
        return annotate;
    }

    public boolean getHighlight(){
        return highlight;
    }

    public boolean getErase(){
        return erase;
    }

    public int getPageIndex(){
        return pageIndex;
    }

    public void setPageIndexDown(){
        if(pageIndex+1<maxPage){
            pageIndex++;
        }
        Log.d("index","changing");
        setChanged();
        notifyObservers();
    }

    public void addAction(){
        int action=3;
        int opposite=1;
        if (highlight){
            opposite=2;
            action=3;
        }
        EditAction u = new EditAction(pageIndex,action,opposite,paints.get(pageIndex).get(paints.get(pageIndex).size()-1),
                paths.get(pageIndex).get(paths.get(pageIndex).size()-1),points.get(pageIndex).get(points.get(pageIndex).size()-1));
        undoStack.push(u);
    }

    public void addActionErase(Path p, Paint pai,PathData pt){
        int opposite=3;
        int action=1;
        if(pai.getStrokeWidth()==10){
                action=2;
        }
        EditAction u = new EditAction(pageIndex,action,opposite,pai,p,pt);
        undoStack.push(u);
    }

    public void addRedo(){
        EditAction u=undoStack.getFirst();
        EditAction r= new EditAction(u.getPageIndex(),u.getOpposite(),u.getAction(),u.getPaint(),u.getPath(),u.getPoints());
        redoStack.push(r);
        undoStack.pop();
    }

    public void setPageIndex(int i){
        pageIndex=i;
    }

    public void resetRedo(){
        redoStack.clear();
        redoStack=new ArrayDeque<>();
    }

    public int getRedoPage(){
        if (redoStack.size()>0){
            return redoStack.peek().getPageIndex();
       }else{
            return -1;
        }
    }

    public int getUndoPage(){
        if (undoStack.size()>0){
            return undoStack.peek().getPageIndex();
        }else{
            return -1;
        }
    }

    public EditAction getUndo(){
        undo=false;
        if(undoStack.size()>0) {
            EditAction u1 = undoStack.pop();
            EditAction r = new EditAction(u1.getPageIndex(),u1.getOpposite(),u1.getAction(),u1.getPaint(),u1.getPath(),u1.getPoints());
            redoStack.push(r);
            return u1;
        }else{
            return null;
        }
    }

    public EditAction getRedo(){
        redo=false;
        if (redoStack.size()>0){
            EditAction r= redoStack.pop();
            EditAction u = new EditAction(r.getPageIndex(),r.getOpposite(),r.getAction(),r.getPaint(),r.getPath(),r.getPoints());
            undoStack.push(u);
            return r;
        }else{
            return null;
        }

    }

    public void setPageIndexUp(){
        if(pageIndex>0){
            pageIndex--;
        }
        Log.d("index","changing");
        setChanged();
        notifyObservers();
    }

    public void setMaxPage(int i){
        int prev = maxPage;
        maxPage=i;
        if (prev == -1){
            makePageArray();
        }
        notifyObservers();
    }

    private void makePageArray(){
        for (int i=0;i<maxPage;i++){
            paths.add(new ArrayList<Path>());
            paints.add(new ArrayList<Paint>());
            points.add(new ArrayList<PathData>());
        }
    }

    public ArrayList<Path>  getPaths(){
        return paths.get(pageIndex);
    }

    public ArrayList<Paint>  getPaints(){
        return paints.get(pageIndex);
    }

    public ArrayList<PathData> getPoints(){
        return points.get(pageIndex);
    }

    public void setPaths(ArrayList<Path> p){
        paths.set(pageIndex,new ArrayList<Path>(p));
    }

    public void setPaints(ArrayList<Paint> p){
        paints.set(pageIndex,new ArrayList<Paint>(p));
    }

    public void setPoints(ArrayList<PathData> p){points.set(pageIndex,new ArrayList<PathData>(p));}

    public int getMaxPage(){
        return maxPage;
    }


    /**
     * Helper method to make it easier to initialize all observers
     */
    public void initObservers()
    {
        setChanged();
        notifyObservers();
    }

    /**
     * Deletes an observer from the set of observers of this object.
     * Passing <CODE>null</CODE> to this method will have no effect.
     *
     * @param o the observer to be deleted.
     */
    @Override
    public synchronized void deleteObserver(Observer o)
    {
        super.deleteObserver(o);
    }

    /**
     * Adds an observer to the set of observers for this object, provided
     * that it is not the same as some observer already in the set.
     * The order in which notifications will be delivered to multiple
     * observers is not specified. See the class comment.
     *
     * @param o an observer to be added.
     * @throws NullPointerException if the parameter o is null.
     */
    @Override
    public synchronized void addObserver(Observer o)
    {
        super.addObserver(o);
    }

    /**
     * Clears the observer list so that this object no longer has any observers.
     */
    @Override
    public synchronized void deleteObservers()
    {
        super.deleteObservers();
    }

    /**
     * If this object has changed, as indicated by the
     * <code>hasChanged</code> method, then notify all of its observers
     * and then call the <code>clearChanged</code> method to
     * indicate that this object has no longer changed.
     * <p>
     * Each observer has its <code>update</code> method called with two
     * arguments: this observable object and <code>null</code>. In other
     * words, this method is equivalent to:
     * <blockquote><tt>
     * notifyObservers(null)</tt></blockquote>
     *
     * @see Observable#clearChanged()
     * @see Observable#hasChanged()
     * @see Observer#update(Observable, Object)
     */
    @Override
    public void notifyObservers()
    {
        super.notifyObservers();
    }
}

