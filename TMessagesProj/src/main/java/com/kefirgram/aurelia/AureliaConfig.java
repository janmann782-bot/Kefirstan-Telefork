package com.kefirgram.aurelia;

import android.content.Context;
import android.content.SharedPreferences;

import org.telegram.messenger.ApplicationLoader;
import org.telegram.messenger.FileLog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public final class AureliaConfig {
    public static final int DRAWER_SETTINGS = 21062;
    private static String defaultNames;
    private static String cachedNames;
    private static AureliaTextRules cachedRules;

    private AureliaConfig() { }

    private static SharedPreferences preferences() {
        return ApplicationLoader.applicationContext.getSharedPreferences("kefirgram", Context.MODE_PRIVATE);
    }

    public static String getDefaultNames() {
        if (defaultNames == null) {
            StringBuilder names = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    ApplicationLoader.applicationContext.getAssets().open("aurelia-names.txt"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.trim().isEmpty() && !line.startsWith("#")) names.append(line.trim()).append('\n');
                }
            } catch (Exception e) {
                FileLog.e(e);
            }
            defaultNames = names.toString().trim();
        }
        return defaultNames;
    }

    public static String getNames() {
        return preferences().getString("aureliaNames", getDefaultNames());
    }

    public static void setNames(String names) {
        preferences().edit().putString("aureliaNames", names.trim()).apply();
    }

    public static synchronized AureliaTextRules rules() {
        String names = getNames();
        if (cachedRules == null || !names.equals(cachedNames)) {
            cachedRules = new AureliaTextRules(Arrays.asList(names.split("\\r?\\n")));
            cachedNames = names;
        }
        return cachedRules;
    }
}
