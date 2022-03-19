package com.vlad.linguisto.tools;

import android.annotation.SuppressLint;
import android.util.Log;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class QuizData {

    public static final String LOG_TAG = QuizData.class.getSimpleName();

    private final DictDbHelper dictDbHelper;
    private final QuizInfQueue quizInfQueue;
    private List<Inf> wordList = new ArrayList<>();
    private static final int minWordListSize = 40;
    private int curPos = 0;
    private boolean bInited = false;

    public QuizData(DictDbHelper dbHelper) {
        this.dictDbHelper = dbHelper;
        this.quizInfQueue = new QuizInfQueue(dbHelper);
    }

    public void init() {
        if (!bInited) {
            bInited = true;
            wordList = dictDbHelper.getQuizInfs();
            if (wordList.size() < 1) {
                for (int i = 0; i < minWordListSize; i++) {
                    Inf inf = quizInfQueue.poll();
                    dictDbHelper.addQuizInf(inf.getId());
                }
                wordList = dictDbHelper.getQuizInfs();
            }
            Collections.shuffle(wordList);
        }
    }

    public Inf getCurWord() {
        return wordList.get(curPos);
    }

    public QuizQuestion getCurQuestion() {
        return prepareQuestion(getCurWord());
    }


    public void fetchNextWord() {
        if (curPos < wordList.size() - 1) {
            curPos++;
        } else {
            curPos = 0;
        }
    }

    public void removeCurrentQuizItem() {
        Inf inf = wordList.get(curPos);
        Log.d(LOG_TAG, "Remove word ["+inf.getId()+","+inf.getInf()+ "] from quiz.");
        dictDbHelper.removeQuizInf(inf.getId());
        wordList.remove(curPos);
        Log.d(LOG_TAG, "WordList.size = "+wordList.size());
        maybeAddQuizItems();
    }

    private void maybeAddQuizItems() {
        int itemsToAdd = QuizData.minWordListSize - wordList.size();
        if (itemsToAdd > 0) {
            List<Inf> newInfLst = quizInfQueue.poll(itemsToAdd);
            if (newInfLst.size() > 0) {
                for (Inf inf : newInfLst) {
                    if (dictDbHelper.addQuizInf(inf.getId())) {
                        wordList.add(inf);
                        Log.d(LOG_TAG, "Add word ["+inf.getId()+","+inf.getInf()+","+inf.isKnown()+ "] to quiz.");
                    }
                }
            }
        }
        Log.d(LOG_TAG, "WordList.size = "+wordList.size());
    }

    private QuizQuestion prepareQuestion(Inf inf) {
        QuizQuestion ret = null;
        if (inf.getQuizScore()%2 == 1) {
            ret = prepareReverseQuestion(inf);
        } else {
            ret = prepareEngQuestion(inf);
        }
        return ret;
    }

    /*
     * prepare (english) word as a question
     * user have to find correct (ukrainian) translation for it from several answers
     */
    @SuppressLint("SetTextI18n")
    private QuizQuestion prepareEngQuestion(Inf inf) {
        QuizQuestion ret = new QuizQuestion();
        ret.setInf(inf);
        ret.setCorrectAnswer(getTranslationForQuiz(inf));

        List<String> answers = new ArrayList<>();
        answers.add(ret.getCorrectAnswer());

        int cnt = 0;
        do {
            //List<Inf> wrongAnswers = appManager.getInfCache().getInfsRandom(inf.getType(), 3);
            List<Inf> wrongAnswers = dictDbHelper.getInfsRandom(inf.getType(), 3);
            for (Inf wInf : wrongAnswers) {
                if (wInf.getId().equals(inf.getId())) continue;
                if (wInf.getTrList().size() > 0) {
                    String str = wInf.getTrList().get(0).getTranslation();
                    boolean bAddCase = false;
                    if (str.length() > 5) {
                        if (!ret.getCorrectAnswer().startsWith(str.substring(0, 5))) {
                            bAddCase = true;
                        }
                    } else if (str.length() > 0) {
                        if (!ret.getCorrectAnswer().startsWith(str) && !str.startsWith(ret.getCorrectAnswer())) {
                            bAddCase = true;
                        }
                    }
                    if (bAddCase) {
                        String tr = getTranslationForQuiz(wInf);
                        if (!tr.contains("[[")) {
                            answers.add(removeCommentWithEng(tr));
                        }
                    }
                }
                if (answers.size() == 4) break;
            }
            cnt++;
        } while (answers.size() < 4 && cnt < 3);

        Collections.shuffle(answers);
        ret.setAnswers(answers);
        ret.setQuestion(inf.getInf() + "  " + inf.getTranscription() + "  " + inf.getTypeAsString());

        return ret;
    }

    /*
     * prepare (ukrainian) translation as a question
     * user have to find correct (english) word for it from several answers
     */
    private QuizQuestion prepareReverseQuestion(Inf inf) {
        QuizQuestion ret = new QuizQuestion();
        ret.setInf(inf);
        ret.setCorrectAnswer(inf.getInf());
        List<String> answers = new ArrayList<>();
        answers.add(ret.getCorrectAnswer());

        int cnt = 0;
        do {
            //List<Inf> wrongAnswers = appManager.getInfCache().getInfsRandom(inf.getType(), 3);
            List<Inf> wrongAnswers = dictDbHelper.getInfsRandom(inf.getType(), 3);
            for (Inf wInf : wrongAnswers) {
                if (wInf.getId().equals(inf.getId())) continue;
                String str = wInf.getInf();
                boolean bAddCase = false;
                // prevent similar words
                if (str.length() > 5) {
                    if (!ret.getCorrectAnswer().startsWith(str.substring(0, 5))) {
                        bAddCase = true;
                    }
                } else if (str.length() > 0) {
                    if (!ret.getCorrectAnswer().startsWith(str) && !str.startsWith(ret.getCorrectAnswer())) {
                        bAddCase = true;
                    }
                }
                if (bAddCase) {
                    answers.add(removeCommentWithEng(str));
                }
                if (answers.size() == 4) break;
            }
            cnt++;
        } while (answers.size() < 4 && cnt < 3);

        Collections.shuffle(answers);
        ret.setAnswers(answers);

        String strQuestion = getTranslationForQuiz(inf);
        ret.setQuestion(strQuestion);

        return ret;
    }

    private String getTranslationForQuiz(Inf inf) {
        StringBuffer ret = new StringBuffer();
        if (inf.getTrList().size() > 0) {
            // first translation
            ret.append(inf.getTrList().get(0).getTranslation());
            if (ret.length() > 0 && inf.getTrList().size() > 1)
                ret.append("\n");
            // first word / fraze from translations 2-4
            for (int i = 1; i < inf.getTrList().size() && i < 4; i++) {
                String tr = inf.getTrList().get(i).getTranslation();
                if (tr != null && tr.length() > 0) {
                    String[] trSplit = tr.split("[;¦]",2);
                    if (trSplit.length > 0) {
                        tr = trSplit[0];
                    }
                    if (ret.length() + tr.length() > 100) {
                        break;
                    }
                    if (i > 1) {
                        ret.append("; ");
                    }
                    ret.append(tr);
                }
            }
        }
        return removeCommentWithEng(ret.toString().replace('¦', ';'));
    }

    private String removeCommentWithEng(String str) {
        return str.replaceAll("\\([^)(]*?[a-zA-Z']+?[^)(]*?\\)", "").trim();
    }

}
