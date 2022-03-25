/*
#######################################################################
#
#  Linguisto Portal
#
#  Copyright (c) 2017 Volodymyr Vlad
#
#######################################################################
*/

package com.vlad.linguisto.text;

import android.util.Log;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;

import java.util.*;

public class BuilderPOS {

    private static final String LOG_TAG = BuilderPOS.class.getSimpleName();

    public static String TagSeparator = "¿";
    private static MaxentTagger posTagger = null;

	public BuilderPOS() throws Exception {
        if (posTagger == null) {
            loadTagger();
        }
	}

	private void loadTagger() {
        Date start = new Date();
        Properties config = new Properties();
        config.setProperty("tagSeparator", TagSeparator);
        config.setProperty("tokenizerOptions", "asciiQuotes");
        //MaxentTagger posTagger = new MaxentTagger("/home/vlad/Dokumente/my_dev/pos-tagging/stanford-postagger/models/english-left3words-distsim.tagger", config);
        posTagger = new MaxentTagger("edu/stanford/nlp/models/pos-tagger/english-left3words/english-left3words-distsim.tagger", config);
        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        Log.d(LOG_TAG, "MaxentTagger loaded in "+ runTime + " sec.");
    }

    public MaxentTagger getPosTagger() {
	    return posTagger;
    }

    public String makeTextAsHtml(String strText, Dictionary dict, boolean completeHtmlPage) throws Exception {
        String ret = null;

        //read text in first language
        TextPOS text = makeText(strText, dict);

        //store HTML
        ret = text.getHtml(TextPOS.FIND_WORD_GERMAN);

        return ret;
    }

    public TextPOS makeText(String strText, Dictionary dict) throws Exception {
        TextPOS ret = null;
        Date start = new Date();

        //read text in first language
        ret = new TextPOS(strText, dict, posTagger);
        int textProcessingType = TextPOS.FIND_WORD_GERMAN; //as is, first word lower case
        ret.prepareDict(textProcessingType);

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        Log.d(LOG_TAG, "Builder.makeText() done in "+ runTime + " sec.");
        return ret;
    }

}
