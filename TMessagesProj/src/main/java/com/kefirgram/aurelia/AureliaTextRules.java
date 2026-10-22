package com.kefirgram.aurelia;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Offline matching only. Offsets are UTF-16, like Android spans and Telegram entities. */
public final class AureliaTextRules {
    public enum Style { BOLD, UNDERLINE }

    public static final class Match {
        public final int start;
        public final int end;
        public final Style style;

        Match(int start, int end, Style style) {
            this.start = start;
            this.end = end;
            this.style = style;
        }
    }

    private static final String LEFT = "(?<![\\p{L}\\p{N}_])";
    private static final String RIGHT = "(?![\\p{L}\\p{N}_])";
    private static final String SPACE = "[\\s\\u00a0]+";
    private static final String MONTHS = "января|февраля|марта|апреля|мая|июня|июля|августа|сентября|октября|ноября|декабря";
    private static final String YEAR_SUFFIX = "(?:" + SPACE + "(?:года|году|год|гг?\\.))?";
    private static final Pattern NUMERIC = Pattern.compile(LEFT + "(\\d{1,2})([./])(\\d{1,2})\\2(\\d{4})" + RIGHT);
    private static final Pattern ISO = Pattern.compile(LEFT + "(\\d{4})-(\\d{1,2})-(\\d{1,2})" + RIGHT);
    private static final Pattern WRITTEN = Pattern.compile(LEFT + "(\\d{1,2})" + SPACE + "(" + MONTHS + ")" + SPACE + "(\\d{4})" + YEAR_SUFFIX + RIGHT, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    private static final Pattern YEAR = Pattern.compile(LEFT + "(?<!\\d[./])([12]\\d{3})" + YEAR_SUFFIX + RIGHT + "(?![./]\\d)");
    private static final Pattern PROTECTED = Pattern.compile("```[\\s\\S]*?(?:```|$)|`[^`\\n]*(?:`|$)|(?:https?://|www\\.)\\S+|[\\p{L}\\p{N}_.+-]+@[\\p{L}\\p{N}.-]+\\.[\\p{L}]{2,}|[@#][\\p{L}\\p{N}_]+", Pattern.CASE_INSENSITIVE);

    private final Pattern names;

    public AureliaTextRules(Collection<String> aliases) {
        List<String> sorted = new ArrayList<>();
        for (String alias : aliases) {
            String clean = alias.trim();
            if (!clean.isEmpty() && !sorted.contains(clean)) sorted.add(clean);
        }
        sorted.sort(Comparator.comparingInt(String::length).reversed());
        StringBuilder alternatives = new StringBuilder();
        for (String alias : sorted) {
            if (alternatives.length() > 0) alternatives.append('|');
            String[] words = alias.split(SPACE);
            for (int i = 0; i < words.length; i++) {
                if (i > 0) alternatives.append(SPACE);
                alternatives.append(Pattern.quote(words[i]));
            }
        }
        names = alternatives.length() == 0 ? null : Pattern.compile(LEFT + "(?:" + alternatives + ")" + RIGHT, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    public List<Match> find(String text) {
        List<Match> result = new ArrayList<>();
        List<int[]> protectedRanges = new ArrayList<>();
        Matcher matcher = PROTECTED.matcher(text);
        while (matcher.find()) protectedRanges.add(new int[]{matcher.start(), matcher.end()});
        List<int[]> dates = new ArrayList<>();
        for (Pattern pattern : new Pattern[]{NUMERIC, ISO, WRITTEN}) {
            matcher = pattern.matcher(text);
            while (matcher.find()) {
                // Reserve invalid dates too: 31.02.2059 must not underline just 2059.
                dates.add(new int[]{matcher.start(), matcher.end()});
                int year, month, day;
                if (pattern == NUMERIC) {
                    day = Integer.parseInt(matcher.group(1));
                    month = Integer.parseInt(matcher.group(3));
                    year = Integer.parseInt(matcher.group(4));
                } else if (pattern == ISO) {
                    year = Integer.parseInt(matcher.group(1));
                    month = Integer.parseInt(matcher.group(2));
                    day = Integer.parseInt(matcher.group(3));
                } else {
                    day = Integer.parseInt(matcher.group(1));
                    month = monthNumber(matcher.group(2));
                    year = Integer.parseInt(matcher.group(3));
                }
                if (validDate(year, month, day) && !overlaps(protectedRanges, matcher.start(), matcher.end())) {
                    result.add(new Match(matcher.start(), matcher.end(), Style.UNDERLINE));
                }
            }
        }
        matcher = YEAR.matcher(text);
        while (matcher.find()) {
            if (!overlaps(dates, matcher.start(), matcher.end()) && !overlaps(protectedRanges, matcher.start(), matcher.end())) {
                result.add(new Match(matcher.start(), matcher.end(), Style.UNDERLINE));
            }
        }
        if (names != null) {
            matcher = names.matcher(text);
            while (matcher.find()) {
                if (!overlaps(protectedRanges, matcher.start(), matcher.end())) {
                    result.add(new Match(matcher.start(), matcher.end(), Style.BOLD));
                }
            }
        }
        result.sort(Comparator.comparingInt((Match match) -> match.start).thenComparingInt(match -> match.end));
        return result;
    }

    private static int monthNumber(String month) {
        String[] months = MONTHS.split("\\|");
        for (int i = 0; i < months.length; i++) if (months[i].equalsIgnoreCase(month)) return i + 1;
        return 0;
    }

    private static boolean validDate(int year, int month, int day) {
        try {
            LocalDate.of(year, month, day);
            return year >= 1000 && year <= 2999;
        } catch (DateTimeException e) {
            return false;
        }
    }

    private static boolean overlaps(List<int[]> ranges, int start, int end) {
        for (int[] range : ranges) if (start < range[1] && end > range[0]) return true;
        return false;
    }
}
