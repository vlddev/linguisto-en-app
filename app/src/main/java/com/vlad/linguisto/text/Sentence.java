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

public class Sentence {

    //private int startPosInText = 0;
    private int id = 0;
    private boolean newParagraph = false;
    private String content;
    //private String contentLowerCase;
    //private Locale lang;
    private List<String> elemList = new ArrayList<>();
    //private List<String> elemListLowerCase = new ArrayList<String>();
    private List<String> dividers = new ArrayList<>();

    public Sentence(String text) {
        content = text;
        // parse sentence
        init();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    private void init() {
        StringTokenizer fIn = new StringTokenizer(content, Text.DIVIDER_CHARS, true); //return delimiters is on
        if(fIn.hasMoreTokens()) {
            String strWord = "";
            String strDivider = "";
            String token;
            do {
                token = fIn.nextToken();
                if (token.length() == 1 && Text.DIVIDER_CHARS.contains(token)) { //divider
                    strDivider += token;
                } else { //word
                    strWord = token;
                    while (strWord.startsWith("-"))  {
                        strWord = strWord.substring(1);
                        //add "-" to the last divider
                        strDivider += "-";
                    }
                    String nextDivider = "";
                    while (strWord.endsWith("-"))  {
                        strWord = strWord.substring(0, strWord.length()-1);
                        nextDivider += "-";
                    }
                    if (strWord.length() > 0) {
                        //if (elemList.size() != 0) { //ignore divider before first word in sentence
                        dividers.add(strDivider);
                        //}
                        strDivider = nextDivider; //reset

                        elemList.add(strWord);
                    }

                }
            } while(fIn.hasMoreTokens());
            dividers.add(strDivider); //last divider
        }
        // init elemListLoverCase
//        for (String s : elemList) {
//            elemListLowerCase.add(s.toLowerCase());
//        }
        //contentLowerCase = content.toLowerCase();
    }

    public String getContent() {
        return content;
    }

    /** Return Html representation of this sentence.
     */
    public String getHtml(Text parent, int wordSearchMode){
        StringBuffer sb = new StringBuffer();

        if (getContent().trim().length() > 0) {
            String sWord;
            for(int i=0; i< elemList.size(); i++){
                sWord = elemList.get(i);
                //Search word
                sb.append(parent.escapeXml(dividers.get(i)));
                switch(wordSearchMode) {
                    case Text.FIND_WORD_AS_IS:
                    case Text.FIND_WORD_IGNORE_CASE:
                        sb.append(parent.getWordHtml(sWord, wordSearchMode));
                        break;
                    case Text.FIND_WORD_GERMAN:
                        if (i==0) {
                            //TODO: check german word pattern (all letters lowercase (Ex. arbeiten), first letter uppercase the rest lowercase (Ex. Arbeit))
                            // words not matching german word pattern process ignoring case
                            sb.append(parent.getWordHtml(sWord, Text.FIND_WORD_GERMAN));
                        } else {
                            sb.append(parent.getWordHtml(sWord, Text.FIND_WORD_AS_IS));
                        }
                        break;
                }
            }
            sb.append(parent.escapeXml(dividers.get(dividers.size()-1))); //last divider
        }

        return sb.toString();
    }

    public List<String> getElemList() {
        return elemList;
    }

    public boolean isNewParagraph() {
        return newParagraph;
    }

    public void setNewParagraph(boolean newParagraph) {
        this.newParagraph = newParagraph;
    }

}
