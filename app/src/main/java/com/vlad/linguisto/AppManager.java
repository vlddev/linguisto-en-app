package com.vlad.linguisto;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;
import com.vlad.linguisto.tools.QuizData;

import java.util.ArrayList;
import java.util.List;

import static android.content.Context.MODE_PRIVATE;

// Singleton AppManager
public class AppManager {
    private static AppManager instance = null;

    private static DictDbHelper dbHelper;
    private String readFile = null;
    private final Context context;

    private final QuizData quizData;

    public static String knownWordsFile = "knownWords.txt";
    private static final String sharedPropFile = "linguisto.app";
    public static final String BASE_URL_ASSET = "file:///android_asset/";
    public static List allWords = new ArrayList();

    private int maxQuizScore = 6;
    private final int maxRecentFiles = 5;
    private final List<String> recentFiles = new ArrayList<>();
    private List<Inf> wordList = new ArrayList<>();
    private String wordListName;

    public static AppManager getInstance(Context context) {
        if (instance == null) {
            instance = new AppManager(context);
        }
        return instance;
    }

    private AppManager(Context context) {
        this.context = context;
        dbHelper = new DictDbHelper(context);
        dbHelper.initGlobalData();
        this.quizData = new QuizData(dbHelper);
        readPreferences();
    }

    protected void finalize() {
        dbHelper.close();
    }

    public String getContextString(int resId) {
        return context.getString(resId);
    }

    public static DictDbHelper getDictDbHelper() {
        return dbHelper;
    }

    public QuizData getQuizData() {
        quizData.init();
        return quizData;
    }

    public String getReadFile() {
        return readFile;
    }

    public void setReadFile(String readFile) {
        this.readFile = readFile;
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putString("readFile", readFile);
        prefEditor.commit();
        // add to recent files
        addRecentFile(readFile);
    }

    public int getPageForFile(String file) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        return sharedPreferences.getInt("lastReadPage:"+file, 0);
    }

    public void setPageForFile(String file, int page) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        prefEditor.putInt("lastReadPage:"+file, page);
        prefEditor.commit();
    }

    private void addRecentFile(String file) {
        if (!recentFiles.contains(file)) {
            if (recentFiles.size() < maxRecentFiles) {
                recentFiles.add(0, file);
            } else {
                //remove last file
                removePreferencesForFile(recentFiles.get(recentFiles.size()-1));
                recentFiles.remove(recentFiles.size()-1);
                recentFiles.add(0, file);
            }
        }
        //refresh properties
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        for (int i = 0; i < recentFiles.size(); i++) {
            prefEditor.putString("recentFile"+i, recentFiles.get(i));
        }
        prefEditor.commit();
    }

    public List<String> getRecentFiles() {
        return recentFiles;
    }

    private void removePreferencesForFile(String file) {
        SharedPreferences sharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        SharedPreferences.Editor prefEditor = sharedPreferences.edit();
        boolean bPrefEdited = false;
        if (sharedPreferences.contains("lastReadPage:"+file)) {
            bPrefEdited = true;
            prefEditor.remove("lastReadPage:"+file);
        }
        if (bPrefEdited) {
            prefEditor.commit();
        }

    }

    public int getMaxQuizScore() {
        return maxQuizScore;
    }

    public void setMaxQuizScore(int maxQuizScore) {
        this.maxQuizScore = maxQuizScore;
    }

    public int getTextZoom() {
        SharedPreferences spDefault = PreferenceManager.getDefaultSharedPreferences(context);
        String strVal = spDefault.getString("textZoom", "100");
        return Integer.parseInt(strVal);
    }

    private void readPreferences() {
        // read MaxQuizScore
        SharedPreferences spDefault = PreferenceManager.getDefaultSharedPreferences(context);
        String strMaxQuizScore = spDefault.getString("maxQuizScore", "6");
        maxQuizScore = Integer.parseInt(strMaxQuizScore);
        //read recent files
        SharedPreferences appSharedPreferences = context.getSharedPreferences(sharedPropFile, MODE_PRIVATE);
        if (appSharedPreferences.contains("readFile")) {
            this.readFile = appSharedPreferences.getString("readFile", null);
        }
        for (int i = 0; i < maxRecentFiles; i++) {
            if (appSharedPreferences.contains("recentFile"+i)) {
                recentFiles.add(appSharedPreferences.getString("recentFile"+i, ""));
            }
        }
    }

    public List<Inf> getWordList() {
        return wordList;
    }

    public void setWordList(List<Inf> wordList) {
        this.wordList = wordList;
    }

    public String getWordListName() {
        return wordListName;
    }

    public void setWordListName(String wordListName) {
        this.wordListName = wordListName;
    }
}
