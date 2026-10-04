import calendar.ChineseCalendar;

public final class CalendarExample {
    public static void main(String[] args) {
        ChineseCalendar.CalendarResult result = ChineseCalendar.getCalendar(2026, 10, 4, 9, 30);
        System.out.println(result.lunar.year + "-" + result.lunar.month + "-" + result.lunar.day);
        System.out.println(result.pillars.year + " " + result.pillars.month);
        System.out.println(ChineseCalendar.formatCalendar(2026, 10, 4, 9, 30));
    }
}
