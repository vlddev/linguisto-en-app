package com.vlad.linguisto.tools;

import com.vlad.linguisto.db.DictDbHelper;
import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InfCache {

    private final int sizePerType = 100;
    private final DictDbHelper dbHelper;
    Map<Integer, List<Integer>> mapTypeInfId = new HashMap<>();

    Map<Integer, Inf> mapIdInf = new HashMap<>();

    public InfCache(DictDbHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Inf> getInfsRandom(int type, int count) {
        List<Inf> ret = new ArrayList<>();
        List<Integer> ids = mapTypeInfId.get(type);
        if (ids != null && ids.size() > 0) {
            Collections.shuffle(ids);
            for(int i = 0; i < count && i < ids.size(); i++) {
                ret.add(mapIdInf.get(ids.get(i)));
            }
        } else {
            initForType(type);
            ret = getInfsRandom(type, count);
        }
        return ret;
    }

    private void initForType(int type) {
        List<Inf> infs = dbHelper.getInfsRandom(type, sizePerType);
        List<Integer> ids = new ArrayList<>(infs.size());
        for (Inf inf : infs) {
            ids.add(inf.getId());
            mapIdInf.put(inf.getId(), inf);
        }
        mapTypeInfId.put(type, ids);
    }
}
