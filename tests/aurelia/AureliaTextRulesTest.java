import com.kefirgram.aurelia.AureliaTextRules;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public final class AureliaTextRulesTest {
    private static int checks;
    private static final AureliaTextRules RULES = new AureliaTextRules(Arrays.asList(
            "Рализм", "Рализма", "Тимофей Рализм", "Вера Крестова", "Михаил Исаев", "А.Б. Тест"));

    public static void main(String[] args) throws Exception {
        check("18 марта 2049 года Тимофей Рализм победил", "UNDERLINE:18 марта 2049 года", "BOLD:Тимофей Рализм");
        check("15.08.2050 Вера Крестова", "UNDERLINE:15.08.2050", "BOLD:Вера Крестова");
        check("2057-06-28 и 12/11/2060", "UNDERLINE:2057-06-28", "UNDERLINE:12/11/2060");
        check("В 2057-2060 годах", "UNDERLINE:2057", "UNDERLINE:2060");
        check("В 2050 году", "UNDERLINE:2050 году");
        check("В 2059. Затем 2060, потом 2061!", "UNDERLINE:2059", "UNDERLINE:2060", "UNDERLINE:2061");
        check("Рализма и РАЛИЗМ", "BOLD:Рализма", "BOLD:РАЛИЗМ");
        check("Рализмизм и предРализм и Рализм_тест");
        check("Каменный Мост и Новый Год");
        check("31.02.2059 29.02.2059 2059-02-31 31 апреля 2059 года");
        check("29.02.2060", "UNDERLINE:29.02.2060");
        check("03.47 20499 1.2059 2059.15");
        check("https://example.org/Рализм/2059 www.test.com/2059 @Рализм #Рализм");
        check("Рализм@example.org");
        check("`Рализм 2059` ```\nРализм 18 марта 2049 года\n```");
        check("```Рализм 2059");
        check("«Рализм», (2059)", "BOLD:Рализм", "UNDERLINE:2059");
        check("🧀 Тимофей\u00a0Рализм 18\u00a0марта\u00a02049 года", "BOLD:Тимофей\u00a0Рализм", "UNDERLINE:18\u00a0марта\u00a02049 года");
        check("А.Б. Тест", "BOLD:А.Б. Тест");
        if (!new AureliaTextRules(Collections.emptyList()).find("Рализм").isEmpty()) throw new AssertionError("Empty dictionary");
        checks++;

        List<String> aliases = Files.readAllLines(Paths.get("TMessagesProj/src/main/assets/aurelia-names.txt"), StandardCharsets.UTF_8)
                .stream().filter(line -> !line.startsWith("#")).collect(Collectors.toList());
        AureliaTextRules defaults = new AureliaTextRules(aliases);
        for (String alias : aliases) {
            if (alias.trim().isEmpty()) continue;
            List<AureliaTextRules.Match> matches = defaults.find(alias);
            if (matches.size() != 1 || matches.get(0).start != 0 || matches.get(0).end != alias.length()) {
                throw new AssertionError("Default alias does not match as one name: " + alias);
            }
            checks++;
        }
        String text = "🧀 18 марта 2049 года Тимофей Рализм";
        List<AureliaTextRules.Match> first = RULES.find(text);
        List<AureliaTextRules.Match> second = RULES.find(text);
        if (first.size() != second.size()) throw new AssertionError("Non-deterministic matching");
        for (int i = 0; i < first.size(); i++) {
            if (first.get(i).start != second.get(i).start || first.get(i).end != second.get(i).end) throw new AssertionError("Changed offsets");
        }
        checks++;
        System.out.println("Aurelia rules: " + checks + " checks passed");
    }

    private static void check(String text, String... expected) {
        List<String> actual = new ArrayList<>();
        for (AureliaTextRules.Match match : RULES.find(text)) {
            actual.add(match.style + ":" + text.substring(match.start, match.end));
        }
        if (!actual.equals(Arrays.asList(expected))) {
            throw new AssertionError(text + "\nExpected: " + Arrays.toString(expected) + "\nActual: " + actual);
        }
        checks++;
    }
}
