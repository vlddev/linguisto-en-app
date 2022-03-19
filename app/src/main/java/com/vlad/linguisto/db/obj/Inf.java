package com.vlad.linguisto.db.obj;

import com.vlad.linguisto.AppManager;
import com.vlad.linguisto.R;
import com.vlad.linguisto.semantic.SChunk;
import com.vlad.linguisto.semantic.Styles;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;


public class Inf extends BaseObj {

    public final static int recentlyLearnedDays = 90;
    public final static int recapAfterDays = 60;
    private final static long recentDiff = TimeUnit.DAYS.toMillis(1) * recentlyLearnedDays; //90 days

    private String inf;
    private Integer type;
    private String transcription;
    private Integer rank;
    private boolean known;
    private Date learnDate;
    private int quizScore = 0;
    private List<Translation> trList = null;
    private List<WordForm> wfList = null;
    public static Map<Integer, String> wordTypeMap = new HashMap<>();

    public Inf() {
    }

    public Inf(String inf, Integer type) {
        this();
        setInf(inf);
        setType(type);
    }

    @Override
    public void setId(Integer id) {
        this.id = id;
        if (wfList != null) {
            for(WordForm wf : wfList) {
                wf.setFkInf(id);
            }
        }
    }

    public String getInf() {
        return inf;
    }

    public String getUrlInf() {
        return inf.replace(" ", "%20");
    }

    public void setInf(String inf) {
        this.inf = inf;
    }

    public Integer getType() {
        return type;
    }

    public String getTypeAsString() {
        return wordTypeMap.get(getType());
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTranscription() {
        return transcription;
    }

    public void setTranscription(String transcription) {
        this.transcription = transcription;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }

    public boolean isKnown() {
        return known;
    }

    public void setKnown(boolean known) {
        this.known = known;
    }

    public Date getLearnDate() {
        return learnDate;
    }

    public void setLearnDate(Date learnDate) {
        this.learnDate = learnDate;
    }

    public boolean isRecentlyLearned() {
        boolean ret = false;
        if(isKnown() && getLearnDate() != null) {
            if (System.currentTimeMillis() - getLearnDate().getTime() < recentDiff) {
                ret = true;
            }
        }
        return ret;
    }

    public int getQuizScore() {
        return quizScore;
    }

    public void setQuizScore(int quizScore) {
        if (quizScore < 0) {
            this.quizScore = 0;
        } else {
            this.quizScore = quizScore;
        }
    }

    public List<WordForm> getWfList() {
        return wfList;
    }

    public String getWf(String fid) {
        String ret = "-";
        if (getWfList() != null) {
            for (WordForm wf : getWfList()) {
                if (fid.equals(wf.getFid())) {
                    ret = wf.getWf();
                    break;
                }
            }
        }
        return ret;
    }

    public void setWfList(List<WordForm> wfList) {
        this.wfList = wfList;
        if (wfList != null ) {
            for(WordForm wf : wfList) {
                wf.setFkInf(id);
            }
        }
    }

    public List<Translation> getTrList() {
        return trList;
    }

    public void setTrList(List<Translation> trList) {
        this.trList = trList;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        boolean ret = false;
        if (obj instanceof Inf) {
            Inf otherInf = (Inf)obj;
            ret = new EqualsBuilder().
                // if deriving: appendSuper(super.equals(obj)).
                append(getId(), otherInf.getId()).
                isEquals();
        }
        return ret;
    }

    @Override
    public int hashCode(){
        return new HashCodeBuilder(17, 31). // two randomly chosen prime numbers
                // if deriving: appendSuper(super.hashCode()).
                append(getId()).
                toHashCode();
    }

    public boolean equals2(Object obj) {
        if (this == obj) return true;
        boolean ret = false;
        if (obj instanceof Inf) {
            Inf otherInf = (Inf)obj;
            ret = new EqualsBuilder().
                    // if deriving: appendSuper(super.equals(obj)).
                    append(getId(), otherInf.getId()).
                    append(getType(), otherInf.getType()).
                    append(getInf(), otherInf.getInf()).
                    append(getTranscription(), otherInf.getTranscription()).
                    isEquals();
        }
        return ret;
    }

    public String getHeadHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append("<p style=\"font-size:120%;\"><b>").append(getInf()).append("</b>");
        if (getTranscription() != null) {
            sb.append("<span style=\"color: silver;\"> ").append(getTranscription()).append("</span>");
        }
        sb.append("<span style=\"color: green\"> ").append(getTypeAsString()).append("</span>");
        if (getRank() != null) {
            sb.append(Styles.rankBegin).append(" R:").append(getRank()).append("</span>");
        }
        sb.append("</p>\n");
        sb.append("<span style=\"border-radius: 15px; padding:5px; background-color: "+(isKnown()?"green":"yellow")+"\">");
        sb.append((isKnown()?
                AppManager.getInstance(null).getContextString(R.string.btn_known) :
                AppManager.getInstance(null).getContextString(R.string.btn_unknown)));
        sb.append("</span>");
        if (getRank() != null) {
            sb.append(String.format("<a href=\"dict://rank/%s\"> ⇐ </a>", getRank()-1));
            sb.append(" | ");
            sb.append(String.format("<a href=\"dict://rank/%s\"> ⇒ </a>", getRank()+1));
        }
        return sb.toString();
    }

    public String getHtml() {
        StringBuilder sb = new StringBuilder();
        sb.append(getHeadHtml());
        sb.append("<table style=\"width:100%\"><tbody>\n");
        for (Translation tr : getTrList()) {
            // translation
            sb.append("<tr><td style=\"width:20px\"></td><td colspan=\"2\">");
            for (SChunk chunk : tr.getTranslationChunks()) {
                if (chunk.isLink()) {
                    sb.append(String.format("<a href=\"dict://%s\">%s</a>", chunk.getLinkId(), chunk.getText()));
                } else {
                    sb.append(String.format("%s%s</span>", chunk.getStyleBegin()!=null?chunk.getStyleBegin():"<span>", chunk.getText()));
                }
            }
            sb.append("</td></tr>\n");
            // examples
            List<List<SChunk>> exList = tr.getExamplesChunks();
            if (exList != null && exList.size() > 0) {
                sb.append("<tr><td style=\"width:20px\"></td><td style=\"width:20px\"></td><td>\n");
                sb.append("<table style=\"width:100%\"><tbody>\n");
                for(List<SChunk> ex : exList) {
                    sb.append("<tr><td>");
                    for (SChunk chunk : ex) {
                        if (chunk.isLink()) {
                            sb.append(String.format("<a href=\"dict://%s\">%s</a>", chunk.getLinkId(), chunk.getText()));
                        } else {
                            sb.append(String.format("%s%s</span>", chunk.getStyleBegin()!=null?chunk.getStyleBegin():"<span>", chunk.getText()));
                        }
                    }
                    sb.append("</td></tr>\n");
                }
                sb.append("</tbody></table>\n");
                sb.append("</td></tr>\n");
            }
        }
        sb.append("</tbody></table>\n");
        return sb.toString();
    }
}
