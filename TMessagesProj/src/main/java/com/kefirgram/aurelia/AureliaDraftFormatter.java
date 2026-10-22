package com.kefirgram.aurelia;

import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.URLSpan;

import org.telegram.messenger.MediaDataController;
import org.telegram.ui.Components.TextStyleSpan;

/** Uses native Telegram spans, so formatting survives sending and editing. */
public final class AureliaDraftFormatter {
    private AureliaDraftFormatter() { }

    public static SpannableStringBuilder format(CharSequence source) {
        SpannableStringBuilder result = new SpannableStringBuilder(source);
        for (AureliaTextRules.Match match : AureliaConfig.rules().find(source.toString())) {
            if (isProtected(result, match.start, match.end)) continue;
            TextStyleSpan.TextStyleRun run = new TextStyleSpan.TextStyleRun();
            run.flags = match.style == AureliaTextRules.Style.BOLD
                    ? TextStyleSpan.FLAG_STYLE_BOLD : TextStyleSpan.FLAG_STYLE_UNDERLINE;
            MediaDataController.addStyleToText(new TextStyleSpan(run), match.start, match.end, result, true);
        }
        return result;
    }

    private static boolean isProtected(Spanned text, int start, int end) {
        if (text.getSpans(start, end, URLSpan.class).length > 0) return true;
        for (TextStyleSpan span : text.getSpans(start, end, TextStyleSpan.class)) {
            if ((span.getStyleFlags() & TextStyleSpan.FLAG_STYLE_MONO) != 0) return true;
        }
        return false;
    }
}
