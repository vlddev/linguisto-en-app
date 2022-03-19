package com.vlad.linguisto.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PageCache {

    private final Map<Integer, String> cacheMap = new HashMap<>();
    private final int cacheSize = 7;

    public PageCache() {}

    public boolean contains(int page) {
        return cacheMap.containsKey(page);
    }

    public String get(int page) {
        return cacheMap.get(page);
    }

    public void put(int page, String content) {
        cacheMap.put(page, content);
        if (cacheMap.size() > cacheSize) {
            List<Integer> keys = new ArrayList<>();
            keys.addAll(cacheMap.keySet());
            Collections.sort(keys);
            for (Integer key : keys.subList(0, keys.size()-cacheSize)) {
                cacheMap.remove(key);
            }
        }
    }

}
