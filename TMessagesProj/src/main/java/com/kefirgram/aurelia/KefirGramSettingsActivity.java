package com.kefirgram.aurelia;

import android.content.Context;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import org.telegram.messenger.AndroidUtilities;
import org.telegram.messenger.LocaleController;
import org.telegram.messenger.NotificationCenter;
import org.telegram.messenger.R;
import org.telegram.ui.ActionBar.ActionBar;
import org.telegram.ui.ActionBar.BaseFragment;
import org.telegram.ui.ActionBar.Theme;
import org.telegram.ui.Components.BulletinFactory;
import org.telegram.ui.Components.EditTextBoldCursor;
import org.telegram.ui.Components.LayoutHelper;

/** Local-only dictionary and appearance settings. No chat text leaves the device. */
public class KefirGramSettingsActivity extends BaseFragment {
    @Override
    public View createView(Context context) {
        actionBar.setTitle("KefirGram");
        actionBar.setBackButtonImage(R.drawable.ic_ab_back);
        actionBar.setActionBarMenuOnItemClick(new ActionBar.ActionBarMenuOnItemClick() {
            @Override
            public void onItemClick(int id) {
                if (id == -1) finishFragment();
            }
        });

        ScrollView scroll = new ScrollView(context);
        scroll.setFillViewport(true);
        scroll.setBackgroundColor(Theme.getColor(Theme.key_windowBackgroundWhite));
        LinearLayout content = new LinearLayout(context);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(AndroidUtilities.dp(20), AndroidUtilities.dp(16), AndroidUtilities.dp(20), AndroidUtilities.dp(24));
        scroll.addView(content);
        fragmentView = scroll;

        addText(context, content, R.string.KefirAppearance, 20);
        TextView theme = addText(context, content, R.string.KefirApplyTheme, 16);
        theme.setMinHeight(AndroidUtilities.dp(56));
        theme.setGravity(Gravity.CENTER_VERTICAL);
        theme.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        theme.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        theme.setOnClickListener(v -> {
            NotificationCenter.getGlobalInstance().postNotificationName(
                    NotificationCenter.needSetDayNightTheme, Theme.getTheme("KefirGram"), false, null, -1);
            finishFragment();
        });

        addText(context, content, R.string.KefirAureliaPosting, 20);
        addText(context, content, R.string.KefirPostingHelp, 15);
        addText(context, content, R.string.KefirNamesHelp, 15);
        EditTextBoldCursor names = new EditTextBoldCursor(context);
        names.setTextSize(16);
        names.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        names.setCursorColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        names.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        names.setGravity(Gravity.TOP | Gravity.START);
        names.setMinLines(6);
        names.setMaxLines(12);
        names.setHint(LocaleController.getString(R.string.KefirNamesHelp));
        names.setText(AureliaConfig.getNames());
        content.addView(names, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT, 0, 12, 0, 12));

        TextView save = addText(context, content, R.string.KefirSaveNames, 16);
        save.setGravity(Gravity.CENTER_VERTICAL);
        save.setMinHeight(AndroidUtilities.dp(56));
        save.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        save.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        save.setOnClickListener(v -> {
            AureliaConfig.setNames(names.getText().toString());
            BulletinFactory.of(this).createSuccessBulletin(LocaleController.getString(R.string.KefirNamesSaved)).show();
        });

        TextView reset = addText(context, content, R.string.KefirResetNames, 16);
        reset.setMinHeight(AndroidUtilities.dp(56));
        reset.setGravity(Gravity.CENTER_VERTICAL);
        reset.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlueText));
        reset.setBackground(Theme.createSelectorDrawable(Theme.getColor(Theme.key_listSelector)));
        reset.setOnClickListener(v -> names.setText(AureliaConfig.getDefaultNames()));
        return fragmentView;
    }

    private TextView addText(Context context, LinearLayout parent, int stringId, int size) {
        TextView text = new TextView(context);
        text.setText(LocaleController.getString(stringId));
        text.setTextSize(size);
        text.setTextColor(Theme.getColor(Theme.key_windowBackgroundWhiteBlackText));
        text.setPadding(0, AndroidUtilities.dp(8), 0, AndroidUtilities.dp(8));
        parent.addView(text, LayoutHelper.createLinear(LayoutHelper.MATCH_PARENT, LayoutHelper.WRAP_CONTENT));
        return text;
    }
}
