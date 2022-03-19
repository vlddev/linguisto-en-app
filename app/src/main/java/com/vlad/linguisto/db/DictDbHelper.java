package com.vlad.linguisto.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.vlad.linguisto.R;
import com.vlad.linguisto.db.obj.Inf;
import com.vlad.linguisto.db.obj.Translation;
import com.vlad.linguisto.db.obj.WordType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class DictDbHelper extends SQLiteOpenHelper {

    private static final String LOG_TAG = DictDbHelper.class.getSimpleName();

    private static String APP_DATA_PATH = "";
    public static final String DB_NAME = "linguisto.db";
    public static final int DB_VERSION = 1;
    private final Context context;
    ReaderDAO dbReader = new ReaderDAO();
    private final SQLiteDatabase db;


    public static final String SQL_GET_WORDTYPE =
            "SELECT id, descr, comment FROM word_type" +
                    " ORDER BY id ";

    public static final String SQL_FIND_INF =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE lower(inf.inf) like lower(?) " +
                    " ORDER by inf.rank " +
                    " LIMIT ? ";

    public static final String SQL_GET_INF_FROM_RANK =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE inf.rank >= ? " +
                    " ORDER by inf.rank " +
                    " LIMIT ? ";

    public static final String SQL_GET_INF_BY_ID =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE inf.id in ({:ids}) ORDER by inf.rank";

    public static final String SQL_FIND_UNKNOWN_INF =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.inf_id is null AND inf.type > 0 AND inf.type != 20 " +
                    " ORDER by inf.rank LIMIT ?";

    public static final String SQL_FIND_UNKNOWN_INF_BY_RANK =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.inf_id is null AND inf.type > 0 AND inf.type != 20 " +
                    "    AND inf.rank < ?" +
                    " ORDER by inf.rank";

    public static final String SQL_FIND_RECENTLY_LEARNED_INF_BY_RANK =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.learn_date > date('now', '-"+Inf.recentlyLearnedDays+" day') " +
                    "    AND inf.rank < ?" +
                    " ORDER by inf.rank";

    public static final String SQL_GET_RECAP_INF =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.learn_date < date('now', '-"+Inf.recapAfterDays+" day')  AND inf.type > 0 AND inf.type != 20" +
                    "     AND k.recap_date IS NULL  " +
                    "     AND inf.id NOT IN (SELECT inf_id FROM quiz_inf qi)" +
                    " ORDER by k.learn_date desc LIMIT ?";

    public static final String SQL_GET_RECAP_INF_STEP2 =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date FROM inf" +
                    " JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.learn_date > date('now', '-"+Inf.recapAfterDays*3+" day')  AND inf.type > 0 AND inf.type != 20" +
                    "     AND k.recap_date IS NOT NULL  " +
                    "     AND inf.id NOT IN (SELECT inf_id FROM quiz_inf qi)" +
                    " ORDER by k.learn_date LIMIT ?";

    public static final String SQL_GET_UNKNOWN_NOT_IN_QUIZ_INF =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date " +
                    " FROM inf " +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE k.inf_id is null AND inf.type > 0 AND inf.type != 20" +
                    "     AND inf.id NOT IN (SELECT inf_id FROM quiz_inf qi)" +
                    " ORDER by inf.rank LIMIT ? ";

    public static final String SQL_GET_QUIZ_INF =
            "SELECT id, inf, type, transcription, rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date, qi.score quiz_score " +
                    " FROM inf, quiz_inf qi " +
                    " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                    " WHERE inf.id = qi.inf_id ";

    public static final String SQL_GET_RANDOM_INF =
            "SELECT id, inf, type, transcription, rank, -1 known, -1 learn_date FROM inf" +
                    " WHERE inf.type = ? AND inf.rank < 10000 AND inf.type > 0 AND inf.type != 20" +
                    " ORDER by RANDOM() LIMIT ? ";

    public static final String SQL_GET_TR =
            "SELECT id, order_nr, translation, example FROM tr" +
                    " WHERE fk_inf = ? ORDER BY order_nr ";

    public static final String SQL_GET_EXT_ARTICLE =
            "SELECT cnt FROM extdict WHERE word = ?";

    public static final String SQL_GET_LEARN_STATS_OLD =
        "select learn_date, count(inf_id) " +
        " FROM known_inf " +
        " group by learn_date" +
        " order by learn_date desc" +
        " LIMIT 30";

    public static final String SQL_GET_TOTAL_LEARN =
        "select count(inf_id), sum(case ifnull(recap_date, 0) when 0 then 0 else 1 end) rsum" +
        " from known_inf ";

    public static final String SQL_GET_LEARN_STATS =
        "select date1, sum(cnt1), sum(cnt2) " +
        "from ( " +
        "select learn_date date1, count(inf_id) cnt1, 0 cnt2 " +
        "   FROM known_inf" +
        "   where learn_date is not null" +
        "   group by learn_date" +
        " union " +
        "select recap_date date1, 0 cnt1, count(inf_id) cnt2 " +
        "   FROM known_inf" +
        "   where recap_date is not null" +
        "   group by recap_date " +
        " ) tbl " +
        "group by date1 " +
        "order by 1 desc " +
        "LIMIT 30";

    public DictDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
        APP_DATA_PATH = context.getApplicationInfo().dataDir;
        this.context = context;
        this.db = getWritableDatabase();
        Log.d(LOG_TAG, "DbHelper APP_DATA_PATH : " + APP_DATA_PATH);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        //throw new IllegalStateException("Databese not exist!");
    }

    public void initGlobalData() {
        List<WordType> wordTypes = getWordTypes();
        for(WordType obj : wordTypes) {
            Inf.wordTypeMap.put(obj.getId(), obj.getDesc());
        }
    }

    public String getDbVersion() {
        String ret = "";
        try {
            Cursor cursor = db.rawQuery("select val from sysinfo where key = 'db_version'", null);
            if(cursor.getCount() > 0) {
                if (cursor.moveToNext()) {
                    ret = cursor.getString(0);
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getQuizInfs: " + e.getMessage());
        }
        return ret;
    }

    public void setDbVersion(String version) {
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM sysinfo WHERE key = 'db_version'");
            db.execSQL("INSERT INTO sysinfo(key, val) VALUES (?, ?)", new String[] { "db_version", version });
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setDbVersion: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public List<Inf> findInfs(String pattern) {
        return findInfs(pattern, 100);
    }

    public List<Inf> findInfs(String pattern,  int maxResults) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_FIND_INF, new String[] { pattern, ""+maxResults });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in findWords: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getInfsFromRank(int startRank, int maxResults) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_INF_FROM_RANK, new String[] { ""+startRank, ""+maxResults });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getInfsFromRank: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getInfs(String infIds) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_INF_BY_ID.replace("{:ids}", infIds ), null);
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getInfs: " + e.getMessage());
        }
        return ret;
    }

    public String getExtDictArticle(String word) {
        String ret = "";
        try (Cursor cursor = db.rawQuery(SQL_GET_EXT_ARTICLE, new String[] { word })) {
            if(cursor.moveToNext()) {
                ret = cursor.getString(0);
            }
            if (ret.length() > 0) {
                ret = "<html>\n" +
                        "<head>\n" +
                        "<style>\n" +
                        "blockquote {\n" +
                        "   margin-left:10px;\n" +
                        "   width: 100%;\n" +
                        "}\n" +
                        "</style>\n" +
                        "</head>\n" +
                        "<body>\n"+
                        ret+
                        "</body>\n" +
                        "</html>";
            }
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error in getExtDictArticle: " + e.getMessage(), e);
        }
        return ret;
    }

    public List<Inf> getBaseForm(String wf, boolean ignoreCase) {
        String sql = "SELECT distinct inf.id, inf.inf, inf.type, inf.transcription, inf.rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date " +
                " FROM wf, inf" +
                " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                " where wf.wf = ? and wf.fk_inf = inf.id";
        if (ignoreCase) {
            sql = "SELECT distinct inf.id, inf.inf, inf.type, inf.transcription, inf.rank, COALESCE(k.inf_id, -1) known, COALESCE(k.learn_date, -1) learn_date " +
                " FROM wf, inf" +
                " LEFT JOIN known_inf k ON k.inf_id = inf.id" +
                " where lower(wf.wf) = lower(?) and wf.fk_inf = inf.id";
        }
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(sql, new String[] { wf });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getBaseForm: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getInfsToLearn(int count) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_FIND_UNKNOWN_INF, new String[] { ""+count });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getInfsToLearn: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getInfsToRecap(int count) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_RECAP_INF, new String[] { ""+count });
            if (cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
            if (ret.size() < 1) {
                cursor = db.rawQuery(SQL_GET_RECAP_INF_STEP2, new String[] { ""+count });
                if (cursor.getCount() > 0) {
                    ret = getInfs(cursor);
                }
                cursor.close();
            }
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getInfsToLearn: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getUnknownNotInQuizInfs(int count) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_UNKNOWN_NOT_IN_QUIZ_INF, new String[] { ""+count });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getUnknownNotInQuizInfs: " + e.getMessage());
        }
        return ret;
    }

    public String getLearnStats() {
        StringBuffer sbRet = new StringBuffer();
        try {
            sbRet.append("<html>\n" +
                    "<head>\n" +
                    "<style>\n" +
                    "table {\n" +
                    "  font-family: arial, sans-serif;\n" +
                    "  border-collapse: collapse;\n" +
                    "  width: 100%;\n" +
                    "}\n" +
                    "\n" +
                    "td, th {\n" +
                    "  border: 1px solid #dddddd;\n" +
                    "  text-align: left;\n" +
                    "  padding: 8px;\n" +
                    "}\n" +
                    "\n" +
                    "tr:nth-child(even) {\n" +
                    "  background-color: #dddddd;\n" +
                    "}\n" +
                    "</style>\n" +
                    "</head>\n" +
                    "<body>");
            sbRet.append("<table>\n" +
                    "  <tr>\n" +
                    "    <th>"+context.getString(R.string.dlg_learn_stats_date)+"</th>\n" +
                    "    <th>"+context.getString(R.string.dlg_learn_stats_word_count)+"</th>\n" +
                    "    <th>"+context.getString(R.string.dlg_learn_stats_recap_count)+"</th>\n" +
                    "  </tr>");
            Cursor cursor = db.rawQuery(SQL_GET_TOTAL_LEARN, null);
            if(cursor.getCount() > 0) {
                cursor.moveToNext();
                sbRet.append("<tr><td>").append(context.getString(R.string.dlg_learn_stats_total)).
                    append("</td><td>").append(cursor.getString(0)).
                    append("</td><td>").append(cursor.getString(1)).
                    append("</td></tr>");
            }
            cursor.close();
            cursor = db.rawQuery(SQL_GET_LEARN_STATS, null);
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    sbRet.append("<tr><td>").append(cursor.getString(0)).
                        append("</td><td>").append(cursor.getString(1)).
                        append("</td><td>").append(cursor.getString(2)).
                        append("</td></tr>");
                }
            }
            sbRet.append("</table></body>");
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getLearnStats: " + e.getMessage());
        }
        return sbRet.toString();
    }

    public List<Inf> getQuizInfs() {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_QUIZ_INF, null);
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getQuizInfs: " + e.getMessage());
        }
        return ret;
    }

    public List<Inf> getInfsRandom(int type, int count) {
        List<Inf> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_RANDOM_INF, new String[] { ""+type, ""+count });
            if(cursor.getCount() > 0) {
                ret = getInfs(cursor);
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getInfsRandom: " + e.getMessage());
        }
        return ret;
    }

    private List<Inf> getInfs(Cursor cursor) {
        List<Inf> ret = new ArrayList<>();
        Inf curInf = null;
        while (cursor.moveToNext()) {
            curInf = dbReader.readInf(cursor);
            curInf.setTrList(getTrList(curInf.getId()));
            ret.add(curInf);
        }
        return ret;
    }

    public List<Translation> getTrList(Integer infId) {
        List<Translation> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_TR, new String[] { infId.toString() });
            if(cursor.getCount() > 0) {
                Translation tr = null;
                while (cursor.moveToNext()) {
                    tr = dbReader.readTranslation(cursor);
                    ret.add(tr);
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getTrList: " + e.getMessage());
        }

        return ret;
    }

    public List<WordType> getWordTypes() {
        List<WordType> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery(SQL_GET_WORDTYPE, null);
            if(cursor.getCount() > 0) {
                WordType obj = null;
                while (cursor.moveToNext()) {
                    obj = dbReader.readWordType(cursor);
                    ret.add(obj);
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getTrList: " + e.getMessage());
        }

        return ret;
    }

    public String getKnownInfs() {
        StringBuffer sb = new StringBuffer();
        try {
            Cursor cursor = db.rawQuery("SELECT inf_id, learn_date, COALESCE(recap_date, '') FROM known_inf ORDER BY inf_id", null);
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    sb.append(cursor.getString(0)).append("\t").
                            append(cursor.getString(1)).append("\t").
                            append(cursor.getString(2)).append("\n");
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getKnownInfs: " + e.getMessage());
        }
        return sb.toString();
    }

    public List<String> getKnownInfsAsList() {
        List<String> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery("SELECT inf_id, learn_date, COALESCE(recap_date, '') FROM known_inf ORDER BY inf_id", null);
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    ret.add(cursor.getString(0)
                            +"\t"+cursor.getString(1)
                            +"\t"+cursor.getString(2));
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getKnownInfs: " + e.getMessage());
        }
        return ret;
    }

    public void setKnownInfsWithRankLess(int rank) {
        db.beginTransaction();
        Cursor cursor = null;
        try {
            cursor = db.rawQuery(SQL_FIND_RECENTLY_LEARNED_INF_BY_RANK, new String[] { ""+rank });
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    db.execSQL("UPDATE known_inf SET learn_date = date('now', '-"+(Inf.recentlyLearnedDays+1)+" day') WHERE inf_id = ? ",
                            new String[] { ""+cursor.getInt(0) }); //infId
                }
            }
            cursor.close();

            cursor = db.rawQuery(SQL_FIND_UNKNOWN_INF_BY_RANK, new String[] { ""+rank });
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    db.execSQL("INSERT INTO known_inf(inf_id, learn_date) VALUES (?, date('now', '-"+(Inf.recentlyLearnedDays+1)+" day')) ",
                            new String[] { ""+cursor.getInt(0) }); //infId
                }
            }
            cursor.close();
            db.setTransactionSuccessful();

        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setKnownInfsWithRankLess: " + e.getMessage(), e);
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
            db.endTransaction();
        }
    }

    public void setKnownInfs(List<String> rows) {
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM known_inf");
            for (String row : rows) {
                String[] fields = row.split("\t");
                if (fields.length > 2 && fields[2].length() > 0) {
                    db.execSQL("INSERT INTO known_inf(inf_id, learn_date, recap_date) VALUES (?, ?, ?)", new String[] { fields[0], fields[1], fields[2] });
                } else {
                    db.execSQL("INSERT INTO known_inf(inf_id, learn_date) VALUES (?, ?)", new String[] { fields[0], fields[1] });
                }
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setKnownInfs: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public void setInfKnown(Integer infId, boolean known) {
        db.beginTransaction();
        try {
            if (known) {
                db.execSQL("INSERT INTO known_inf(inf_id) VALUES (?)", new String[] { infId.toString() });
            } else {
                db.execSQL("DELETE FROM known_inf WHERE inf_id = ?", new String[] { infId.toString() });
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setInfKnown: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public void setInfRecapDate(Integer infId) {
        db.beginTransaction();
        try {
            db.execSQL("UPDATE known_inf SET recap_date = CURRENT_DATE WHERE inf_id = ?", new String[] { infId.toString() });
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setInfRecapDate: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public boolean setInfRecapDate(Map<String, String> data) {
        boolean ret = true;
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            for (Map.Entry<String, String> item : data.entrySet()) {
                db.execSQL("UPDATE known_inf SET recap_date = ? WHERE inf_id = ?", new String[]{item.getValue(), item.getKey()});
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            ret = false;
            Log.e(LOG_TAG, "Error in setInfRecapDate: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
        return ret;
    }

    public boolean addQuizInf(Integer infId) {
        boolean ret = true;
        db.beginTransaction();
        try {
            db.execSQL("INSERT INTO quiz_inf(inf_id) VALUES (?)", new String[] { infId.toString() });
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in addQuizInf [infId = "+infId+"]: " + e.getMessage(), e);
            ret = false;
        } finally {
            db.endTransaction();
        }
        return ret;
    }

    public void updateQuizInf(Integer infId, int score) {
        db.beginTransaction();
        try {
            db.execSQL("UPDATE quiz_inf SET score = ? WHERE inf_id = ?", new String[] { ""+score, infId.toString() });
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in updateQuizInf: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public void removeQuizInf(Integer infId) {
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM quiz_inf WHERE inf_id = ?", new String[] { infId.toString() });
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in removeQuizInf: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    public List<String> getQuizInfsAsList() {
        List<String> ret = new ArrayList<>();
        try {
            Cursor cursor = db.rawQuery("SELECT inf_id, score FROM quiz_inf ORDER BY inf_id", null);
            if(cursor.getCount() > 0) {
                while (cursor.moveToNext()) {
                    ret.add(cursor.getString(0)
                            +"\t"+cursor.getString(1));
                }
            }
            cursor.close();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in getKnownInfs: " + e.getMessage());
        }
        return ret;
    }

    public void setQuizInfs(List<String> rows) {
        db.beginTransaction();
        try {
            db.execSQL("DELETE FROM quiz_inf");
            for (String row : rows) {
                String[] fields = row.split("\t");
                db.execSQL("INSERT INTO quiz_inf(inf_id, score) VALUES (?, ?)", new String[] { fields[0], fields[1] });
            }
            db.setTransactionSuccessful();
        } catch (SQLiteException e) {
            Log.e(LOG_TAG, "Error in setQuizInfs: " + e.getMessage(), e);
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Log.d(LOG_TAG, "DbHelper.onUpgrade: oldVersion=" + oldVersion + ", newVersion="+ newVersion);
    }

}
