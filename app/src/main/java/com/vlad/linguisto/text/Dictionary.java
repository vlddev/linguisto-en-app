package com.vlad.linguisto.text;

import com.vlad.linguisto.db.obj.Inf;

import java.util.Collection;
import java.util.List;

/**
 * Common dictionary interface.
 */
public interface Dictionary {

    /**
     * Find word base
     * @param wf
     * @return
     */
    List<Inf> getBaseForm(String wf, boolean ignoreCase);

    /**
     *  Gets translation articles (in HTML) from dictionary.
     *
     * @param word
     * @return  list of Strings as HTML
     */
    List<String> getTranslation(Inf word);

    //List<Inf> getTranslation(List<Inf> wfList);

    Collection<Inf> checkUserKnow(Collection<Inf> words);

}