package com.liskovsoft.smartyoutubetv2.common.exoplayer.selector;

import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.RendererCapabilities;
import com.google.android.exoplayer2.source.TrackGroup;
import com.google.android.exoplayer2.source.TrackGroupArray;
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector.Parameters;
import com.google.android.exoplayer2.trackselection.TrackSelection.Definition;
import com.google.android.exoplayer2.video.ColorInfo;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.AudioTrack;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.selector.track.VideoTrack;
import com.liskovsoft.smartyoutubetv2.common.exoplayer.versions.selector.RestoreTrackSelector;
import org.junit.Test;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import static org.junit.Assert.*;

/** Regression coverage for automatic quality, HDR and frame-rate grouping. */
public class AutomaticVideoSelectionTest {
    private static final int SEAMLESS = RendererCapabilities.FORMAT_HANDLED | RendererCapabilities.ADAPTIVE_SEAMLESS;

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
                hdrVideo("vp09", 3840, 2160, 24, 30000000),
                hdrVideo("av01", 3840, 2160, 24, 12000000)) > 0);
    }

    @Test public void bestHdrTrackWinsOverAnSdrLadder() throws Exception {
        TrackGroup sdr = new TrackGroup(video("vp09", 3840, 2160, 24, 16780000),
                video("vp09", 2560, 1440, 24, 8310000));
        TrackGroup vp9 = new TrackGroup(hdrVideo("vp09", 3840, 2160, 24, 21990000));
        TrackGroup av1 = new TrackGroup(hdrVideo("av01", 3840, 2160, 24, 26200000));
        TrackGroupArray groups = new TrackGroupArray(sdr, vp9, av1);
        int[][] support = {{SEAMLESS, SEAMLESS}, {SEAMLESS}, {SEAMLESS}};

        Definition selection = automaticSelection(groups, support);
        assertSame(av1, selection.group);
        assertArrayEquals(new int[]{0}, selection.tracks);

        support[2][0] = RendererCapabilities.FORMAT_EXCEEDS_CAPABILITIES;
        assertSame(vp9, automaticSelection(groups, support).group);
    }

    @Test public void bandwidthAlternativesStayInTheBestTracksHdrAndFpsFamily() throws Exception {
        TrackGroup mixed = new TrackGroup(
                hdrVideo("vp09", 3840, 2160, 24, 21990000),
                hdrVideo("vp09", 1920, 1080, 23.976f, 6000000),
                hdrVideo("vp09", 1920, 1080, 30, 6000000),
                video("vp09", 1920, 1080, 24, 6000000),
                hdrVideo("av01", 1920, 1080, 24, 6000000));
        TrackGroupArray groups = new TrackGroupArray(mixed);
        int[][] support = {{SEAMLESS, SEAMLESS, SEAMLESS, SEAMLESS, SEAMLESS}};

        assertArrayEquals(new int[]{0, 1}, automaticSelection(groups, support).tracks);

        support[0][0] = RendererCapabilities.FORMAT_HANDLED;
        assertArrayEquals(new int[]{0}, automaticSelection(groups, support).tracks);
    }

    @Test public void threeCodecsUseTheSameBitrateThresholdInEitherOrder() throws Exception {
        TrackGroup av1 = new TrackGroup(hdrVideo("av01", 3840, 2160, 24, 12000000));
        TrackGroup vp9 = new TrackGroup(hdrVideo("vp09", 3840, 2160, 24, 17000000));
        TrackGroup hevc = new TrackGroup(hdrVideo("hvc1", 3840, 2160, 24, 24000000));
        int[][] support = {{SEAMLESS}, {SEAMLESS}, {SEAMLESS}};

        assertSame(vp9, automaticSelection(new TrackGroupArray(av1, vp9, hevc), support).group);
        assertSame(vp9, automaticSelection(new TrackGroupArray(hevc, vp9, av1), support).group);
    }

    @Test public void audioSummaryUsesCodecAndOnlyReportedChannels() {
        Format surround = Format.createAudioSampleFormat(null, "audio/eac3", null,
                384000, Format.NO_VALUE, 6, 48000, null, null, 0, null);
        Format unknown = Format.createAudioSampleFormat(null, "audio/eac3", null,
                384000, Format.NO_VALUE, Format.NO_VALUE, 48000, null, null, 0, null);
        assertEquals("Dolby Digital Plus · 5.1 · 384 kbps",
                TrackInfoFormatter2.buildAudioDetails(surround, "channels unknown", true));
        assertEquals("Dolby Digital Plus · channels unknown",
                TrackInfoFormatter2.buildAudioDetails(unknown, "channels unknown", false));
    }

    @Test public void videoMenuMarksCurrentInputInsteadOfQueuedResolution() {
        VideoTrack twoK = new VideoTrack(TrackSelectorManager.RENDERER_INDEX_VIDEO);
        twoK.format = Format.createVideoSampleFormat("shared", "video/av01", "av01",
                13500000, Format.NO_VALUE, 2560, 1440, 60, null, null);
        VideoTrack fourK = new VideoTrack(TrackSelectorManager.RENDERER_INDEX_VIDEO);
        fourK.format = Format.createVideoSampleFormat("shared", "video/av01", "av01",
                27000000, Format.NO_VALUE, 3840, 2160, 60, null, null);
        Format input = Format.createVideoSampleFormat("shared", "video/av01", "av01",
                Format.NO_VALUE, Format.NO_VALUE, 3840, 2160, 60, null, null);

        List<FormatItem> formats = ExoFormatItem.fromVideoTracks(
                new LinkedHashSet<>(Arrays.asList(twoK, fourK)), input, twoK.format);

        assertFalse(formats.get(0).isSelected());
        assertTrue(formats.get(1).isSelected());
        assertSame(fourK, formats.get(1).getTrack());
    }

    @Test public void manifestResolutionWinsWhenSabrReusesDecoderDimensions() {
        VideoTrack twoK = new VideoTrack(TrackSelectorManager.RENDERER_INDEX_VIDEO);
        twoK.format = Format.createVideoSampleFormat("700", "video/av01", "av01",
                13078242, Format.NO_VALUE, 2560, 1440, 60, null, null);
        Format staleInput = Format.createVideoSampleFormat("700", "video/av01", "av01",
                13078242, Format.NO_VALUE, 3840, 2160, 60, null, null);

        assertSame(twoK.format, ExoFormatItem.resolveVideoSource(Collections.singleton(twoK), staleInput));
        assertTrue(ExoFormatItem.fromVideoTracks(Collections.singleton(twoK), staleInput, null).get(0).isSelected());
    }

    @Test public void audioMenuMarksSourceAndKeepsChannelCountsSeparate() {
        AudioTrack surround = new AudioTrack(TrackSelectorManager.RENDERER_INDEX_AUDIO);
        surround.format = Format.createAudioSampleFormat("surround", "audio/eac3", null,
                384000, Format.NO_VALUE, Format.NO_VALUE, 48000, null, null, 0, null);
        AudioTrack stereo = new AudioTrack(TrackSelectorManager.RENDERER_INDEX_AUDIO);
        stereo.format = Format.createAudioSampleFormat("stereo", "audio/eac3", null,
                192000, Format.NO_VALUE, 2, 48000, null, null, 0, null);

        List<FormatItem> formats = ExoFormatItem.fromAudioTracks(
                new LinkedHashSet<>(Arrays.asList(surround, stereo)), surround.format,
                Collections.singletonMap(surround.format, 6));

        assertTrue(formats.get(0).isSelected());
        assertTrue(formats.get(0).getTitle().toString().contains("5.1"));
        assertFalse(formats.get(1).isSelected());
        assertTrue(formats.get(1).getTitle().toString().contains("2.0"));
        assertSame(surround, formats.get(0).getTrack());
    }

    @Test public void sampleChannelsOverrideManifestAndFallBackWhenMissing() {
        Format manifest = Format.createAudioSampleFormat("audio", "audio/eac3", "ec-3",
                384000, Format.NO_VALUE, 6, 48000, null, null, 0, null);
        Format unknown = Format.createAudioSampleFormat(null, "audio/eac3", "ec-3",
                Format.NO_VALUE, Format.NO_VALUE, Format.NO_VALUE, 48000, null, null, 0, null);
        Format stereo = Format.createAudioSampleFormat(null, "audio/eac3", "ec-3",
                Format.NO_VALUE, Format.NO_VALUE, 2, 48000, null, null, 0, null);
        assertEquals(6, unknown.copyWithManifestFormatInfo(manifest).channelCount);
        assertEquals(384000, unknown.copyWithManifestFormatInfo(manifest).bitrate);
        assertEquals(2, stereo.copyWithManifestFormatInfo(manifest).channelCount);
    }

    private static Definition automaticSelection(TrackGroupArray groups, int[][] support) throws Exception {
        Method method = RestoreTrackSelector.class.getDeclaredMethod("selectAutomaticVideo",
                TrackGroupArray.class, int[][].class, Parameters.class, boolean.class);
        method.setAccessible(true);
        return (Definition) method.invoke(null, groups, support, Parameters.DEFAULT, true);
    }

    private static Format hdrVideo(String codec, int width, int height, float fps, int bitrate) {
        String mime;
        switch (codec) {
            case "av01": mime = "video/av01"; break;
            case "hvc1": mime = "video/hevc"; break;
            default: mime = "video/x-vnd.on2.vp9"; break;
        }
        return Format.createVideoSampleFormat(null, mime, codec, bitrate, Format.NO_VALUE, width, height,
                fps, null, Format.NO_VALUE, Format.NO_VALUE, null, Format.NO_VALUE,
                new ColorInfo(C.COLOR_SPACE_BT2020, C.COLOR_RANGE_LIMITED, C.COLOR_TRANSFER_ST2084, null), null);
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
        Format hdr = hdrVideo("vp09", 3840, 2160, 30, 10000000);
        assertFalse((boolean) method.invoke(null, sdr, hdr));
        assertTrue((boolean) method.invoke(null, hdr, hdr));
    }
}
