package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.disposables.Disposable;
import java.util.HashSet;
import java.util.Set;

/**
 * Best-effort, rate-limited resolution probes. Never blocks card binding.
 * At most two concurrent requests and twelve new requests per rolling minute.
 */
public final class VideoQualityProbe {
    public interface Listener { void onResult(String videoId, int tier); }
    private static final int MAX_ACTIVE = 2;
    private static final int MAX_PER_MINUTE = 12;
    private static final Set<String> IN_FLIGHT = new HashSet<>();
    private static int active;
    private static int issued;
    private static long windowStart;
    private VideoQualityProbe() {}

    public static void request(String videoId, Listener listener) {
        if (videoId == null || videoId.isEmpty() || listener == null) return;
        Integer cached = VideoQualityCache.get(videoId);
        if (cached != null) {
            listener.onResult(videoId, cached);
            return;
        }
        synchronized (VideoQualityProbe.class) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60000) { windowStart = now; issued = 0; }
            if (active >= MAX_ACTIVE || issued >= MAX_PER_MINUTE || !IN_FLIGHT.add(videoId)) return;
            active++;
            issued++;
        }
        try {
            MediaItemService service = YouTubeServiceManager.instance().getMediaItemService();
            service.getFormatInfoObserve(videoId).take(1).subscribe(info -> {
                VideoQualityCache.put(videoId, info);
                finish(videoId);
                Integer tier = VideoQualityCache.get(videoId);
                listener.onResult(videoId, tier == null ? 0 : tier);
            }, error -> finish(videoId));
        } catch (Exception error) {
            finish(videoId);
        }
    }

    private static synchronized void finish(String videoId) {
        if (IN_FLIGHT.remove(videoId)) active--;
    }
}
