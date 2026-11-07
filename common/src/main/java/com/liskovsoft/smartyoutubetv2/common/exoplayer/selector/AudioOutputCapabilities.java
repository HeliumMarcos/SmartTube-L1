package com.liskovsoft.smartyoutubetv2.common.exoplayer.selector;

import android.content.Context;
import android.media.AudioDeviceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.MediaCodecInfo;
import android.media.MediaCodecList;
import android.os.Build;

import com.google.android.exoplayer2.Format;
import com.google.android.exoplayer2.audio.AudioCapabilities;
import com.google.android.exoplayer2.util.MimeTypes;
import com.liskovsoft.sharedutils.mylogger.Log;

/**
 * Conservative snapshot of the platform's reported audio capabilities.
 * HDMI encoded output and on-device decoding are different capabilities:
 * a decoder can output PCM without providing Dolby passthrough.
 */
public final class AudioOutputCapabilities {
    private static final String TAG = "AudioOutputCapabilities";
    private final boolean ac3Output;
    private final boolean eac3Output;
    private final boolean ac3Decoder;
    private final boolean sinkAc3;
    private final boolean sinkEac3;
    private final int sinkMaxChannels;
    private final boolean eac3Decoder;

    private AudioOutputCapabilities(boolean ac3Output, boolean eac3Output,
                                    boolean ac3Decoder, boolean eac3Decoder,
                                    boolean sinkAc3, boolean sinkEac3, int sinkMaxChannels) {
        this.ac3Output = ac3Output;
        this.eac3Output = eac3Output;
        this.ac3Decoder = ac3Decoder;
        this.eac3Decoder = eac3Decoder;
        this.sinkAc3 = sinkAc3;
        this.sinkEac3 = sinkEac3;
        this.sinkMaxChannels = sinkMaxChannels;
    }

    public static AudioOutputCapabilities query(Context context) {
        boolean ac3 = false;
        boolean eac3 = false;
        AudioManager manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (manager != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            for (AudioDeviceInfo device : manager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)) {
                int type = device.getType();
                // Built-in speakers and Bluetooth do not establish encoded HDMI passthrough.
                if (type != AudioDeviceInfo.TYPE_HDMI
                        && type != AudioDeviceInfo.TYPE_HDMI_ARC
                        && (Build.VERSION.SDK_INT < 31
                        || type != AudioDeviceInfo.TYPE_HDMI_EARC)) {
                    continue;
                }
                for (int encoding : device.getEncodings()) {
                    ac3 |= encoding == AudioFormat.ENCODING_AC3;
                    eac3 |= encoding == AudioFormat.ENCODING_E_AC3
                            || (Build.VERSION.SDK_INT >= 28
                            && encoding == AudioFormat.ENCODING_E_AC3_JOC);
                }
            }
        }
        AudioCapabilities sink = AudioCapabilities.getCapabilities(context);
        AudioOutputCapabilities result = new AudioOutputCapabilities(
                ac3, eac3, hasDecoder(MimeTypes.AUDIO_AC3), hasDecoder(MimeTypes.AUDIO_E_AC3),
                sink.supportsEncoding(AudioFormat.ENCODING_AC3),
                sink.supportsEncoding(AudioFormat.ENCODING_E_AC3), sink.getMaxChannelCount());
        Log.d(TAG, "Reported HDMI encoded output: AC3=" + result.ac3Output
                + ", EAC3=" + result.eac3Output
                + "; device decoders: AC3=" + result.ac3Decoder
                + ", EAC3=" + result.eac3Decoder
                + "; ExoPlayer sink AC3=" + result.sinkAc3
                + ", EAC3=" + result.sinkEac3
                + ", max channels=" + result.sinkMaxChannels
                + ". Decoder support does not imply passthrough.");
        return result;
    }

    private static boolean hasDecoder(String mime) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return false; // MediaCodecList.getCodecInfos() requires API 21.
        }
        try {
            for (MediaCodecInfo info : new MediaCodecList(MediaCodecList.REGULAR_CODECS).getCodecInfos()) {
                if (!info.isEncoder()) {
                    for (String supported : info.getSupportedTypes()) {
                        if (mime.equalsIgnoreCase(supported)) {
                            return true;
                        }
                    }
                }
            }
        } catch (RuntimeException e) {
            Log.e(TAG, "Unable to inspect audio decoders: " + e.getMessage());
        }
        return false;
    }

    public boolean canPlay(Format format) {
        if (format == null) {
            return false;
        }
        String codec = format.codecs != null ? format.codecs.toLowerCase(java.util.Locale.US) : "";
        String mime = format.sampleMimeType != null ? format.sampleMimeType : "";
        if (mime.equals(MimeTypes.AUDIO_E_AC3) || codec.startsWith("ec-3")) {
            return eac3Decoder || (sinkEac3 && channelFits(format));
        }
        if (mime.equals(MimeTypes.AUDIO_AC3) || codec.startsWith("ac-3")) {
            return ac3Decoder || (sinkAc3 && channelFits(format));
        }
        return true; // Other formats are still filtered by ExoPlayer's track mapping.
    }

    private boolean channelFits(Format format) {
        return format.channelCount <= 0 || format.channelCount <= sinkMaxChannels;
    }

    public boolean hasEncodedOutput(Format format) {
        if (format == null) {
            return false;
        }
        String codec = format.codecs != null ? format.codecs.toLowerCase(java.util.Locale.US) : "";
        return (sinkEac3 && channelFits(format) && (MimeTypes.AUDIO_E_AC3.equals(format.sampleMimeType)
                || codec.startsWith("ec-3")))
                || (sinkAc3 && channelFits(format) && (MimeTypes.AUDIO_AC3.equals(format.sampleMimeType)
                || codec.startsWith("ac-3")));
    }
}
