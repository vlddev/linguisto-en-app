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

import java.util.*;

public class Builder {

    private static final String LOG_TAG = Builder.class.getSimpleName();

    public static String TagSeparator = "¿";

	public Builder() {
        //loadTagger();
	}

//	private void loadTagger() {
//        Date start = new Date();
//        Properties config = new Properties();
//        config.setProperty("tagSeparator", TagSeparator);
//        config.setProperty("tokenizerOptions", "asciiQuotes");
//        long runTime = ((new Date()).getTime() - start.getTime())/1000;
//    }

    public String makeTextAsHtml(String strText, Dictionary dict, boolean completeHtmlPage)  {
        //read text in first language
        Text text = makeText(strText, dict);
        return text.getHtml(Text.FIND_WORD_GERMAN);
    }

    public Text makeText(String strText, Dictionary dict) {
        Date start = new Date();

        //read text in first language
        Text ret = new Text(strText, dict);
        int textProcessingType = Text.FIND_WORD_GERMAN; //as is, first word lower case

        ret.prepareDict(textProcessingType);

        long runTime = ((new Date()).getTime() - start.getTime())/1000;
        Log.d(LOG_TAG, "Builder.makeText() done in "+ runTime + " sec.");
        return ret;
    }

}
