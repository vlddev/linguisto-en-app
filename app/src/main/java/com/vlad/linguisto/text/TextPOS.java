package com.vlad.linguisto.text;

import com.vlad.linguisto.db.obj.Inf;

import edu.stanford.nlp.tagger.maxent.MaxentTagger;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/** Input text. Contains set of Sentences. 
 */
public class TextPOS {

    public static final String LINE_SEPARATOR = System.getProperty("line.separator");
    public static final String DIVIDER_CHARS = ".,:;!?§$%&/()=[]\\#+*<>{}\"—…«»“”•~^‹› \t\r\n";

    /**
     * FIND_WORD_IGNORE_CASE: convert word from text to lower case
     *                    and convert words in DB to lower case
     */
    public static final int FIND_WORD_IGNORE_CASE = 0;
    /**
     * FIND_WORD_AS_IS: no converting, match all as is.
     */
    public static final int FIND_WORD_AS_IS = 1;
    /**
     * FIND_WORD_GERMAN: match ignoring case:
     *                     1) first word in sentence
     *                     2) words not matching german word pattern (all letters lowercase (Ex. arbeiten), first letter uppercase the rest lowercase (Ex. Arbeit)).
     *                   All the rest match as is.
     */
    public static final int FIND_WORD_GERMAN = 2;

    public static final Logger log = Logger.getLogger(TextPOS.class.getName());

    private Dictionary dict;

    private Locale lang;
    private Locale targetLang;
    private String content;

    private MaxentTagger posTagger;
    private EnPOSTagSet enPOSTagSet = new EnPOSTagSet();

    //Sentences
    List<SentencePOS> sentences;

    //number of words in text
    int wCount;
    //number of recognized words in text
    int totalRecWCount;
    //number of distinct recognized words in text
    int foundWords;
    //number of known words in the set of different recognized words
    int knownWords;

    //distinct words
    Set<String> distWords;

    //unrecognized words
    HashSet<String> unrecognizedWords = new HashSet<>();

    // words without translation
    HashMap<Long, Inf> notTranslated = new HashMap<>();

    //mapping of word-forms to word-bases
    HashMap<String, Collection<Inf>> wfMap = new HashMap<>();

    //dictionary for this text
    HashMap<Inf, List<String>> textDict = new HashMap<>();

    /** Constructor for texts
     */
    public TextPOS(String content, Dictionary d, MaxentTagger posTagger) {
    	this(content, "utf-8", d, posTagger);
    }

    /** Constructor for texts
     */
    public TextPOS(String content, String encoding, Dictionary d, MaxentTagger posTagger) {
    	if(d==null){
	        throw new NullPointerException("Dictionary is null");
    	}
        this.dict = d;
        this.lang = Locale.ENGLISH;
        this.content = content;
        this.posTagger = posTagger;

        //init object
        init();
    }

    public List<SentencePOS> getSentences() {
        return sentences;
    }

    public HashMap<Inf, List<String>> getTextDict() {
        return textDict;
    }

    /**
     */
    private void init() {
        long start = System.currentTimeMillis();
        // remove first BOM in utf8
        if (content.length() > 0) {
            byte[] bomArr = content.substring(0, 1).getBytes();
            if (bomArr.length == 3 && bomArr[0] == (byte)0xEF && bomArr[1] == (byte)0xBB && bomArr[2] == (byte)0xBF) {
                //BOM in utf8
                content = content.substring(1);
            }
        }

        sentences = new ArrayList<>();

        content = preprocess(content);

        SentenceReader2 sr = new SentenceReader2(content);
        try {
            String str = sr.readSentence();
            SentencePOS sent;
            int prevSentPos = 0;
            boolean newParagraph = false;
            boolean prevSentEndsWithEmptyLine = false;
            boolean sentEndsWithEmptyLine = false;
            while (str != null) {
                newParagraph = sentences.size() == 0 || prevSentEndsWithEmptyLine;
                sentEndsWithEmptyLine = str.endsWith("\n") || str.endsWith("\r\n");
                str = str.replace(LINE_SEPARATOR," ").trim();
                if (str.length() > 0) {
                    sent = new SentencePOS(str, posTagger);
                    sent.setStartPosInText(prevSentPos);
                    sent.setId(sentences.size());
                    if (newParagraph) {
                        sent.setNewParagraph(true);
                    }
                    sentences.add(sent);
                    prevSentPos += sent.getElemList().size();
                } else {
                    sentEndsWithEmptyLine = true;
                }
                str = sr.readSentence();
                prevSentEndsWithEmptyLine = sentEndsWithEmptyLine;
                //TODO: check words count in sentence.
                // If no words, add string to the previous sentence
            }
        } catch (IOException e) {
            log.log(Level.SEVERE, "Error: "+e.getMessage(), e);
        }
        log.info("Text.init() took "+(System.currentTimeMillis() - start)+" ms.");
    }

    private String preprocess(String content) {
        String text = content;

        //1. replace "..." with "…"
        text = text.replace("...","…");
        //2. replace "’" with "'"
        text = text.replace("’","'");

        //preprocess text
        text = text.replace("—", " — ").trim();
        text = text.replace("won't", "will not");
        text = text.replace("Won't", "Will not");
        text = text.replace("can't", "can not");
        text = text.replace("Can't", "Can not");
        text = text.replace("ain't", "ain-t");
        text = text.replace("Ain't", "Ain-t");

        text = text.replace("n't", " not");

        text = text.replace("ain-t", "ain't");
        text = text.replace("Ain-t", "Ain't");

        text = text.replace("'ll", " will");

        text = text.replace("he's", "he is");
        text = text.replace("He's", "He is");
        text = text.replace("here's", "here is");
        text = text.replace("Here's", "Here is");
        text = text.replace("i'm", "i am");
        text = text.replace("I'm", "I am");
        text = text.replace("it's", "it is");
        text = text.replace("It's", "It is");
        text = text.replace("i've", "i have");
        text = text.replace("I've", "I have");
        text = text.replace("let's", "let us");
        text = text.replace("Let's", "Let us");
        text = text.replace("that's", "that is");
        text = text.replace("That's", "That is");
        text = text.replace("there's", "there is");
        text = text.replace("There's", "There is");
        text = text.replace("they're", "they are");
        text = text.replace("They're", "They are");
        text = text.replace("we're", "we are");
        text = text.replace("We're", "We are");
        text = text.replace("we've", "we have");
        text = text.replace("We've", "We have");
        text = text.replace("what's", "what is");
        text = text.replace("What's", "What is");
        text = text.replace("you're", "you are");
        text = text.replace("You're", "You are");
        text = text.replace("you've", "you have");
        text = text.replace("You've", "You have");

        text = text.trim().replaceAll(" +", " ");

        return text;
    }


    /**
     * prepare dictionary for this text
     */
    public void prepareDict(int wordSearchMode){
        long start = System.currentTimeMillis();
        this.targetLang = targetLang;
        distWords =  getDistinctWords(wordSearchMode);
        if (distWords == null) {
            log.warning("Text.distWord is Null");
            return;
        } else {
            log.info("Text contains "+ distWords.size()+" distinct words.");
        }
        
        for(String word : distWords) {
            try {
                //System.out.println(word);
                HashSet<Inf> searchRes = new HashSet<>();
                switch(wordSearchMode) {
                    case TextPOS.FIND_WORD_AS_IS:
                        searchRes.addAll(dict.getBaseForm(word, false));
                        break;
                    case TextPOS.FIND_WORD_IGNORE_CASE:
                        //searchRes.addAll(dict.getBaseForm(word.toLowerCase(), lang, false));
                        searchRes.addAll(dict.getBaseForm(word, true));
                        break;
                    case TextPOS.FIND_WORD_GERMAN:
                        searchRes.addAll(dict.getBaseForm(word, false));
                        if (searchRes.size() == 0) {
                            searchRes.addAll(dict.getBaseForm(word.toLowerCase(), false));
                        }
                        break;
                }
                //dict.checkUserKnow(searchRes);
                if( searchRes.size() > 0) {
                    wfMap.put(word, searchRes);
                    for(Inf w : searchRes) {
                        List<String> translations = dict.getTranslation(w);
                        if (translations != null && translations.size() > 0) {
                            textDict.put(w, translations);
                        }
                    }
                } else {
                    unrecognizedWords.add(word);
                }
                //if(nCount%100 == 0) LogUtil.writeLog("another 100");
            } catch(Exception e){
                log.log(Level.SEVERE, "Error processing word '" + word + "'", e);
            }
        }
        log.info("Text.init() took "+(System.currentTimeMillis() - start)+" ms.");
    }

    public Set<String> getDistinctWords(int wordSearchMode) {
        Set<String> ret = new HashSet<>();
        String s;
        for (SentencePOS sent : getSentences()) {
            for (int i = 0; i < sent.getElemList().size(); i++) {
                s = sent.getElemList().get(i);
                if (s.length() > 0) {
                    switch(wordSearchMode) {
                        case TextPOS.FIND_WORD_AS_IS:
                            ret.add(s);
                        case TextPOS.FIND_WORD_IGNORE_CASE:
                            ret.add(s.toLowerCase());
                            if (!s.toLowerCase().equals(s)) {
                                ret.add(s);
                            }
                            break;
                        case TextPOS.FIND_WORD_GERMAN:
                            //TODO: check german word pattern (all letters lowercase (Ex. arbeiten), first letter uppercase the rest lowercase (Ex. Arbeit))
                            // words not matching german word pattern process ignoring case
                            if (i == 0) {
                                ret.add(s.toLowerCase());
                                if (!s.toLowerCase().equals(s)) {
                                    ret.add(s);
                                }
                            } else {
                                ret.add(s);
                            }
                            break;
                    }
                }
            }
        }
        return ret;
    }

    /** Return HTML representation of this text.
     */
    public String getHtml(int wordSearchMode) {
        StringBuilder sbRet = new StringBuilder();

        int sentInd = 0;
        for(SentencePOS sent : sentences){
            if (sent.isNewParagraph()) {
                if (sentInd>0) {
                    sbRet.append("</p>\n<p>");
                } else {
                    sbRet.append("\n<p>");
                }
            }
            sbRet.append(sent.getHtml(this, wordSearchMode));
            sentInd++;
        }
        return sbRet.toString();
    }
    
    public String escapeXml(String str) {
    	return str.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;"); //.replace('\a', '')
    }

    /** Return html representation of this word.
     */
    public String getWordHtml(String word, String posTag, int wordSearchMode){
        if (posTag.startsWith("NNP")) {
            wordSearchMode = TextPOS.FIND_WORD_AS_IS;
        }
        StringBuilder sb = new StringBuilder();
        String searchWord = word;
        String displayWord = escapeXml(word);
        Collection<Inf> wordBases = null;
        switch(wordSearchMode) {
            case TextPOS.FIND_WORD_IGNORE_CASE:
                searchWord = word.toLowerCase();
                wordBases = wfMap.get(searchWord);
                break;
            case TextPOS.FIND_WORD_AS_IS:
            case TextPOS.FIND_WORD_GERMAN:
            default:
                wordBases = wfMap.get(searchWord);
                break;
        }

        if(wordBases == null || wordBases.isEmpty()) { //unrecognized word
            if (wordSearchMode == TextPOS.FIND_WORD_GERMAN) {
                // ignore case if nothing found
                sb.append(getWordHtml(word, "", TextPOS.FIND_WORD_IGNORE_CASE));
            } else {
                sb.append(displayWord);
            }
        } else { // recognized
            if (posTag != null && posTag.length() > 0) {
                if (lang.getLanguage().equals("en")) {
                    String strWordTypes = EnPOSTagSet.getWordType(posTag);
                    if (strWordTypes.length() > 0) {
                        Integer[] wordTypes = convertStrToInt(strWordTypes);
                        Collection<Inf> filteredWordBases = filterWordBases(wordBases, wordTypes);
                        if (filteredWordBases.size() > 0) {
                            wordBases = filteredWordBases;
                        } else {
                            log.info(String.format("Ignore POSTag. There was no WordBases for word '%s', wordTypes '%s' (POSTag '%s')", word, strWordTypes, posTag));
                        }
                    }
                }
            }
            boolean bUserKnows = true;
            for (Inf w : wordBases) {
                bUserKnows &= w.isKnown();
                if (!bUserKnows) break;
            }
            if(bUserKnows){ //user knows this word
                if (wordBases.size() == 1) {
                    if (lang.getLanguage().equals("de")) {
                        int wordType = wordBases.iterator().next().getType();
                        if (wordType == 2 || wordType == 3 || wordType == 4) { //German noun
                        	switch (wordType) {
                        	case 2:
                                sb.append(displayWord).append("<sup>m</sup>");
                                break;
                        	case 3:
                                sb.append(displayWord).append("<sup>f</sup>");
                                break;
                        	case 4:
                                sb.append(displayWord).append("<sup>n</sup>");
                                break;
                        	}
                        } else {
                            sb.append(displayWord);
                        }
                    } else {
                        Inf wordBase = wordBases.iterator().next();
                        if (wordBase.isRecentlyLearned()) {
                            //recently learned
                            String linkStyle = "style=\"color: green\"";
                            sb.append(" <a href=\"dict://infid/").append(wordBase.getId().toString()).append("\" "+linkStyle+">"+word+"</a>");
                        } else {
                            sb.append(displayWord);
                        }
                    }
                } else {
                    sb.append(displayWord);
                }
            } else { //add all unknown words
                boolean bMakeLink = false;
                String wordNoteLink = "word/"+searchWord;
                if (wordBases.size() == 1) {
                    Inf wordBase = wordBases.iterator().next();
                    wordNoteLink = "infid/"+wordBase.getId().toString();
                } else if (wordBases.size() > 1) {
                    wordNoteLink = "infid/";
                    int i = 0;
                    for (Inf inf : wordBases) {
                        wordNoteLink += (i==0?"":",") + inf.getId();
                        i++;
                    }
                }

                boolean bUserKnowsAll = true;
                for (Inf w : wordBases) {
                    if (!w.isKnown()) {
                        bUserKnowsAll = false;
                        break;
                    }
                }
                if (!bUserKnowsAll) {
                    bMakeLink = true;
                }
                if (bMakeLink) {
                    sb.append(" <a href=\"dict://").append(wordNoteLink).append("\">"+word+"</a>");
                } else {
                    sb.append(word);
                }
                if (lang.getLanguage().equals("de") && wordBases.size() == 1) {
                    int wordType = wordBases.iterator().next().getType();
                    switch (wordType) {
                    case 2:
                        sb.append("<sup>m</sup>");
                        break;
                    case 3:
                        sb.append("<sup>f</sup>");
                        break;
                    case 4:
                        sb.append("<sup>n</sup>");
                        break;
                    }
                }
            }
        }
        return sb.toString();
    }

    /**
     * Converts a string containing comma separated list of integers to array
     * @param str - comma separated list of integers
     */
    private Integer[] convertStrToInt(String str) {
        List<Integer> ret = new ArrayList<>();
        for (String s : str.split(",")) {
            ret.add(Integer.valueOf(s));
        }
        return (Integer[])ret.toArray(new Integer[ret.size()]);
    }

    private Collection<Inf> filterWordBases(Collection<Inf> words, Integer[] wordTypes) {
        Collection<Inf> ret = new ArrayList<>();
        for (Inf inf : words) {
            if (ArrayUtils.contains(wordTypes, inf.getType())) {
                ret.add(inf);
            }
        }
        return ret;
    }


    /**
     * Returns statistics on last processed test
     */
    public Dictionary getDictionary(){
        return dict;
    }
    
    /** Return number of words in this text.
     *  Use this method after compile()
     */
    public int getWordCount(){
        return wCount;
    }
    
    /** Return number of recognized words in this text.
     *  Use this method after getHtmlForm()
     */
    public int getRecognizedWordCount(){
        return wCount;
    }

}