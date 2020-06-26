package net.codebot.pdfviewer;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.pdf.PdfRenderer;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Observable;
import java.util.Observer;

import static android.graphics.Color.rgb;

// PDF sample code from
// https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
// Issues about cache etc. are not at all obvious from documentation, so we should expect people to need this.
// We may wish to provied them with this code.

public class MainActivity extends AppCompatActivity implements Observer {

    final String LOGNAME = "pdf_viewer";
    final String FILENAME = "testfile.pdf";
    final int FILERESID = R.raw.testfile;

    Model model;
    TextView statusText;

    // manage the pages of the PDF, see below
    PdfRenderer pdfRenderer;
    private ParcelFileDescriptor parcelFileDescriptor;
    private PdfRenderer.Page currentPage;
    int currentIndex;

    // custom ImageView class that captures strokes and draws them over the image
    PDFimage pageImage;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        model=Model.getInstance();
        model.addObserver(this);

        LinearLayout layout = findViewById(R.id.pdfLayout);
        statusText= findViewById(R.id.pageStatus);
        pageImage = new PDFimage(this);
        layout.addView(pageImage);
        layout.setEnabled(true);
        pageImage.setMinimumWidth(1000);
        pageImage.setMinimumHeight(2000);

        // inflate menu

        // open page 0 of the PDF
        // it will be displayed as an image in the pageImage (above)
        try {
            openRenderer(this);
            showPage(model.getPageIndex());
           // closeRenderer();
        } catch (IOException exception) {
            Log.d(LOGNAME, "Error opening PDF");
        }

        model.initObservers();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.editting_menu, menu);
        return true;
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public boolean onOptionsItemSelected(MenuItem item){
        int id = item.getItemId();

        // still need to implement
        if (id==R.id.page_down){
            model.setPageIndexDown();
            showPage(model.getPageIndex());
        }else if (id==R.id.page_up){
            model.setPageIndexUp();
            showPage(model.getPageIndex());
        }else if (id==R.id.click){
            model.clearDraw();
            View v=findViewById(R.id.highlight);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.click);
            v.setBackgroundColor(rgb(0,87,75));
            v=findViewById(R.id.erase);
            v.setBackgroundColor(Color.TRANSPARENT);
            v = findViewById(R.id.annotate);
            v.setBackgroundColor(Color.TRANSPARENT);
        }
        else if (id==R.id.annotate){
            model.setAnnotate();
            View v = findViewById(R.id.annotate);
            v.setBackgroundColor(rgb(0,87,75));
            v=findViewById(R.id.highlight);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.erase);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.click);
            v.setBackgroundColor(Color.TRANSPARENT);
        }else if (id==R.id.erase){
            model.setErase();
            View v = findViewById(R.id.erase);
            v.setBackgroundColor(rgb(0,87,75));
            v=findViewById(R.id.highlight);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.annotate);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.click);
            v.setBackgroundColor(Color.TRANSPARENT);
        }else if (id==R.id.highlight){
            model.setHighlight();
            View v = findViewById(R.id.highlight);
            v.setBackgroundColor(rgb(0,87,75));
            v=findViewById(R.id.erase);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.annotate);
            v.setBackgroundColor(Color.TRANSPARENT);
            v=findViewById(R.id.click);
            v.setBackgroundColor(Color.TRANSPARENT);
        }else if (id==R.id.redo){
            Log.d("work","r");
            if (model.getRedoPage()!=-1 && model.getRedoPage()!=model.getPageIndex()){
                model.setPageIndex(model.getRedoPage());
                showPage(model.getPageIndex());
            }
            model.setRedoState(true);
        }else if (id==R.id.undo){
            Log.d("work","u");
            if(model.getUndoPage()!=-1&&model.getUndoPage()!=model.getPageIndex()){
                model.setPageIndex(model.getUndoPage());
                showPage(model.getPageIndex());
            }
            model.setUndoState(true);
        }
        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    protected void onStop() {
        super.onStop();
        try {
            closeRenderer();
        } catch (IOException ex) {
            Log.d(LOGNAME, "Unable to close PDF renderer");
        }
    }
// PDFRenderer from https://medium.com/@chahat.jain0/rendering-a-pdf-document-in-android-activity-fragment-using-pdfrenderer-442462cb8f9a
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void openRenderer(Context context) throws IOException {
        // In this sample, we read a PDF from the assets directory.
        File file = new File(context.getCacheDir(), FILENAME);
        if (!file.exists()) {
            // pdfRenderer cannot handle the resource directly,
            // so extract it into the local cache directory.
            InputStream asset = this.getResources().openRawResource(FILERESID);
            FileOutputStream output = new FileOutputStream(file);
            final byte[] buffer = new byte[1024];
            int size;
            while ((size = asset.read(buffer)) != -1) {
                output.write(buffer, 0, size);
            }
            asset.close();
            output.close();
        }
        parcelFileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);

        // capture PDF data
        // all this just to get a handle to the actual PDF representation
        if (parcelFileDescriptor != null) {
            pdfRenderer = new PdfRenderer(parcelFileDescriptor);
            model.setMaxPage(pdfRenderer.getPageCount());
            setTitle(file.getName());
        }
    }

    // do this before you quit!
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void closeRenderer() throws IOException {
        if (null != currentPage) {
            currentPage.close();
        }
        pdfRenderer.close();
        parcelFileDescriptor.close();
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void showPage(int index) {
        if (pdfRenderer.getPageCount() <= index) {
            return;
        }

        // Close the current page before opening another one.
        if (null != currentPage) {
            currentPage.close();
        }

        // Use `openPage` to open a specific page in PDF.
        currentPage = pdfRenderer.openPage(index);
        // Important: the destination bitmap must be ARGB (not RGB).
        Bitmap bitmap = Bitmap.createBitmap(currentPage.getWidth(), currentPage.getHeight(), Bitmap.Config.ARGB_8888);

        // Here, we render the page onto the Bitmap.
        // To render a portion of the page, use the second and third parameter. Pass nulls to get the default result.
        // Pass either RENDER_MODE_FOR_DISPLAY or RENDER_MODE_FOR_PRINT for the last parameter.
        currentPage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY);

        // Display the page
        pageImage.setImage(bitmap);
    }

    @Override
    public void update(Observable o, Object arg) {
        Log.d("update em","here");
        // Status bar update
        String t= "Page " + (model.getPageIndex()+1) +"/"+model.getMaxPage();
        statusText.setText(t);
    }
}
