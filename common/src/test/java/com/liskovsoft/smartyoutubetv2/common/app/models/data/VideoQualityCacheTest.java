package com.liskovsoft.smartyoutubetv2.common.app.models.data;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class VideoQualityCacheTest {
    @Test public void classifies4kAnd8k() {
        assertEquals(4, VideoQualityCache.classify(3840, 2160));
        assertEquals(8, VideoQualityCache.classify(7680, 4320));
        assertEquals(4, VideoQualityCache.classify(2160, 3840));
    }

    @Test public void rejectsLowerAndInvalidResolutions() {
        assertEquals(0, VideoQualityCache.classify(1920, 1080));
        assertEquals(0, VideoQualityCache.classify(2560, 1440));
        assertEquals(0, VideoQualityCache.classify(0, 4320));
        assertEquals(0, VideoQualityCache.classify(-1, -1));
    }

    @Test public void unknownVideosAreNotMislabelled() {
        VideoQualityCache.clear();
        assertNull(VideoQualityCache.get("not-in-cache"));
    }
}
