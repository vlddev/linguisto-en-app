package com.vlad.linguisto.tools;

import com.vlad.linguisto.db.DictDbHelper;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;

public class DataFile {

    private final static String LDF_MARK = "#LDF"; //file header, LDF = linguisto data file
    private final static String LDF_KNOWN_INFS = "#LDF known infs";
    private final static String LDF_QUIZ_INFS = "#LDF quiz infs";
    private final static String LDF_END = "#LDF end";

    public static void save(String file, DictDbHelper dbHelper) throws Exception {
        BufferedWriter out = null;
        try {
            out = new BufferedWriter(new FileWriter(file, false));
            out.write(LDF_KNOWN_INFS+"\n");
            out.write(dbHelper.getKnownInfs());
            out.write(LDF_QUIZ_INFS+"\n");
            for(String str : dbHelper.getQuizInfsAsList()) {
                out.write(str+"\n");
            }
            out.write(LDF_END+"\n");
        } finally {
            if (out != null) {
                out.flush();
                out.close();
            }
        }
    }

    public static void load(String file, DictDbHelper dbHelper) throws Exception {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line;
            boolean bFirst = true;
            int mode = -1;
            List<String> knownInfs = new ArrayList<>();
            List<String> quizInfs = new ArrayList<>();
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (bFirst) {
                    bFirst = false;
                    if (!line.startsWith(LDF_MARK)) {
                        throw new Exception("Not a linguisto data file. '"+line+"'");
                    }
                }
                if (line.length() > 0) {
                    if (line.startsWith("#")) {
                        if (line.startsWith(LDF_KNOWN_INFS)) {
                            mode = 1; //"#LDF known infs";
                        } else if (line.startsWith(LDF_QUIZ_INFS)) {
                            mode = 2; //"#LDF quiz infs";
                        }
                    } else {
                        if (mode == 1) {
                            knownInfs.add(line);
                        } else if (mode == 2) {
                            quizInfs.add(line);
                        }
                    }
                }
            }
            dbHelper.setKnownInfs(knownInfs);
            dbHelper.setQuizInfs(quizInfs);
        } finally {
            if (br != null) {
                br.close();
            }
        }
    }
}
