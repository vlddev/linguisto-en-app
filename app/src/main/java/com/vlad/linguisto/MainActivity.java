package com.vlad.linguisto;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.obsez.android.lib.filechooser.ChooserDialog;
import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;
import com.vlad.linguisto.tools.DataFile;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

public class MainActivity extends AppCompatActivity {

    public static final String LOG_TAG = MainActivity.class.getSimpleName();

    private DictDbHelper dbHelper;

    Button btnFind;
    AutoCompleteTextView searchText;
    WebView textView;
    private TextToSpeech tts;
    Map<String, String> mapMenuFile = new HashMap();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        maybeReplaceDbFromAssets();
        AppManager.getInstance(getApplicationContext());

        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        dbHelper = AppManager.getDictDbHelper();
        dbHelper.initGlobalData();

        btnFind = (Button)findViewById(R.id.findButton);
        btnFind.setOnClickListener(
            new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    String searchStr = searchText.getText().toString().trim();
                    // Show all data
                    Log.d(LOG_TAG, "show data for "+searchStr);
                    textView.loadDataWithBaseURL("file:///android_asset/", getSearchAsHtml(searchStr), "text/html", "utf-8", null);
                }
            });

        ArrayAdapter var5 = new ArrayAdapter(this, 2131492900, 2131296577, AppManager.allWords);
        searchText = (AutoCompleteTextView)findViewById(R.id.searchText);
        searchText.setThreshold(2);
        this.searchText.setAdapter(var5);
        this.searchText.setOnItemClickListener(new AdapterView.OnItemClickListener(this) {
            final MainActivity this$0;

            {
                this.this$0 = var1;
            }

            public void onItemClick(AdapterView var1, View var2, int var3, long var4) {
                this.this$0.textView.loadDataWithBaseURL("file:///android_asset/", this.this$0.getSearchAsHtml(var1.getAdapter().getItem(var3).toString()), "text/html", "utf-8", (String)null);
            }
        });

        textView = (WebView)findViewById(R.id.textView);
        textView.setWebViewClient(new DictWebViewClient());
    }

    @Override
    public void onDestroy() {
        //dbHelper.close();
        super.onDestroy();
    }

    public void showHelp() {
        textView.loadUrl("file:///android_asset/help.html");
    }

    public String getSearchAsHtml(String searchStr) {
        StringBuffer buffer = new StringBuffer();
        List<Inf> res = dbHelper.findInfs(searchStr);
        if (res.size() == 0) {
            buffer.append("Nothing found for <b>"+searchStr+"</b>");
        } else {
            buffer.append(infListAsHtml(res));
        }
        return buffer.toString();
    }

    public String getInfIdsAsHtml(String csvInfIds) {
        StringBuffer buffer = new StringBuffer();
        List<Inf> res = dbHelper.getInfs(csvInfIds);
        if (res.size() == 0) {
            buffer.append("Nothing found for ids <b>"+csvInfIds+"</b>");
        } else {
            buffer.append(infListAsHtml(res));
        }
        return buffer.toString();
    }

    private String infListAsHtml(List<Inf> infs) {
        StringBuffer buffer = new StringBuffer();
        String lastWord = "";
        for (Inf inf : infs) {
            if (lastWord.length() > 0 && !lastWord.equals(inf.getInf())) {
                // add extended dict article
                String extDict = dbHelper.getExtDictArticle(inf.getInf());
                if (extDict.length() > 0) {
                    buffer.append(extDict+"<hr/>\n");
                }
            }
            lastWord = inf.getInf();
            buffer.append(inf.getHtml()+"\n");
            buffer.append("<table style=\"width:100%\"><tr><td>");
            if (inf.isKnown()) {
                buffer.append(String.format("<a href=\"dict://unknown/%s\">"+getString(R.string.dlg_dict_set_to_unknown)+"</a>", inf.getId()));
            } else {
                buffer.append(String.format("<a href=\"dict://known/%s\">"+getString(R.string.dlg_dict_set_to_known)+"</a>", inf.getId()));
            }
            buffer.append("</td><td>");
            buffer.append("<div style=\"text-align:right\">");
            buffer.append(String.format("<a href=\"dict://quizadd/%s\">"+getString(R.string.menu_add_to_quiz)+"</a>", inf.getId()));
            buffer.append("</div></td></tr></table>");
        }
        // add extended dict article
        String extDict = dbHelper.getExtDictArticle(lastWord);
        if (extDict.length() > 0) {
            buffer.append(extDict+"<hr/>\n");
        }
        return buffer.toString();
    }

    public String getInfByRankAsHtml(int startRank) {
        StringBuffer buffer = new StringBuffer();
        List<Inf> res = dbHelper.getInfsFromRank(startRank, 100);
        if (res.size() == 0) {
            buffer.append("Nothing found for rank <b>"+startRank+"</b>");
        } else {
            for (Inf inf : res) {
                buffer.append(inf.getHeadHtml() + "\n");
            }
        }
        return buffer.toString();
    }

    private void maybeReplaceDbFromAssets() {
        String appDataPath = this.getApplicationInfo().dataDir;
        File dbFolder = new File(appDataPath + "/databases");
        dbFolder.mkdir();
        File dbFilePath = new File(appDataPath + "/databases/" + DictDbHelper.DB_NAME);

        try {
            if (!dbFilePath.exists()) {
                Log.d(LOG_TAG, "Copy database from assets");
                copyDbFromAssets(DictDbHelper.DB_NAME, dbFilePath);
                //set db version
                DictDbHelper dbHelper = new DictDbHelper(getApplicationContext());
                String strDbVersion = dbHelper.getDbVersion();
                if (strDbVersion == null || strDbVersion.length() == 0) {
                    // set default version
                    dbHelper.setDbVersion(getString(R.string.db_version));
                }
                dbHelper.close();
            } else {
                Log.d(LOG_TAG, "Database file "+dbFilePath.getAbsolutePath()+ " already exists.");

                DictDbHelper dbHelper = new DictDbHelper(getApplicationContext());
                String strAssetDbVersion = getString(R.string.db_version);
                String strDbVersion = dbHelper.getDbVersion();
                if (strDbVersion.compareTo(strAssetDbVersion) < 0) {
                    Toast.makeText(this, "Database updated", Toast.LENGTH_SHORT).show();
                    Log.i(LOG_TAG, "Upgrade DB: strAssetDbVersion = "+strAssetDbVersion + "> strDbVersion = "+strDbVersion);
                    List<String> knownInfs = dbHelper.getKnownInfsAsList();
                    Log.d(LOG_TAG, "knownInfs.size = "+knownInfs.size());
                    List<String> quizInfs = dbHelper.getQuizInfsAsList();
                    Log.d(LOG_TAG, "quizInfs.size = "+quizInfs.size());
                    dbHelper.close();

                    //replace old database file
                    if (dbFilePath.delete()) {
                        //copy new db from assets
                        copyDbFromAssets(DictDbHelper.DB_NAME, dbFilePath);

                        // migrate user data from old DB to new DB
                        dbHelper = new DictDbHelper(getApplicationContext());
                        dbHelper.setDbVersion(getString(R.string.db_version));
                        dbHelper.setKnownInfs(knownInfs);
                        dbHelper.setQuizInfs(quizInfs);
                        dbHelper.close();

                    } else {
                        Log.w(LOG_TAG, "File "+dbFilePath.getAbsolutePath()+ " was not deleted.");
                    }
                } else {
                    Log.i(LOG_TAG, "DB-upgrade not needed: strAssetDbVersion = "+strAssetDbVersion + "<= strDbVersion = "+strDbVersion);
                    dbHelper.close();
                }
            }
        } catch (IOException e){
            //handle
            Log.e(LOG_TAG, "Error replacing database from assets: " + e.getMessage());
        }
    }

    private boolean copyDbFromAssets(String assetDbName, File destDbFile) throws IOException {
        boolean ret = true;
        try (InputStream inputStream = this.getAssets().open(assetDbName)) {
            ret = copyFile(inputStream, destDbFile);
        } catch (IOException e) {
            ret = false;
            Log.e(LOG_TAG, "Error copying file.", e);
        }
        if (ret)
            Log.d(LOG_TAG, "Database file copied to "+destDbFile.getAbsolutePath());
        return ret;
    }

    private boolean copyFile(InputStream inputStream, File destFile) throws IOException {
        boolean ret = true;
        try (OutputStream outputStream = new FileOutputStream(destFile)) {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer))>0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.flush();
        } catch (IOException e) {
            ret = false;
            Log.e(LOG_TAG, "Error copying file.", e);
        }
        return ret;
    }

    private void storeKnownWordsToFile() {
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())) {
            new ChooserDialog(MainActivity.this)
                    .withFilter(true, false)
                    .withResources(R.string.title_choose_folder, R.string.btn_ok, R.string.btn_cancel)
                    .withStartFile(Environment.getExternalStorageDirectory().getAbsolutePath())
                    .withChosenListener(new ChooserDialog.Result() {
                        @Override
                        public void onChoosePath(String path, File pathFile) {
                            File outFile = new File(path, AppManager.knownWordsFile);
                            try {
                                DataFile.save(outFile.getAbsolutePath(), dbHelper);
                                Log.d(LOG_TAG, "File stored to '"+outFile.getAbsolutePath()+"'");
                                Toast.makeText(MainActivity.this, "File stored", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Log.e(LOG_TAG, "Error writing file.", e);
                                try {
                                    outFile = new File(getExternalFilesDir(null), AppManager.knownWordsFile);
                                    DataFile.save(outFile.getAbsolutePath(), dbHelper);
                                    Log.d(LOG_TAG, "File stored to '"+outFile.getAbsolutePath()+"'");
                                    Toast.makeText(MainActivity.this, "WARNING: File stored to '"+outFile.getAbsolutePath()+"'\n"
                                            + "Storing to '"+path+"' impossible: "+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                } catch (Exception ex) {
                                    Log.e(LOG_TAG, "Error writing file.", ex);
                                    Toast.makeText(MainActivity.this, "ERROR: File was not stored\n"+ex.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                                }
                            }
                        }
                    })
                    .enableOptions(true)
                    .build()
                    .show();
        } else {
            Toast.makeText(this, "ERROR: File was not stored. \nSD Card has wrong state: "+Environment.getExternalStorageState(),
                    Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, "SD Card has wrong state: "+Environment.getExternalStorageState());
        }
    }

    private void restoreKnownWordsFromFile() {
        new ChooserDialog(MainActivity.this)
                .withStartFile(Environment.getExternalStorageDirectory().getPath())
                .withResources(R.string.title_choose_file, R.string.btn_ok, R.string.btn_cancel)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        try {
                            DataFile.load(path, dbHelper);
                            Toast.makeText(MainActivity.this, "Known words restored", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error restoring known words.", e);
                            Toast.makeText(MainActivity.this, "ERROR: Known words were not restored.\n"+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                // to handle the back key pressed or clicked outside the dialog:
                .withOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Log.d("CANCEL", "CANCEL");
                        dialog.cancel(); // MUST have
                    }
                })
                .build().show();
    }

    private void showWordsFromRank() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_rank_list));
        builder.setMessage(getString(R.string.dlg_rank_list_msg));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int rank = Integer.parseInt(input.getText().toString());
                textView.loadDataWithBaseURL(null, getInfByRankAsHtml(rank), "text/html", "utf-8", null);
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

    private void markWordsAsKnown() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.menu_mark_learned));
        builder.setMessage(getString(R.string.dlg_mark_learned_msg));
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_NORMAL);
        builder.setView(input);
        builder.setPositiveButton(getString(R.string.btn_ok), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int rank = Integer.parseInt(input.getText().toString());
                dbHelper.setKnownInfsWithRankLess(rank);
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

    private void checkForUpdate() {
        try {
            new CheckForUpdateTask().execute(new URL("https://github.com/vlddev/linguisto-dicts/raw/master/en-uk-dict-version"));
        } catch (MalformedURLException e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
    }

    private void updateDbFromFile() {
        new ChooserDialog(MainActivity.this)
                .withStartFile(Environment.getExternalStorageDirectory().getPath())
                .withResources(R.string.title_choose_file, R.string.btn_ok, R.string.btn_cancel)
                .withChosenListener(new ChooserDialog.Result() {
                    @Override
                    public void onChoosePath(String path, File pathFile) {
                        try {
                            new UpdateDictFromFileTask().execute(new File(path));
                            Toast.makeText(MainActivity.this, "DB updated", Toast.LENGTH_LONG).show();
                        } catch (Exception e) {
                            Log.e(LOG_TAG, "Error updating DB from file.", e);
                            Toast.makeText(MainActivity.this, "ERROR: can't update DB from file.\n"+e.getLocalizedMessage(), Toast.LENGTH_LONG).show();
                        }
                    }
                })
                // to handle the back key pressed or clicked outside the dialog:
                .withOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        Log.d("CANCEL", "CANCEL");
                        dialog.cancel(); // MUST have
                    }
                })
                .build().show();
    }

    private String readFileFromURL(URL url) {
        String ret = null;
        try (BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()))) {
            StringBuilder sb = new StringBuilder();
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                sb.append(inputLine);
            ret = sb.toString();
        } catch(Exception e) {
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return ret;
    }

    private boolean copyFileFromURL(URL url, File dest) {
        boolean ret = true;
        try (InputStream in = url.openStream()) {
            ret = copyFile (in, dest);
        } catch(Exception e) {
            ret = false;
            Log.e(LOG_TAG, e.getMessage(), e);
        }
        return ret;
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);

        // Find the menuItem to add your SubMenu
        MenuItem submenuRecentFiles = menu.findItem(R.id.submenu_choose_text_file);
        buildMenuResentFiles(submenuRecentFiles);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem submenuRecentFiles = menu.findItem(R.id.submenu_choose_text_file);
        int i = 0;
        do  {
            MenuItem mi = submenuRecentFiles.getSubMenu().getItem(i);
            if (mi.getTitle().toString().startsWith("*")) {
                submenuRecentFiles.getSubMenu().removeItem(mi.getItemId());
            } else {
                i++;
            }
        } while ( i < submenuRecentFiles.getSubMenu().size());

        buildMenuResentFiles(submenuRecentFiles);

        return true;
    }

    private void buildMenuResentFiles(MenuItem menu) {
        mapMenuFile.clear();
        List<String> recentFiles = AppManager.getInstance(getApplicationContext()).getRecentFiles();
        int cnt = 1;
        for (String str : recentFiles) {
            File fFile = new File(str);
            String menuStr = "*"+cnt+" "+ StringUtils.abbreviateMiddle(fFile.getName(), "..", 30);
            mapMenuFile.put(menuStr, str);
            MenuItem mi = menu.getSubMenu().add(menuStr);
            cnt++;
        }
    }

    public void showLearnStatsDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.web_view_dialog, null);
        WebView webView = view.findViewById(R.id.dialogWebView);
        webView.loadDataWithBaseURL(null, dbHelper.getLearnStats(), "text/html", "utf-8", null);
        alert.setView(view);
        alert.show();
    }

    public void showAboutDialog() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.web_view_dialog, null);
        WebView webView = view.findViewById(R.id.dialogWebView);
        StringBuffer sbContent = new StringBuffer();
        sbContent.append("<html><body>");
        sbContent.append("<h3>Про Лінґвісто</h3>");
        sbContent.append("<h4>Версія: "+this.getString(R.string.versionName)+"</h4>");
        sbContent.append("<h4>версія словника: "+dbHelper.getDbVersion()+"</h4>");
        sbContent.append("<h4>© 2020 Володимир Влад</h4>");
        sbContent.append("<h4>Веб-сторінка: <a href=\"https://www.facebook.com/linguisto.eu/\">www.facebook.com/linguisto.eu</a></h4>");
        sbContent.append("</body></html>");

        webView.loadDataWithBaseURL(null, sbContent.toString(), "text/html", "utf-8", null);
        alert.setView(view);
        alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        AppManager appManager = AppManager.getInstance(getApplicationContext());

        if (id == R.id.action_store_learned) {
            storeKnownWordsToFile();
        } else if (id == R.id.action_restore_learned) {
            restoreKnownWordsFromFile();
        } else if (id == R.id.action_mark_learned) {
            markWordsAsKnown();
        } else if (id == R.id.action_learn_list) {
            appManager.setWordList(AppManager.getDictDbHelper().getInfsToLearn(40));
            appManager.setWordListName(getString(R.string.title_activity_learn_list));
            startActivity(new Intent(this, WordListActivity.class));
        } else if (id == R.id.action_recap_list) {
            appManager.setWordList(AppManager.getDictDbHelper().getInfsToRecap(50));
            appManager.setWordListName(getString(R.string.title_activity_recap_list));
            startActivity(new Intent(this, WordListActivity.class));
        } else if (id == R.id.action_quiz) {
            startActivity(new Intent(this, QuizActivity.class));
        } else if (id == R.id.action_restore_learned) {
            restoreKnownWordsFromFile();
        } else if (id == R.id.action_learn_stats) {
            showLearnStatsDialog();
        } else if (id == R.id.action_rank_list) {
            showWordsFromRank();
        } else if (id == R.id.action_check_update) {
            checkForUpdate();
        } else if (id == R.id.action_update_db_from_file) {
            updateDbFromFile();
        } else if (id == R.id.action_about) {
            showAboutDialog();
        } else if (id == R.id.action_help) {
            showHelp();
        } else if (id == R.id.action_choose_text_file) {
            new ChooserDialog(MainActivity.this)
                    //.withStartFile(path)
                    .withChosenListener(new ChooserDialog.Result() {
                        @Override
                        public void onChoosePath(String path, File pathFile) {
                            AppManager.getInstance(getApplicationContext()).setReadFile(path);
                            Toast.makeText(MainActivity.this, "FILE: " + path, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(MainActivity.this, ReadActivity.class));
                        }
                    })
                    // to handle the back key pressed or clicked outside the dialog:
                    .withOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            Log.d("CANCEL", "CANCEL");
                            dialog.cancel(); // MUST have
                        }
                    })
                    .build()
                    .show();
        } else if (id == R.id.action_settings) {
            startActivity(new Intent(this, SettingsActivity.class));
        }

        if (item.getTitle().toString().startsWith("*")) {
            appManager.setReadFile(mapMenuFile.get(item.getTitle()));
            startActivity(new Intent(this, ReadActivity.class));
        }

        return super.onOptionsItemSelected(item);
    }

    private class DictWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
            String url = request.getUrl().toString();
            Log.d(LOG_TAG, "DictWebViewClient: Load url: " + url);
            if (url.startsWith("dict://")) {
                if (url.startsWith("dict://unknown/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://unknown/", ""));
                    dbHelper.setInfKnown(infId, false);
                    view.loadDataWithBaseURL(null, getSearchAsHtml(searchText.getText().toString().trim()), "text/html", "utf-8", null);
                } else if (url.startsWith("dict://known/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://known/", ""));
                    dbHelper.setInfKnown(infId, true);
                    view.loadDataWithBaseURL(null, getSearchAsHtml(searchText.getText().toString().trim()), "text/html", "utf-8", null);
                } else if (url.startsWith("dict://rank/")) {
                    Integer rank = Integer.parseInt(url.replace("dict://rank/", ""));
                    List<Inf> lst = dbHelper.getInfsFromRank(rank, 1);
                    view.loadDataWithBaseURL(null, infListAsHtml(lst), "text/html", "utf-8", null);
                    if ( lst.size() > 0 ) {
                        searchText.setText(lst.get(0).getInf());
                    }
                } else if (url.startsWith("dict://quizadd/")) {
                    Integer infId = Integer.parseInt(url.replace("dict://quizadd/", ""));
                    if( dbHelper.addQuizInf(infId) ) {
                        Toast.makeText(getApplicationContext(), "Word added to quiz", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getApplicationContext(), "Word is already in quiz", Toast.LENGTH_LONG).show();
                    }
                } else {
                    view.loadDataWithBaseURL(null, getSearchAsHtml(url.replace("dict://", "")), "text/html", "utf-8", null);
                }
            } else {
                view.loadDataWithBaseURL(null, "Can't process URL: "+url, "text/html", "utf-8", null);
            }
            return true;
        }

        @Override
        public void onPageFinished(WebView view, String url) {
            super.onPageFinished(view, url);
            view.scrollTo(0,0);
        }

    }

    private class CheckForUpdateTask extends AsyncTask<URL, Integer, String> {
        AlertDialog alertDialog;
        protected void onPreExecute() {
            super.onPreExecute();
            alertDialog = new AlertDialog.Builder(MainActivity.this).create();
        }

        protected String doInBackground(URL... urls) {
            String repoDbVersion = readFileFromURL(urls[0]);
            return repoDbVersion;
        }

        protected void onPostExecute(String repoDbVersion) {
            if (repoDbVersion != null) {
                Log.d(LOG_TAG, "repoDbVersion: "+repoDbVersion);
                String strDbVersion = dbHelper.getDbVersion();
                if (strDbVersion.compareTo(repoDbVersion) < 0) {
                    Log.i(LOG_TAG, "DB update is available: repoDbVersion = "+repoDbVersion + "> strDbVersion = "+strDbVersion);
                    alertDialog.setTitle("Update");
                    alertDialog.setMessage("New dictionary version "+repoDbVersion+" is available."+
                            "\nCurrent dictionary version is "+strDbVersion+"."+
                            "\nWould you like to update your dictionary?");
                    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE ,"Yes" , new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    try {
                                        new UpdateDictTask().execute(new URL("https://github.com/vlddev/linguisto-dicts/raw/master/linguisto-en-uk.db"));
                                    } catch (MalformedURLException e) {
                                        Log.e(LOG_TAG, e.getMessage(), e);
                                    }
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE , "No", new DialogInterface.OnClickListener() {

                                public void onClick(DialogInterface dialog, int which) {
                                    // Do nothing but close the dialog
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                } else {
                    Toast.makeText(getApplicationContext(), "Your dictionary DB is up to date.", Toast.LENGTH_LONG).show();
                }
            } else {
                alertDialog.setTitle("Update error");
                alertDialog.setMessage("Check your internet connection.");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Do nothing but close the dialog
                        dialog.dismiss();
                    }
                });
                alertDialog.show();
            }
        }
    }

    private class UpdateDictTask extends AsyncTask<URL, Integer, Boolean> {
        protected Boolean doInBackground(URL... urls) {
            boolean bOk = true;
            try {
                File outputDir = getApplicationContext().getCacheDir();
                File tmpDictFile = null;
                tmpDictFile = File.createTempFile("dict", ".db", outputDir);
                // download new dictionary
                Log.d(LOG_TAG, "UpdateDictTask: downloading update");
                bOk = copyFileFromURL(urls[0], tmpDictFile);
                Log.d(LOG_TAG, "UpdateDictTask: update downloaded");
                if (bOk) {
                    //replace database
                    List<String> knownInfs = dbHelper.getKnownInfsAsList();
                    Log.d(LOG_TAG, "UpdateDictTask: knownInfs.size = "+knownInfs.size());
                    List<String> quizInfs = dbHelper.getQuizInfsAsList();
                    Log.d(LOG_TAG, "UpdateDictTask: quizInfs.size = "+quizInfs.size());
                    dbHelper.close();

                    //replace old database file
                    File dbFilePath = new File(getApplicationInfo().dataDir + "/databases/" + DictDbHelper.DB_NAME);
                    if (dbFilePath.delete()) {
                        //copy new db from temp file
                        try (InputStream in = new FileInputStream(tmpDictFile)) {
                            bOk = copyFile(in, dbFilePath);
                            Log.d(LOG_TAG, "UpdateDictTask: dictionary replaced");
                        }
                        // migrate user data from old DB to new DB
                        dbHelper = new DictDbHelper(getApplicationContext());
                        dbHelper.setKnownInfs(knownInfs);
                        dbHelper.setQuizInfs(quizInfs);
                    } else {
                        Log.w(LOG_TAG, "UpdateDictTask: File "+dbFilePath.getAbsolutePath()+ " was not deleted.");
                    }
                    //TODO trigger restart app
                }
                tmpDictFile.delete();
            } catch (IOException e) {
                bOk = false;
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return bOk;
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(LOG_TAG, "UpdateDictTask: successfully updated");
                Toast.makeText(getApplicationContext(), "Your dictionary DB is up to date.", Toast.LENGTH_LONG).show();
            } else {
                Log.w(LOG_TAG, "UpdateDictTask: Update error");
                Toast.makeText(getApplicationContext(), "Update error.", Toast.LENGTH_LONG).show();
            }
        }
    }

    private class UpdateDictFromFileTask extends AsyncTask<File, Integer, Boolean> {
        protected Boolean doInBackground(File... files) {
            boolean bOk = true;
            try {
                File dictFile = files[0];
                //replace database
                List<String> knownInfs = dbHelper.getKnownInfsAsList();
                Log.d(LOG_TAG, "UpdateDictTask: knownInfs.size = "+knownInfs.size());
                List<String> quizInfs = dbHelper.getQuizInfsAsList();
                Log.d(LOG_TAG, "UpdateDictTask: quizInfs.size = "+quizInfs.size());
                dbHelper.close();

                //replace old database file
                File dbFilePath = new File(getApplicationInfo().dataDir + "/databases/" + DictDbHelper.DB_NAME);
                if (dbFilePath.delete()) {
                    //copy new db from temp file
                    try (InputStream in = new FileInputStream(dictFile)) {
                        bOk = copyFile(in, dbFilePath);
                        Log.d(LOG_TAG, "UpdateDictTask: dictionary replaced");
                    }
                    // migrate user data from old DB to new DB
                    dbHelper = new DictDbHelper(getApplicationContext());
                    dbHelper.setKnownInfs(knownInfs);
                    dbHelper.setQuizInfs(quizInfs);
                    //TODO trigger restart app
                }
            } catch (IOException e) {
                bOk = false;
                Log.e(LOG_TAG, e.getMessage(), e);
            }
            return bOk;
        }

        protected void onPostExecute(Boolean success) {
            if (success) {
                Log.d(LOG_TAG, "UpdateDictTask: successfully updated");
                Toast.makeText(getApplicationContext(), "Your dictionary DB is up to date.", Toast.LENGTH_LONG).show();
            } else {
                Log.w(LOG_TAG, "UpdateDictTask: Update error");
                Toast.makeText(getApplicationContext(), "Update error.", Toast.LENGTH_LONG).show();
            }
        }
    }
}
