package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import com.liskovsoft.mediaserviceinterfaces.MediaItemService;
import com.liskovsoft.youtubeapi.service.YouTubeServiceManager;
import io.reactivex.disposables.Disposable;
import io.reactivex.disposables.Disposables;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/** Bounded, cancellable best-effort quality probes for visible video cards. */
public final class VideoQualityProbe {
    public interface Listener { void onResult(String videoId, int tier); }
    private static final int MAX_ACTIVE = 2;
    private static final int MAX_PER_MINUTE = 12;
    private static final Map<String, Request> IN_FLIGHT = new HashMap<>();
    private static int issued;
    private static long windowStart;

    private static final class Request {
        final AtomicReference<Disposable> upstream = new AtomicReference<>();
        boolean cancelled;
    }
    private VideoQualityProbe() {}

    /** Dispose when a card is recycled or detached; never holds a view reference. */
    public static Disposable request(String videoId, Listener listener) {
        if (videoId == null || videoId.isEmpty() || listener == null) return Disposables.empty();
        Integer cached = VideoQualityCache.get(videoId);
        if (cached != null) {
            listener.onResult(videoId, cached);
            return Disposables.empty();
        }
        final Request request;
        synchronized (VideoQualityProbe.class) {
            long now = System.currentTimeMillis();
            if (now - windowStart >= 60_000) { windowStart = now; issued = 0; }
            if (IN_FLIGHT.size() >= MAX_ACTIVE || issued >= MAX_PER_MINUTE || IN_FLIGHT.containsKey(videoId)) {
                return Disposables.empty();
            }
            request = new Request();
            IN_FLIGHT.put(videoId, request);
            issued++;
        }
        Disposable cancellation = Disposables.fromAction(() -> cancel(videoId, request));
        try {
            MediaItemService service = YouTubeServiceManager.instance().getMediaItemService();
            Disposable upstream = service.getFormatInfoObserve(videoId).take(1)
                    .doFinally(() -> finish(videoId, request))
                    .subscribe(info -> {
                        if (isCurrent(videoId, request)) {
                            VideoQualityCache.put(videoId, info);
                            Integer tier = VideoQualityCache.get(videoId);
                            if (isCurrent(videoId, request)) listener.onResult(videoId, tier == null ? 0 : tier);
                        }
                    }, error -> { /* An unavailable format must not be displayed as confirmed. */ });
            request.upstream.set(upstream);
            if (cancellation.isDisposed()) upstream.dispose();
        } catch (Exception error) {
            cancel(videoId, request);
        }
        return cancellation;
    }

    private static synchronized boolean isCurrent(String id, Request request) {
        return !request.cancelled && IN_FLIGHT.get(id) == request;
    }

    private static synchronized void finish(String id, Request request) {
        if (IN_FLIGHT.get(id) == request) IN_FLIGHT.remove(id);
    }

    private static void cancel(String id, Request request) {
        Disposable upstream;
        synchronized (VideoQualityProbe.class) {
            request.cancelled = true;
            if (IN_FLIGHT.get(id) == request) IN_FLIGHT.remove(id);
            upstream = request.upstream.get();
        }
        if (upstream != null) upstream.dispose();
    }
}
