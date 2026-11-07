package com.liskovsoft.smartyoutubetv2.common.exoplayer.selector;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.video.ColorInfo;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import org.junit.Test;
import java.lang.reflect.Method;
import static org.junit.Assert.*;

/** Regression coverage for automatic quality, HDR and frame-rate grouping. */
public class AutomaticVideoSelectionTest {
    private static Format video(String codec, int width, int height, float fps, int bitrate) {
        return Format.createVideoSampleFormat(null, "video/x-vnd.on2.vp9", codec,
                bitrate, Format.NO_VALUE, width, height, fps, null, null);
    }

    private static boolean sameFps(float first, float second) throws Exception {
        Method method = RestoreTrackSelector.class.getDeclaredMethod(
                "sameContentFrameRate", Format.class, Format.class);
        method.setAccessible(true);
        return (boolean) method.invoke(null,
                video("vp09", 3840, 2160, first, 12000000),
                video("vp09", 1920, 1080, second, 5000000));
    }

    @Test public void higherResolutionWinsOverBitrate() {
        assertTrue(TrackSelectorManager.compareAutomaticVideo(
                video("vp09", 3840, 2160, 30, 8000000),
                video("vp09", 1920, 1080, 30, 25000000)) > 0);
    }

    @Test public void sameResolutionHigherFpsWins() {
        assertTrue(TrackSelectorManager.compareAutomaticVideo(
                video("vp09", 3840, 2160, 60, 14000000),
                video("vp09", 3840, 2160, 30, 20000000)) > 0);
    }

    @Test public void codecPreferenceWhenBitratesAreComparable() {
        assertTrue(TrackSelectorManager.compareAutomaticVideo(
                video("av01", 3840, 2160, 30, 14000000),
                video("vp09", 3840, 2160, 30, 16000000)) > 0);
    }

    @Test public void substantialBitrateDifferenceCanWin() {
        assertTrue(TrackSelectorManager.compareAutomaticVideo(
                video("vp09", 3840, 2160, 30, 22000000),
                video("av01", 3840, 2160, 30, 12000000)) > 0);
    }

    @Test public void nearEquivalentFpsCanAdapt() throws Exception {
        assertTrue(sameFps(23.976f, 24f));
        assertTrue(sameFps(29.97f, 30f));
        assertTrue(sameFps(59.94f, 60f));
    }

    @Test public void distinctFpsCannotAdapt() throws Exception {
        assertFalse(sameFps(24f, 30f));
        assertFalse(sameFps(30f, 60f));
        assertFalse(sameFps(25f, 50f));
    }

    @Test public void unknownFpsDoesNotMixWithKnown() throws Exception {
        assertFalse(sameFps(-1f, 30f));
        assertTrue(sameFps(-1f, -1f));
    }

    @Test public void hdrMetadataIsComparedSeparately() throws Exception {
        Method method = RestoreTrackSelector.class.getDeclaredMethod(
                "sameHdrTransfer", Format.class, Format.class);
        method.setAccessible(true);
        Format sdr = video("vp09", 3840, 2160, 30, 10000000);
        Format hdr = sdr.copyWithColorInfo(new ColorInfo(
                C.COLOR_SPACE_BT2020, C.COLOR_RANGE_LIMITED, C.COLOR_TRANSFER_ST2084, null));
        assertFalse((boolean) method.invoke(null, sdr, hdr));
        assertTrue((boolean) method.invoke(null, hdr, hdr));
    }
}
