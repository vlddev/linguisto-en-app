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
import java.util.stream.Collectors;

public class SentencePOS {

    private static final String LOG_TAG = SentencePOS.class.getSimpleName();

    private int startPosInText = 0;
    private int id = 0;
    private boolean newParagraph = false;
	private String content;
	private String contentLowerCase;
	private List<SentElem> elemList = new ArrayList<>();
	private MaxentTagger posTagger;

	public SentencePOS(String text, MaxentTagger posTagger) {
		this.content = text;
		this.posTagger = posTagger;
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
        String taggedString = posTagger.tagString(content);

        SentElem prevElem = null;
        for (String str : taggedString.split(" ")) {
            String[] wordTag = str.split(BuilderPOS.TagSeparator);
            if (wordTag.length < 2) {
                Log.d(LOG_TAG, String.format("Unexpected tagger output. Word-POSTag='%s' ", str));
            }
            String wf = wordTag[0];
            String tag = wordTag[1];
            if (EnPOSTagSet.getAllTags().contains(tag)) { //wordform
                if (prevElem != null && prevElem.getType() == SentElem.TYPE_WORD) {
                    elemList.add(new SentElem(wf.equals("'s")?"":" ", "", SentElem.TYPE_DIVIDER));
                }
                SentElem curElem = new SentElem(wf, tag);
                elemList.add(curElem);
                prevElem = curElem;
            } else { // divider
                prevElem = new SentElem(wf+" ", "", SentElem.TYPE_DIVIDER);
                elemList.add(prevElem);
            }
        }
        contentLowerCase = content.toLowerCase();
    }

    public String getNormalized() {
		StringBuilder sb = new StringBuilder(contentLowerCase);

		// remove first BOM in utf8
		if (sb.length() > 0) {
			byte[] bomArr = sb.substring(0, 1).getBytes();
			if (bomArr.length == 3 && bomArr[0] == (byte)0xEF && bomArr[1] == (byte)0xBB && bomArr[2] == (byte)0xBF) {
				//BOM in utf8
				sb.deleteCharAt(0);
			}
		}

		// remove first "char(63)"
		if (sb.length() > 0) {
			byte[] bomArr = sb.substring(0, 1).getBytes();
			if (bomArr.length == 1 && bomArr[0] == (byte)0x3F) {
				sb.deleteCharAt(0);
			}
		}
		
		//відкинути всі символи з множини Corpus.DEVIDER_CHARS на початку та в кінці тексту
		String sChar;
		while(sb.length() > 0) {
			sChar = sb.substring(0, 1);
			if (TextPOS.DIVIDER_CHARS.contains(sChar)) {
				sb.deleteCharAt(0);
			} else {
				break;
			}
		}
		while(sb.length() > 0) {
			sChar = sb.substring(sb.length()-1);
			if (TextPOS.DIVIDER_CHARS.contains(sChar)) {
				sb.deleteCharAt(sb.length()-1);
			} else {
				break;
			}
		}
		return sb.toString();
	}
	
	public String getContent() {
		return content;
	}

    /** Return html representation of this sentence.
     */
    public String getHtml(TextPOS parent, int wordSearchMode){
        StringBuilder sb = new StringBuilder();

        if (getContent().trim().length() < 1) {
            sb.append("<empty-line/>");
        } else {
            SentElem elem;
            for(int i=0; i < elemList.size(); i++){
                elem = elemList.get(i);
                if (elem.getType() == SentElem.TYPE_DIVIDER) {
                    sb.append(parent.escapeXml(elem.getValue()));
                } else if (elem.getType() == SentElem.TYPE_WORD) {
                    //Search word
                    switch(wordSearchMode) {
                        case TextPOS.FIND_WORD_AS_IS:
                        case TextPOS.FIND_WORD_IGNORE_CASE:
                            sb.append(parent.getWordHtml(elem.getValue(), elem.getTag(), wordSearchMode));
                            break;
                        case TextPOS.FIND_WORD_GERMAN:
                            if (i==0) {
                                //TODO: check german word pattern (all letters lowercase (Ex. arbeiten), first letter uppercase the rest lowercase (Ex. Arbeit))
                                // words not matching german word pattern process ignoring case
                                sb.append(parent.getWordHtml(elem.getValue(), elem.getTag(), TextPOS.FIND_WORD_GERMAN));
                            } else {
                                sb.append(parent.getWordHtml(elem.getValue(), elem.getTag(), TextPOS.FIND_WORD_AS_IS));
                            }
                            break;
                    }
                }
            }
        }

        return sb.toString();
    }

	public int getStartPosInText() {
		return startPosInText;
	}

	public void setStartPosInText(int startPosInText) {
		this.startPosInText = startPosInText;
	}

	public List<String> getElemList() {
        List<String> ret = new ArrayList<>();
        for (SentElem elem : elemList) {
            if (elem.getType() == SentElem.TYPE_WORD) {
                ret.add(elem.getValue());
            }
        }
        return ret;
	}

    public boolean isNewParagraph() {
        return newParagraph;
    }

    public void setNewParagraph(boolean newParagraph) {
        this.newParagraph = newParagraph;
    }

}
