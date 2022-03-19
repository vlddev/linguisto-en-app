package com.vlad.linguisto;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;
import com.vlad.linguisto.tools.QuizData;
import com.vlad.linguisto.tools.QuizQuestion;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizActivity extends AppCompatActivity {

    public static final String LOG_TAG = QuizActivity.class.getSimpleName();

    private DictDbHelper dictDbHelper;
    private QuizData quizData;
    private AppManager appManager;

    private RadioGroup radioGroupQuiz;
    private Button checkQuizWord;
    private ProgressBar progressQuiz;
    private TextView textQuizQuestion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        radioGroupQuiz = findViewById(R.id.radioGroupQuiz);
        checkQuizWord = findViewById(R.id.checkQuizWord);
        progressQuiz = findViewById(R.id.progressQuiz);
        textQuizQuestion = findViewById(R.id.textQuizInf);
        textQuizQuestion.setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f);

        dictDbHelper = AppManager.getDictDbHelper();
        appManager = AppManager.getInstance(getApplicationContext());
        quizData = appManager.getQuizData();
        quizData.init();

        showCurWord();

        final Button btnNext = findViewById(R.id.nextQuizWord);
        btnNext.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        quizData.fetchNextWord();
                        showCurWord();
                    }
                });

        final Button btnShow = findViewById(R.id.showQuizWord);
        btnShow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDictArticle();
            }
        });

        checkQuizWord.setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        checkAnswer();
                    }
                });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_quiz, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_remove_quiz_item) {
            quizData.removeCurrentQuizItem();
            showCurWord();
        } else if (id == R.id.action_mark_known_remove_quiz_item) {
            Inf inf = quizData.getCurWord();
            if (inf.isKnown()) {
                //set recap date to curdate
                dictDbHelper.setInfRecapDate(inf.getId());
                Toast.makeText(this, getString(R.string.msg_you_have_recapped)+" "+inf.getInf()+"!", Toast.LENGTH_LONG).show();
            } else {
                //set word to known
                dictDbHelper.setInfKnown(inf.getId(),true);
                Toast.makeText(this, getString(R.string.msg_you_have_learned)+" "+inf.getInf()+"!", Toast.LENGTH_LONG).show();
            }
            quizData.removeCurrentQuizItem();
            showCurWord();
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkAnswer() {
        int rbId = radioGroupQuiz.getCheckedRadioButtonId();
        if (rbId > 0) {
            RadioButton btn = (RadioButton)findViewById(rbId);
            Log.d(LOG_TAG, "Selected answer: "+btn.getText());
            QuizQuestion quizQuestion = quizData.getCurQuestion();
            Inf inf = quizQuestion.getInf();
            boolean bWordLearned = false;
            if (quizQuestion.getCorrectAnswer().equals(btn.getText().toString())) { //correct answer
                inf.setQuizScore(inf.getQuizScore()+1);
                if (inf.getQuizScore() >= appManager.getMaxQuizScore()) {
                    // word learned
                    bWordLearned = true;
                } else {
                    dictDbHelper.updateQuizInf(inf.getId(), inf.getQuizScore());
                }
            } else {//wrong answer
                inf.setQuizScore(inf.getQuizScore()-1);
                dictDbHelper.updateQuizInf(inf.getId(), inf.getQuizScore());
            }

            if (bWordLearned) {
                if (inf.isKnown()) {
                    //set recap date to curdate
                    dictDbHelper.setInfRecapDate(inf.getId());
                    Toast.makeText(this, getString(R.string.msg_you_have_recapped)+" "+inf.getInf()+"!", Toast.LENGTH_LONG).show();
                } else {
                    //set word to known
                    dictDbHelper.setInfKnown(inf.getId(),true);
                    Toast.makeText(this, getString(R.string.msg_you_have_learned)+" "+inf.getInf()+"!", Toast.LENGTH_LONG).show();
                }
                quizData.removeCurrentQuizItem();
                showCurWord();
            } else {
                //show correct answer
                //disable
                for(int i = 0; i < radioGroupQuiz.getChildCount(); i++){
                    RadioButton rb = (RadioButton)radioGroupQuiz.getChildAt(i);
                    if (quizQuestion.getCorrectAnswer().equals(rb.getText().toString())) {
                        rb.setBackgroundColor(Color.GREEN);
                    }
                    rb.setEnabled(false);
                }
                checkQuizWord.setEnabled(false);
                updateProgresQuiz(inf.getQuizScore());
            }
        } else {
            Toast.makeText(this, getString(R.string.msg_please_select_answer), Toast.LENGTH_SHORT).show();
        }
    }

    private void showDictArticle() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.web_view_dialog, null);
        WebView webView = view.findViewById(R.id.dialogWebView);
        Inf inf = quizData.getCurWord();

        String strArticle = inf.getHtml();
        // add extended dict article
        String extDict = dictDbHelper.getExtDictArticle(inf.getInf());
        if (extDict.length() > 0) {
            strArticle += (extDict+"\n");
        }

        webView.loadDataWithBaseURL(null, strArticle, "text/html", "utf-8", null);

        alert.setView(view);
        alert.show();
    }

    private void updateProgresQuiz(int score) {
        int curPr = score*100/AppManager.getInstance(getApplicationContext()).getMaxQuizScore();
        progressQuiz.setProgress(curPr);
    }

    private void showCurWord() {
        QuizQuestion quizQuestion = quizData.getCurQuestion();
        for (int i = 1; i < 5 && i < quizQuestion.getAnswers().size()+1; i++) {
            RadioButton btn = findViewById(getResources().getIdentifier("rbQuizAnswer" + i, "id", getPackageName()));
            btn.setText(quizQuestion.getAnswers().get(i-1));
        }
        textQuizQuestion.setText(quizQuestion.getQuestion());

        radioGroupQuiz.clearCheck();
        for(int i = 0; i < radioGroupQuiz.getChildCount(); i++){
            RadioButton rb = (RadioButton)radioGroupQuiz.getChildAt(i);
            rb.setBackgroundColor(android.R.drawable.btn_radio);
            rb.setEnabled(true);
        }
        checkQuizWord.setEnabled(true);
        updateProgresQuiz(quizQuestion.getInf().getQuizScore());
    }

}