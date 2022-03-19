package com.vlad.linguisto.text;

import java.util.ArrayList;
import java.util.List;

public class PagedText {

    int pageSize;
    List<String> content = new ArrayList<>();

    public PagedText(String content, int pageSize) {
        this.pageSize = pageSize;
        Text text = new Text(content, null);
        StringBuffer sb = new StringBuffer();
        for (Sentence sent: text.getSentences()) {
            if (sb.length() > pageSize || (sb.length() < pageSize && sb.length() + sent.getContent().length() > pageSize)) {
                this.content.add(sb.toString());
                sb = new StringBuffer();
            }
            if (sent.isNewParagraph() && sb.length() > 0) {
                sb.append("\n");
            } else if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(sent.getContent());
        }
        if (sb.length() > 0) { //last page
            this.content.add(sb.toString());
        }
    }

    public int getPageCount() {
        return content.size();
    }

    public String getPageText(int page) {
        return content.get(page);
    }
}
