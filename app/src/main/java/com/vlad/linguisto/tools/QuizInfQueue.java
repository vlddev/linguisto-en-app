package com.vlad.linguisto.tools;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class QuizInfQueue {

    private final LinkedList<Inf> wordQueue = new LinkedList<>();
    private final DictDbHelper dbHelper;

    private static final int QUEUE_FILL_SIZE = 40;


    public QuizInfQueue(DictDbHelper dbHelper) {
        this.dbHelper = dbHelper;
        fill();
    }
    /**
     * Retrieves and removes the head of this queue
     */
    public Inf poll() {
        Inf inf = wordQueue.peek();
        if (inf == null) {
            fill();
        }
        inf = wordQueue.poll();
        if (inf == null) {
            throw new IllegalStateException("QuizInfQueue is empty!");
        }
        return inf;
    }

    public List<Inf> poll(int count) {
        List<Inf> ret = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            ret.add(poll());
        }
        return ret;
    }

    public void fill() {
        int recapCount = 20;
        List<Inf> lst = dbHelper.getInfsToRecap(recapCount);
        wordQueue.addAll(lst);
        int learnCount = QUEUE_FILL_SIZE - wordQueue.size();
        wordQueue.addAll(dbHelper.getUnknownNotInQuizInfs(learnCount));
        Collections.shuffle(wordQueue);
    }
}
