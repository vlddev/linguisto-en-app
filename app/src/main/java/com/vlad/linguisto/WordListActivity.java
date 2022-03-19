package com.vlad.linguisto;

import android.graphics.Color;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.List;

public class WordListActivity extends AppCompatActivity {

    public static final String LOG_TAG = WordListActivity.class.getSimpleName();
    private List<Inf> wordList = new ArrayList<>();
    private int curPos = 0;

    private WebView webView;
    private Button btnKnown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_learn);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        btnKnown = (Button)findViewById(R.id.knowLearnWord);
        webView = (WebView)findViewById(R.id.learnWebView);

        initWordList();

        Button btnNext = (Button)findViewById(R.id.nextLearnWord);
        btnNext.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (curPos < wordList.size() - 1) {
                        curPos++;
                    } else {
                        curPos = 0;
                    }
                    showCurWord();
                }
            });

        Button btnPrev = (Button)findViewById(R.id.prevLearnWord);
        btnPrev.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (curPos > 0 ) {
                        curPos--;
                    } else {
                        curPos = wordList.size() - 1;
                    }
                    showCurWord();
                }
            });

        btnKnown.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Inf inf = wordList.get(curPos);
                        AppManager.getDictDbHelper().setInfKnown(inf.getId(), !inf.isKnown());
                        inf.setKnown(!inf.isKnown());
                        showCurWord();
                    }
                });

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        Log.e(LOG_TAG, "WordListActivity created");
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.e(LOG_TAG, "WordListActivity started");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_word_list, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_add_quiz_item) {
            if( AppManager.getDictDbHelper().addQuizInf(wordList.get(curPos).getId()) ) {
                Toast.makeText(this, getString(R.string.msg_word_added_to_quiz), Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, getString(R.string.msg_word_in_quiz), Toast.LENGTH_LONG).show();
            }
        }
        return super.onOptionsItemSelected(item);
    }

    private void initWordList() {
        AppManager appManager = AppManager.getInstance(getApplicationContext());
        setTitle(appManager.getWordListName());
        wordList = appManager.getWordList();
        WebView webView = (WebView)findViewById(R.id.learnWebView);
        showCurWord();
    }

    private void showCurWord() {
        if (wordList.get(curPos).isKnown()) {
            btnKnown.setBackgroundColor(Color.YELLOW);
            btnKnown.setText(getString(R.string.btn_unknown));
        } else {
            btnKnown.setBackgroundColor(Color.GREEN);
            btnKnown.setText(getString(R.string.btn_known));
        }
        webView.loadDataWithBaseURL(null, wordList.get(curPos).getHtml(), "text/html", "utf-8", null);
    }

}
