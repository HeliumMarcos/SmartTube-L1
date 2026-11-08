package com.liskovsoft.smartyoutubetv2.common.exoplayer.selector;

import android.content.Context;

import com.google.android.exoplayer2.Format;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.smartyoutubetv2.common.R;

public class TrackInfoFormatter2 {
    private final String mVideoLabel;
    private final String mAudioLabel;
    private final String mUnknownChannels;
    private Format mVideoFormat;
    private Format mAudioFormat;
    private String mSpeed;
    private boolean mEnableBitrate;

    public TrackInfoFormatter2(Context context) {
        mVideoLabel = context.getString(R.string.settings_video);
        mAudioLabel = context.getString(R.string.playback_audio_label);
        mUnknownChannels = context.getString(R.string.playback_channels_unknown);
    }

    public void setVideoFormat(Format format) {
        mVideoFormat = format;
    }

    public void setAudioFormat(Format format) {
        mAudioFormat = format;
    }

    public void setSpeed(float speed) {
        mSpeed = speed != 1.0f ? Helpers.formatFloat(speed) + "×" : "";
    }

    public String getQualityLabel() {
        String video = buildVideoDetails(mVideoFormat, mEnableBitrate);
        String audio = buildAudioDetails(mAudioFormat, mUnknownChannels, mEnableBitrate);
        String videoLine = video.isEmpty() ? "" : join(mVideoLabel, video, mSpeed);
        String audioLine = audio.isEmpty() ? "" : join(mAudioLabel, audio, videoLine.isEmpty() ? mSpeed : "");
        return videoLine.isEmpty() || audioLine.isEmpty() ? videoLine + audioLine : videoLine + "\n" + audioLine;
    }

    public static String buildVideoDetails(Format format, boolean showBitrate) {
        if (format == null) {
            return "";
        }
        String fps = format.frameRate > 0 ? Helpers.formatFloat(Helpers.round(format.frameRate, 2)) + " fps" : "";
        String bitrate = showBitrate ? TrackSelectorUtil.extractBitrate(format, 2) : "";
        return join(TrackSelectorUtil.getShortResolutionLabel(format),
                TrackSelectorUtil.buildVideoHdrLabel(format), fps, TrackSelectorUtil.buildVideoCodecLabel(format),
                bitrate.isEmpty() ? "" : bitrate + " Mbps", TrackSelectorUtil.buildHighBitrateMark(format));
    }

    public static String buildAudioDetails(Format format, String unknownChannels, boolean showBitrate) {
        if (format == null) {
            return "";
        }
        return join(TrackSelectorUtil.buildAudioCodecLabel(format, false),
                format.channelCount > 0 ? TrackSelectorUtil.buildChannels(format, true) : unknownChannels,
                showBitrate && format.bitrate > 0 ? Math.round(format.bitrate / 1000f) + " kbps" : "",
                TrackSelectorUtil.buildDrcMark(format));
    }

    private static String join(String... items) {
        StringBuilder result = new StringBuilder();
        for (String item : items) {
            if (item == null || item.isEmpty()) {
                continue;
            }
            if (result.length() > 0) {
                result.append(" · ");
            }
            result.append(item);
        }
        return result.toString();
    }

    public void enableBitrate(boolean enable) {
        mEnableBitrate = enable;
    }
}
