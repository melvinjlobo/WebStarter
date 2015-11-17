package com.starter.wv.ui;

import android.app.ActionBar;
import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.artifex.mupdflib.FilePicker;
import com.artifex.mupdflib.MuPDFCore;
import com.artifex.mupdflib.MuPDFPageAdapter;
import com.artifex.mupdflib.MuPDFReaderView;
import com.starter.wv.R;

/**
 * Created by Melvin Lobo on 8/13/2015.
 * <p/>
 * Activity to show the PDF using Mupdf. The activity will be opened up as a dialog activity to give it the
 * style required
 *
 * @author Melvin Lobo
 */
public class ActivityPDFViewer extends Activity implements FilePicker.FilePickerSupport {

    //////////////////////////////////////// CLASS MEMBERS /////////////////////////////////////////
    /**
     * Static definitions
     */
    public static final String PDF_URL   = "pdfURL";

    /**
     * Listener to announce the closure of the PDF View
     */
    private PDFCloseListener mClosePDFViewListener;

    /**
     * The File path along with the name of the PDF to be shown
     */
    private String msFileName = null;

    //////////////////////////////////////// CLASS METHODS /////////////////////////////////////////
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.pdf_viewer_layout);

        if(savedInstanceState != null) {
            msFileName = savedInstanceState.getString(PDF_URL, "");
        }
        else {
            msFileName = getIntent().getExtras().getString(PDF_URL);
        }

        RelativeLayout mupdfWrapper = (RelativeLayout) findViewById(R.id.mupdf_wrapper);
        ImageView closePDF = (ImageView) findViewById(R.id.close_pdf_view);
        closePDF.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                if(mClosePDFViewListener != null)
                    mClosePDFViewListener.onPDFViewerClosed();

                finish();
            }
        });

        //Load MuPDF and show the PDF
        if((msFileName != null) && (!msFileName.isEmpty())) {
            MuPDFCore core;
            try {
                core = new MuPDFCore(this, msFileName);

                MuPDFReaderView mDocView = new MuPDFReaderView(this);
                mDocView.setAdapter(new MuPDFPageAdapter(this, this, core));
                mupdfWrapper.addView(mDocView, new ActionBar.LayoutParams(
                        ActionBar.LayoutParams.MATCH_PARENT, ActionBar.LayoutParams.WRAP_CONTENT));

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(PDF_URL, msFileName);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        msFileName = savedInstanceState.getString(PDF_URL, "");
    }

    @Override
    public void performPickFor(FilePicker picker) {

    }

    //////////////////////////////////////// INNER CLASSES /////////////////////////////////////////
    /**
     * Interface that will be triggered whenever close button on the pdfviewier is clicked
     *
     * @author Melvin Lobo
     */
    public interface PDFCloseListener
    {
        public void onPDFViewerClosed();
    }

}
