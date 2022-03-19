package com.vlad.linguisto.text;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class DbDictionary implements Dictionary {

    private DictDbHelper dbHelper;

    public DbDictionary(DictDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    @Override
    public List<Inf> getBaseForm(String wf, boolean ignoreCase) {
        return dbHelper.getBaseForm(wf, ignoreCase);
    }

    @Override
    public List<String> getTranslation(Inf word) {
        List<String> ret = new ArrayList<>();
        ret.add(word.getHtml());
        return ret;
    }

    @Override
    public Collection<Inf> checkUserKnow(Collection<Inf> words) {
        return words;
    }
}
