package com.liskovsoft.smartyoutubetv2.tv.ui.dialogs.other;

import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.leanback.widget.VerticalGridView;
import androidx.preference.ListPreference;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.RecyclerView.Adapter;
import com.liskovsoft.smartyoutubetv2.tv.R;
import com.liskovsoft.smartyoutubetv2.tv.ui.mod.leanback.preference.LeanbackListPreferenceDialogFragment;
import com.liskovsoft.smartyoutubetv2.tv.util.ViewUtil;

public class RadioListPreferenceDialogFragment extends LeanbackListPreferenceDialogFragment {
    private boolean mIsTransparent;

    @Override
    public Adapter<ViewHolder> onCreateAdapter() {
        return new AdapterRadio(mEntries, mEntryValues, mInitialSelection);
    }

    public static RadioListPreferenceDialogFragment newInstanceSingle(String key) {
        final Bundle args = new Bundle(1);
        args.putString(ARG_KEY, key);

        final RadioListPreferenceDialogFragment
                fragment = new RadioListPreferenceDialogFragment();
        fragment.setArguments(args);

        return fragment;
    }

    /**
     * MOD: Set focus on selected radio item
     */
    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        if (view != null) {
            if (mIsTransparent) {
                ViewUtil.enableTransparentDialog(getActivity(), view);
            }

            VerticalGridView verticalGridView = view.findViewById(android.R.id.list);
            if (verticalGridView != null) {
                verticalGridView.scrollToPosition(findSelectedPosition());
            }
        }

        return view;
    }

    public void enableTransparent(boolean enable) {
        mIsTransparent = enable;
    }

    private int findSelectedPosition() {
        if (mEntryValues == null) {
            return 0;
        }

        for (int i = 0; i < mEntryValues.length; i++) {
            if (mEntryValues[i].equals(mInitialSelection)) {
                return i;
            }
        }

        return 0;
    }

    /**
     * MOD: Don't exit from dialog after selection has been set
     */
    public class AdapterRadio extends AdapterSingle {
        public AdapterRadio(CharSequence[] entries, CharSequence[] entryValues, CharSequence selectedValue) {
            super(entries, entryValues, selectedValue);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            super.onBindViewHolder(holder, position);
            if (!ViewUtil.isPlaybackQualityDialog(getParentFragment())) {
                return;
            }

            CharSequence label = holder.getTitleView().getText();
            int detailStart = TextUtils.indexOf(label, '\n') + 1;
            if (detailStart > 0 && detailStart < label.length()) {
                SpannableString styled = new SpannableString(label);
                int color = ContextCompat.getColor(holder.itemView.getContext(), R.color.l1_text_secondary);
                styled.setSpan(new ForegroundColorSpan(color), detailStart, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                styled.setSpan(new RelativeSizeSpan(0.88f), detailStart, label.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                holder.getTitleView().setText(styled);
            }
        }

        @Override
        public void onItemClick(ViewHolder viewHolder) {
            final int index = viewHolder.getAdapterPosition();
            if (index == RecyclerView.NO_POSITION) {
                return;
            }
            final CharSequence entry = mEntryValues[index];
            final ListPreference preference = (ListPreference) getPreference();
            if (index >= 0 && preference != null) {
                String value = mEntryValues[index].toString();
                if (preference.callChangeListener(value)) {
                    preference.setValue(value);
                    mSelectedValue = entry;
                }
            }

            notifyDataSetChanged();
        }
    }
}
