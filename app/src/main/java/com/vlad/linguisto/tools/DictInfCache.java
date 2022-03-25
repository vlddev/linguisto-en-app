package com.vlad.linguisto.tools;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.Weigher;
import com.vlad.linguisto.db.obj.Inf;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DictInfCache {
    private LoadingCache<String, List<Inf>> cache;
    private final int cacheSize = 500;

    public DictInfCache() {
        CacheLoader<String, List<Inf>> loader;
        loader = new CacheLoader<String, List<Inf>>() {
            @Override
            public List<Inf> load(String key) {
                return new ArrayList<Inf>();
            }
        };

        Weigher<String, List<Inf>> weighByRank;
        weighByRank = new Weigher<String, List<Inf>>() {
            @Override
            public int weigh(String key, List<Inf> value) {
                return value.stream().min(Comparator.comparing(Inf::getRank)).get().getRank();
            }
        };

        cache = CacheBuilder.newBuilder()
                .maximumWeight(1000)
                .weigher(weighByRank)
                .build(loader);
    }

    public boolean contains(String wf) {
        return cache.getIfPresent(wf) != null;
    }

    public List<Inf> get(String wf) {
        return (List)cache.getIfPresent(wf);
    }

    public void put(String wf, List<Inf> infs) {
        cache.put(wf, infs);
    }
}
