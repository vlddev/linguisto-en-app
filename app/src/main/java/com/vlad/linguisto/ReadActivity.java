package com.vlad.linguisto;

import androidx.appcompat.app.AppCompatActivity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;
//import com.vlad.linguisto.text.Builder;
import com.vlad.linguisto.text.Builder;
import com.vlad.linguisto.text.BuilderPOS;
import com.vlad.linguisto.text.DbDictionary;
import com.vlad.linguisto.text.PagedText;
import com.vlad.linguisto.text.Text;
import com.vlad.linguisto.text.TextPOS;
import com.vlad.linguisto.tools.PageCache;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class ReadActivity extends AppCompatActivity {

    public static final String LOG_TAG = ReadActivity.class.getSimpleName();
    public static final int PAGE_SIZE = 900;

    private DictDbHelper dbHelper = AppManager.getDictDbHelper();
    private WebView readWebView;
    private TextView footerTextView;
    private boolean usePosTagger = true;
    private PagedText pagedText = null;
    private int curPage = 0;
    private String curFileName = "";
    private final PageCache pageCache = new PageCache();
    private TextToSpeech tts;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppManager appManager = AppManager.getInstance(this);
        setContentView(R.layout.activity_read);
        //keep screen on (for reading)
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        readWebView = (WebView)findViewById(R.id.readWebView);
        readWebView.setWebViewClient(new ReadWebViewClient());
        readWebView.getSettings().setTextZoom(appManager.getTextZoom());

        footerTextView = (TextView)findViewById(R.id.footerTextView);
        footerTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12f);

        tts = new TextToSpeech(this.getApplicationContext(),
                new TextToSpeech.OnInitListener() {
                    @Override
                    public void onInit(int status) {
                        if (status != -1) {
                            tts.setLanguage(Locale.US);
                            tts.setSpeechRate(0.8F);
                        }
                    }
                }, "com.google.android.tts");

        prepareText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        AppManager appManager = AppManager.getInstance(this);
        if (appManager.getReadFile() != null) {
            appManager.setPageForFile(appManager.getReadFile(), curPage);
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        super.onDestroy();
    }

    private void prepareText() {
        try {
            AppManager appManager = AppManager.getInstance(this);
            String textToPrepare = "Please choose file to read.";
            if (appManager.getReadFile() != null) {
                File inFile = new File(AppManager.getInstance(this).getReadFile());
                BufferedReader br = new BufferedReader(new FileReader(inFile));
                String line;
                StringBuilder sb = new StringBuilder();
                while ((line = br.readLine()) != null) {
                    sb.append(line).append('\n');
                }
                br.close();
                textToPrepare = sb.toString();
                curFileName = inFile.getName();
                curPage = appManager.getPageForFile(appManager.getReadFile());
            }
            pagedText = new PagedText(textToPrepare, PAGE_SIZE);
            showPage(curPage);
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in Builder: " + e.getMessage(), e);
        }

    }

    public String getSearchAsHtml(String searchStr) {
        StringBuffer buffer = new StringBuffer();
        List<Inf> res = dbHelper.findInfs(searchStr);
        if (res.size() == 0) {
            buffer.append("Nothing found for <b>"+searchStr+"</b>");
        } else {
            String lastWord = "";
            for (Inf inf : res) {
                buffer.append(inf.getHtml()+"\n");
                if (inf.isKnown()) {
                    buffer.append(String.format("<a href=\"dict://unknown/%s\">set to unknown</a>", inf.getId()));
                } else {
                    buffer.append(String.format("<a href=\"dict://known/%s\">set to known</a>", inf.getId()));
                }
                lastWord = inf.getInf();
            }
            // add extended dict article
            String extDict = dbHelper.getExtDictArticle(lastWord);
            if (extDict.length() > 0) {
                buffer.append(extDict+"\n");
            }
        }
        return buffer.toString();
    }

    public String getInfAsHtml(String infIds) {
        StringBuffer buffer = new StringBuffer();
        List<Inf> res = dbHelper.getInfs(infIds);
        if (res.size() == 0) {
            buffer.append("Nothing found for id <b>"+infIds+"</b>");
        } else {
            String lastWord = "";
            for (Inf inf : res) {
                buffer.append(inf.getHtml()+"\n");
                buffer.append("<table style=\"width:100%\"><tr><td>");
                if (inf.isKnown()) {
                    buffer.append(String.format("<a href=\"dict://unknown/%s\">"+
                            getString(R.string.dlg_dict_set_to_unknown).toLowerCase()+"</a>", inf.getId()));
                } else {
                    buffer.append(String.format("<a href=\"dict://known/%s\">"+
                            getString(R.string.dlg_dict_set_to_known).toLowerCase()+"</a>", inf.getId()));
                }
                buffer.append("</td><td>");
                buffer.append(String.format("<div style=\"text-align:right\"><a href=\"dict://quizadd/%s\">"+
                        getString(R.string.menu_add_to_quiz).toLowerCase()+"</a>", inf.getId()));
                buffer.append("</td></tr></table>");
                lastWord = inf.getInf();
            }
            // add extended dict article
            String extDict = dbHelper.getExtDictArticle(lastWord);
            if (extDict.length() > 0) {
                buffer.append(extDict+"\n");
            }
        }
        return buffer.toString();
    }

    public void showDialog(String mode, String id) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.web_view_dialog, null);
        WebView webView = view.findViewById(R.id.dialogWebView);
        webView.setWebViewClient(new DictWebViewClient(mode, id));
        webView.loadDataWithBaseURL("file:///android_asset/", getDictContent(mode, id), "text/html", "utf-8", null);

        alert.setView(view);
        alert.show();
    }

    private void speakPage() {
        if (!this.tts.isSpeaking()) {
            this.tts.speak(this.pagedText.getPageText(this.curPage), 0, (HashMap)null);
        }
    }

    private void stopSpeaking() {
        if (this.tts.isSpeaking()) {
            this.tts.stop();
        }
    }

    protected String getDictContent(String mode, String ids) {
        if ("infid".equals(mode)) {
            return getInfAsHtml(ids);
        } else if ("word".equals(mode)) {
            return getSearchAsHtml(ids);
        } else {
            return "Unknown DictContent mode '"+mode+"'";
        }
    }

    private void showPage(int page) {
        if (page > -1 && page < pagedText.getPageCount()) {
            try {
                curPage = page;
                new PrepareTextTask().execute(page);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Can't show page. Error: " + e.getMessage(), e);
                Toast.makeText(getApplicationContext(), "Can't show page. Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            }
        }
    }

    private void goToPage() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.dlg_read_goto_page));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int pageNum = Integer.parseInt(input.getText().toString()) - 1;
                showPage(pageNum);
            }
        });
        builder.setNegativeButton(getString(R.string.btn_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        builder.show();

    }

    private class ReadWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Log.d(LOG_TAG, "DictWebViewClient: Load url: " + url);
            if (url.startsWith("dict://")) {
                if (url.startsWith("dict://infid/")) {
                    String infIds = url.replace("dict://infid/", "");
                    showDialog("infid", infIds);
                } else if (url.startsWith("dict://word/")) {
                    String strWord = url.replace("dict://word/", "");
                    showDialog("word", strWord);
                }
            } else if (url.startsWith("nav://")) {
                if (url.equals("nav://next")) {
                    showPage(curPage+1);
                } else if (url.equals("nav://prev")) {
                    showPage(curPage-1);
                } else if (url.equals("nav://first")) {
                    showPage(0);
                } else if (url.equals("nav://last")) {
                    showPage(pagedText.getPageCount()-1);
                } else if (url.equals("nav://goto")) {
                    goToPage();
                } else if (url.equals("nav://tts")) {
                    speakPage();
                } else if (url.equals("nav://stop-tts")) {
                    stopSpeaking();
                }
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            //Log.d(LOG_TAG, "readWebView MeasuredHeight: "+view.getMeasuredHeight());
            //Log.d(LOG_TAG, "readWebView Height: "+view.getHeight());
            Log.d(LOG_TAG, "readWebView onPageFinished()");
            super.onPageFinished(view, url);
            view.scrollTo(0,0);
        }
    }

    private class DictWebViewClient extends WebViewClient{

        String mode;
        String id;

        public DictWebViewClient(String mode, String id) {
            this.id = id;
            this.mode = mode;
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Log.d(LOG_TAG, "DictWebViewClient: Load url: " + url);
            if (url.startsWith("dict://")) {
                if (url.startsWith("dict://unknown/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://unknown/", ""));
                    dbHelper.setInfKnown(infId, false);
                    view.loadDataWithBaseURL(null, getDictContent(mode, id), "text/html", "utf-8", null);
                } else if (url.startsWith("dict://known/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://known/", ""));
                    dbHelper.setInfKnown(infId, true);
                    view.loadDataWithBaseURL(null, getDictContent(mode, id), "text/html", "utf-8", null);
                } else if (url.startsWith("dict://quizadd/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://quizadd/", ""));
                    if( dbHelper.addQuizInf(infId) ) {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_word_added_to_quiz), Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.msg_word_in_quiz), Toast.LENGTH_LONG).show();
                    }
                } else if (url.startsWith("dict://tts/")) {
                    String word = url.replace("dict://tts/", "");
                    tts.speak(word, 0, (HashMap)null);
                } else {
                    view.loadDataWithBaseURL(null, getSearchAsHtml(url.replace("dict://", "")), "text/html", "utf-8", null);
                }
            } else {
                view.loadDataWithBaseURL(null, "Can't process URL: "+url, "text/html", "utf-8", null);
            }
            return true;
        }
    }

    private class PrepareTextTask extends AsyncTask<Integer, Integer, String> {

        @Override
        protected void onPreExecute() {
            Toast.makeText(getApplicationContext(), "Load page ...", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected String doInBackground(Integer... page) {
            String ret = "";
            try {

                String strOut = "";
                if (pageCache.contains(curPage)) {
                    ret = pageCache.get(curPage);
                } else {
                    if (usePosTagger) {
                        BuilderPOS builder = new BuilderPOS();
                        TextPOS textObj = builder.makeText(pagedText.getPageText(curPage), new DbDictionary(dbHelper));
                        strOut = textObj.getHtml(Text.FIND_WORD_GERMAN);
                    } else {
                        Builder builder = new Builder();
                        Text textObj = builder.makeText(pagedText.getPageText(curPage), new DbDictionary(dbHelper));
                        strOut = textObj.getHtml(Text.FIND_WORD_GERMAN);
                    }
                    pageCache.put(curPage, strOut);
                    ret = strOut;
                }

                //Navigation
                StringBuffer sb = new StringBuffer();
                sb.append("<p><div style=\"width: 100%;\">")
                .append("  <div style=\"float: left; display: inline;\">")
                .append("    <a href=\"nav://prev\"><img style=\"margin:5px;\" src=\"icon/arrow-left-bold-outline-36.png\"/></a>")
                .append("    <a href=\"nav://goto\"><img style=\"margin:5px;\" src=\"icon/card-search-outline-36.png\"/></a>")
                .append("    <a href=\"nav://next\"><img style=\"margin:5px;\" src=\"icon/arrow-right-bold-outline-36.png\"/></a>")
                .append("  </div>")
                .append("  <div style=\"float: right; display: inline;\">")
                .append("    <a href=\"nav://tts\"><img style=\"margin:5px;\" src=\"icon/play-circle-outline-36.png\"/></a>")
                .append("    <a href=\"nav://stop-tts\"><img style=\"margin:5px;\" src=\"icon/stop-circle-outline-36.png\"/></a>")
                .append("  </div>")
                .append("</div></p>");
                ret +=  sb.toString();
            } catch (Exception e) {
                ret = e.getMessage();
            }
            return ret;
        }

//        @Override
//        protected void onProgressUpdate(Integer... progress) {
//            setProgressPercent(progress[0]);
//        }

        @Override
        protected void onPostExecute(String result) {
            readWebView.loadDataWithBaseURL("file:///android_asset/", result, "text/html", "utf-8", null);
            footerTextView.setText(getString(R.string.dlg_read_page)+" "+(curPage+1)+" "+
                    getString(R.string.dlg_read_from)+" "+pagedText.getPageCount() + " | " + curFileName);
        }
    }

}
