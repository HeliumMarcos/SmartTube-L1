package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import com.liskovsoft.mediaserviceinterfaces.data.MediaFormat;
import com.liskovsoft.mediaserviceinterfaces.data.MediaItemFormatInfo;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Bounded, expiring cache of verified maximum stream resolution; never guesses from titles. */
public final class VideoQualityCache {
    private static final long TTL_MS = 6L * 60 * 60 * 1000;
    private static final int MAX_ENTRIES = 400;
    private static final Map<String, Entry> CACHE = new LinkedHashMap<String, Entry>(64, .75f, true) {
        @Override protected boolean removeEldestEntry(Map.Entry<String, Entry> eldest) {
            return size() > MAX_ENTRIES;
        }
    };
    private static final class Entry {
        final int tier;
        final long expires;
        Entry(int tier, long expires) { this.tier = tier; this.expires = expires; }
    }
    private VideoQualityCache() {}

    /** 0 = unknown or below 4K, 4 = 4K, 8 = 8K. */
    public static int classify(int width, int height) {
        if (width <= 0 || height <= 0) return 0;
        int longEdge = Math.max(width, height);
        int shortEdge = Math.min(width, height);
        if (longEdge >= 7680 && shortEdge >= 4320) return 8;
        if (longEdge >= 3840 && shortEdge >= 2160) return 4;
        return 0;
    }

    public static int detect(MediaItemFormatInfo info) {
        if (info == null) return 0;
        int tier = 0;
        if (info.getAdaptiveFormats() != null) {
            for (MediaFormat format : info.getAdaptiveFormats()) {
                if (format != null) tier = Math.max(tier, classify(format.getWidth(), format.getHeight()));
            }
        }
        return tier;
    }

    public static synchronized Integer get(String videoId) {
        if (videoId == null) return null;
        Entry entry = CACHE.get(videoId);
        if (entry == null) return null;
        if (entry.expires <= System.currentTimeMillis()) {
            CACHE.remove(videoId);
            return null;
        }
        return entry.tier;
    }

    public static synchronized void put(String videoId, MediaItemFormatInfo info) {
        if (videoId == null || info == null) return;
        CACHE.put(videoId, new Entry(detect(info), System.currentTimeMillis() + TTL_MS));
    }

    public static synchronized void clear() { CACHE.clear(); }
}
