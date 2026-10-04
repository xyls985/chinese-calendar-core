package calendar;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

/** Dependency-free test entry point, also executed by Maven during the test phase. */
public final class CalendarTest {
    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }

    private static int[] numbers(String line) {
        String[] fields = line.trim().split("\\s+");
        int[] result = new int[5];
        require(fields.length == 5, "Invalid fixture input: " + line);
        for (int i = 0; i < 5; i++) result[i] = Integer.parseInt(fields[i]);
        return result;
    }

    public static void main(String[] args) throws Exception {
        Path root = Paths.get(args.length == 0 ? ".." : args[0]);
        int count = 0;
        // Formatting must not depend on the process's default locale.
        Locale.setDefault(Locale.forLanguageTag("ar-EG"));
        for (String line : Files.readAllLines(root.resolve("tests/fixtures/readings.txt"), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] fields = line.split("\t", 2);
            int[] input = numbers(fields[0]);
            String actual = ChineseCalendar.formatCalendar(input[0], input[1], input[2], input[3], input[4]);
            require(actual.equals(fields[1]), "Regression: " + fields[0] + " -> " + actual);
            count++;
        }
        require(count >= 1000, "Missing regression fixtures");
        for (String line : Files.readAllLines(root.resolve("tests/fixtures/invalid_dates.txt"), StandardCharsets.UTF_8)) {
            if (line.isEmpty() || line.startsWith("#")) continue;
            int[] input = numbers(line);
            for (int api = 0; api < 2; api++) {
                boolean rejected = false;
                try {
                    if (api == 0) ChineseCalendar.formatCalendar(input[0], input[1], input[2], input[3], input[4]);
                    else ChineseCalendar.getCalendar(input[0], input[1], input[2], input[3], input[4]);
                } catch (IllegalArgumentException expected) { rejected = true; }
                require(rejected, "Accepted invalid input: " + line);
            }
        }
        ChineseCalendar.CalendarResult result = ChineseCalendar.getCalendar(2024, 2, 10, 8, 0);
        require(result.lunar.year == 2024 && result.lunar.month == 1 && result.lunar.day == 1, "Lunar result");
        require(!result.lunar.isLeapMonth && result.pillars.hour.equals("戊辰"), "Structured result");
        require(result.solarTerm == null, "Missing term must be null");
        require(ChineseCalendar.getLunarDate(2033, 12, 22).isLeapMonth, "2033 leap eleventh month");
        require(ChineseCalendar.getCalendar(2024, 2, 10).solar.hour == 12, "Default hour");
        require(ChineseCalendar.getCalendar(2024, 2, 4).solarTerm.time.equals("16:27"), "Solar term");
        System.out.println("Java: " + count + " regression fixtures and API checks passed");
    }
}
