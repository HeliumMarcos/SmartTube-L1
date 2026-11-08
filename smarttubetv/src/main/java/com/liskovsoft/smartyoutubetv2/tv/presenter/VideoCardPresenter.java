package com.liskovsoft.smartyoutubetv2.tv.presenter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.graphics.Color;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.os.Build.VERSION;
import android.util.Pair;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.Presenter;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.liskovsoft.sharedutils.helpers.Helpers;
import com.liskovsoft.sharedutils.mylogger.Log;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.Video;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoQualityCache;
import com.liskovsoft.smartyoutubetv2.common.app.models.data.VideoQualityProbe;
import android.os.Handler;
import android.os.Looper;
import java.util.WeakHashMap;
import com.liskovsoft.smartyoutubetv2.common.prefs.MainUIData;
import com.liskovsoft.smartyoutubetv2.common.utils.ClickbaitRemover;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.presenter.base.LongClickPresenter;
import com.liskovsoft.smartyoutubetv2.tv.ui.browse.video.GridFragmentHelper;
import com.liskovsoft.smartyoutubetv2.tv.ui.widgets.complexcardview.ComplexImageCardView;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

/*
 * A CardPresenter is used to generate Views and bind Objects to them on demand.
 * It contains an Image CardView
 */
public class VideoCardPresenter extends LongClickPresenter {
    private static final String TAG = VideoCardPresenter.class.getSimpleName();
    private static final Pattern QUALITY_PREFIX = Pattern.compile("^(?:\\s*)(4K|8K|1080p|1440p)(?=\\s|[·•|]|$)", Pattern.CASE_INSENSITIVE);
    private int mDefaultBackgroundColor = -1;
    private int mDefaultTextColor = -1;
    private int mSelectedBackgroundColor = -1;
    private int mSelectedTextColor = -1;
    private int mCardPreviewType;
    private final WeakHashMap<ComplexImageCardView, String> mBoundVideos = new WeakHashMap<>();
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private int mThumbQuality;
    private int mWidth;
    private int mHeight;

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent) {
        Context context = parent.getContext();

        mDefaultBackgroundColor =
            ContextCompat.getColor(context, Helpers.getThemeAttr(context, R.attr.cardDefaultBackground));
        mDefaultTextColor =
                ContextCompat.getColor(context, R.color.card_default_text);
        mSelectedBackgroundColor =
                ContextCompat.getColor(context, Helpers.getThemeAttr(context, R.attr.cardSelectedBackground));
        mSelectedTextColor =
                ContextCompat.getColor(context, R.color.card_selected_text_grey);

        mCardPreviewType = getCardPreviewType(context);
        mThumbQuality = getThumbQuality(context);

        boolean isCardMultilineTitleEnabled = isCardMultilineTitleEnabled(context);
        boolean isCardMultilineSubtitleEnabled = isCardMultilineSubtitleEnabled(context);
        boolean isCardTextAutoScrollEnabled = isCardTextAutoScrollEnabled(context);
        float cardTextScrollSpeed = getCardTextScrollSpeed(context);

        updateDimensions(context);

        ComplexImageCardView cardView = new ComplexImageCardView(context) {
            @Override
            public void setSelected(boolean selected) {
                updateCardBackgroundColor(this, selected);
                super.setSelected(selected);
                if (android.os.Build.VERSION.SDK_INT >= 23) {
                    View thumbnail = findViewById(R.id.main_image_wrapper);
                    if (thumbnail != null) {
                        thumbnail.setForeground(selected ? ContextCompat.getDrawable(getContext(), R.drawable.lb_card_outline) : null);
                    }
                }
            }
        };

        cardView.setTitleLinesNum(2); // TV-style titles below thumbnails
        cardView.setContentLinesNum(isCardMultilineSubtitleEnabled ? 2 : 1);
        cardView.enableTextAutoScroll(isCardTextAutoScrollEnabled);
        cardView.setTextScrollSpeed(cardTextScrollSpeed);
        cardView.setFocusable(true);
        cardView.setFocusableInTouchMode(true);
        cardView.enableBadge(isBadgeEnabled());
        cardView.enableTitle(isTitleEnabled());
        cardView.enableContent(isContentEnabled());
        cardView.setBackgroundColor(android.graphics.Color.TRANSPARENT); // Separate thumbnail and metadata visually
        //if (VERSION.SDK_INT >= 23 && MainUIData.instance(context).isUiTweakEnabled(MainUIData.UI_TWEAK_ROUNDED_CORNERS)) {
        //    cardView.setForeground(ContextCompat.getDrawable(context, R.drawable.lb_card_outline));
        //}
        updateCardBackgroundColor(cardView, false);
        return new ViewHolder(cardView);
    }

    private void updateCardBackgroundColor(ComplexImageCardView view, boolean selected) {
        int backgroundColor = android.graphics.Color.TRANSPARENT; // No solid info card behind metadata
        int textColor = mDefaultTextColor; // Keep metadata readable on both focus states

        // Both background colors should be set because the view's
        // background is temporarily visible during animations.
        // NOTE: has visual bug with rounded corners
        //view.setBackgroundColor(backgroundColor);

        View infoField = view.findViewById(R.id.info_field);
        if (infoField != null) {
            infoField.setBackgroundColor(backgroundColor);
        }

        TextView titleText = view.findViewById(R.id.title_text);
        if (titleText != null) {
            titleText.setTextColor(textColor);
        }
        TextView contentText = view.findViewById(R.id.content_text);
        if (contentText != null) {
            contentText.setTextColor(textColor);
        }
    }

    @Override
    public void onBindViewHolder(Presenter.ViewHolder viewHolder, Object item) {
        super.onBindViewHolder(viewHolder, item);

        Video video = (Video) item;

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;
        Context context = cardView.getContext();

        cardView.setTitleText(video.getTitle());
        mBoundVideos.put(cardView, video.videoId);
        Integer cachedQuality = VideoQualityCache.get(video.videoId);
        cardView.setContentText(qualitySubtitle(video.getSecondTitle(), cachedQuality));
        if (cachedQuality == null && video.videoId != null && !video.isLive && !video.isUpcoming) {
            final String boundId = video.videoId;
            final java.lang.ref.WeakReference<ComplexImageCardView> weakCard = new java.lang.ref.WeakReference<>(cardView);
            final CharSequence subtitle = video.getSecondTitle();
            VideoQualityProbe.request(boundId, (id, tier) -> mMainHandler.post(() -> {
                ComplexImageCardView current = weakCard.get();
                if (current != null && boundId.equals(mBoundVideos.get(current))) {
                    current.setContentText(qualitySubtitle(subtitle, tier));
                }
            }));
        }
        // Count progress that very close to zero. E.g. when user closed video immediately.
        cardView.setProgress(video.percentWatched > 0 && video.percentWatched < 1 ? 1 : Math.round(video.percentWatched));
        cardView.setBadgeText(
                video.hasNewContent ? context.getString(R.string.badge_new_content) :
                video.isLive ? context.getString(R.string.badge_live) :
                video.isShorts ? context.getString(R.string.header_shorts).toUpperCase() :
                video.badge
        );
        cardView.setBadgeColor(video.hasNewContent || video.isLive || video.isUpcoming ?
                ContextCompat.getColor(context, R.color.dark_red) : ContextCompat.getColor(context, R.color.black));

        if (mCardPreviewType != MainUIData.CARD_PREVIEW_DISABLED) {
            cardView.setPreview(video);
            cardView.setMute(mCardPreviewType == MainUIData.CARD_PREVIEW_MUTED);
        }

        cardView.setMainImageDimensions(mWidth, mHeight);

        if (context instanceof Activity && ((Activity) context).isDestroyed()) {
            // Glide.with(context): IllegalArgumentException: You cannot start a load for a destroyed activity
            return;
        }

        Glide.with(context)
                //.asBitmap() // disable animation (webp, gif)
                .load(ClickbaitRemover.updateThumbnail(video, mThumbQuality))
                //.placeholder(mDefaultCardImage)
                .apply(ViewUtil.glideOptions())
                // improve image compression on low end devices
                .override(mWidth, mHeight)
                // com.liskovsoft.smartyoutubetv2.tv.util.CacheGlideModule
                // Cache makes app crashing on old android versions
                .diskCacheStrategy(VERSION.SDK_INT > 21 ? DiskCacheStrategy.ALL : DiskCacheStrategy.NONE)
                .listener(mErrorListener)
                .error(
                    // Updated thumbnail url not found
                    Glide.with(context)
                        .load(video.cardImageUrl) // always working
                        //.placeholder(mDefaultCardImage)
                        .apply(ViewUtil.glideOptions())
                        .listener(mErrorListener)
                        .error(R.drawable.card_placeholder) // R.color.lb_grey
                )
                .into(cardView.getMainImageView());
    }

    private CharSequence qualitySubtitle(CharSequence subtitle, Integer tier) {
        if (tier == null || tier < 4) return subtitle;
        String prefix = tier >= 8 ? "8K  " : "4K  ";
        CharSequence original = subtitle == null ? "" : subtitle;
        SpannableStringBuilder styled = new SpannableStringBuilder(prefix).append(original);
        styled.setSpan(new BackgroundColorSpan(0xFF555B67), 0, 2, 0);
        styled.setSpan(new ForegroundColorSpan(Color.WHITE), 0, 2, 0);
        styled.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, 2, 0);
        return styled;
    }

    /** Highlight only quality metadata actually provided by the video service. */
    private CharSequence formatQualityMetadata(CharSequence subtitle) {
        if (subtitle == null) return null;
        Matcher matcher = QUALITY_PREFIX.matcher(subtitle);
        if (!matcher.find()) return subtitle;
        SpannableStringBuilder styled = new SpannableStringBuilder(subtitle);
        int start = matcher.start(1);
        int end = matcher.end(1);
        styled.setSpan(new BackgroundColorSpan(0xFF555B67), start, end, 0);
        styled.setSpan(new ForegroundColorSpan(Color.WHITE), start, end, 0);
        styled.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), start, end, 0);
        return styled;
    }

    @Override
    public void onUnbindViewHolder(Presenter.ViewHolder viewHolder) {
        super.onUnbindViewHolder(viewHolder);

        ComplexImageCardView cardView = (ComplexImageCardView) viewHolder.view;
        mBoundVideos.remove(cardView);

        // Remove references to images so that the garbage collector can free up memory.
        cardView.setBadgeImage(null);
        cardView.setMainImage(null);

        // Cleanup Glide resources. https://chatgpt.com/share/682120c5-e428-8010-b848-371b2dec0cd5
        Glide.with(cardView.getContext().getApplicationContext()).clear(cardView.getMainImageView());
    }

    private void updateDimensions(Context context) {
        Pair<Integer, Integer> dimens = getCardDimensPx(context);

        mWidth = dimens.first;
        mHeight = dimens.second;
    }
    
    protected Pair<Integer, Integer> getCardDimensPx(Context context) {
        return GridFragmentHelper.getCardDimensPx(context, R.dimen.card_width, R.dimen.card_height, MainUIData.instance(context).getVideoGridScale());
    }

    protected boolean isCardTextAutoScrollEnabled(Context context) {
        return MainUIData.instance(context).isCardTextAutoScrollEnabled();
    }

    protected int getCardPreviewType(Context context) {
        return MainUIData.instance(context).getCardPreviewType();
    }

    protected boolean isCardMultilineTitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineTitleEnabled();
    }

    protected boolean isCardMultilineSubtitleEnabled(Context context) {
        return MainUIData.instance(context).isCardMultilineSubtitleEnabled();
    }

    protected float getCardTextScrollSpeed(Context context) {
        return MainUIData.instance(context).getCardTextScrollSpeed();
    }

    protected int getThumbQuality(Context context) {
        return MainUIData.instance(context).getThumbQuality();
    }

    protected boolean isContentEnabled() {
        return true;
    }

    protected boolean isTitleEnabled() {
        return true;
    }

    protected boolean isBadgeEnabled() {
        return true;
    }

    private final RequestListener<Drawable> mErrorListener = new RequestListener<Drawable>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Drawable> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Drawable resource, Object model, Target<Drawable> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };

    private final RequestListener<Bitmap> mErrorListener2 = new RequestListener<Bitmap>() {
        @Override
        public boolean onLoadFailed(@Nullable GlideException e, Object model, Target<Bitmap> target, boolean isFirstResource) {
            Log.e(TAG, "Glide load failed: " + e);
            return false;
        }

        @Override
        public boolean onResourceReady(Bitmap resource, Object model, Target<Bitmap> target, DataSource dataSource, boolean isFirstResource) {
            return false;
        }
    };
}
