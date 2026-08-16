package com.example.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class ScheduleUiRules {
    private ScheduleUiRules() {}

    static void sortItems(List<ScheduleStore.Item> items) {
        Comparator<ScheduleStore.Item> timedFirst = Comparator.comparing(item -> item.startTime == null);
        items.sort(timedFirst
            .thenComparing(item -> item.startTime, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(item -> item.title)
            .thenComparingLong(item -> item.id));
    }

    static LocalDate nextWeekday(LocalDate start, int sundayFirstColumn) {
        int target = sundayFirstColumn == 0 ? 7 : sundayFirstColumn;
        return start.plusDays((target - start.getDayOfWeek().getValue() + 7) % 7);
    }

    static boolean showOnCalendar(ScheduleStore.Item item) {
        return !item.todo && !item.quick;
    }

    static LocalTime endTimeAfterStart(LocalTime start, LocalTime currentEnd, boolean endWasEdited) {
        return endWasEdited && currentEnd != null ? currentEnd : start.plusHours(1);
    }

    static boolean showRange(ScheduleStore.Item item) {
        return item.isRange() && !(item.todo && item.quick);
    }

    static int colorIndex(ScheduleStore.Item item) {
        return (int) Math.floorMod(item.id, 360); // ponytail: 360 stable hues; store custom colors only if this ceiling is reached.
    }

    static List<CalendarSegment> calendarSegments(ScheduleStore.Item item, YearMonth month) {
        LocalDate start = item.startDate.isBefore(month.atDay(1)) ? month.atDay(1) : item.startDate;
        LocalDate end = item.endDate.isAfter(month.atEndOfMonth()) ? month.atEndOfMonth() : item.endDate;
        List<CalendarSegment> result = new ArrayList<>();
        if (start.isAfter(end)) return result;

        int offset = month.atDay(1).getDayOfWeek().getValue() % 7;
        int position = offset + start.getDayOfMonth() - 1;
        int endPosition = offset + end.getDayOfMonth() - 1;
        while (position <= endPosition) {
            int column = position % 7;
            int span = Math.min(7 - column, endPosition - position + 1);
            result.add(new CalendarSegment(position / 7 + 1, column, span));
            position += span;
        }
        return result;
    }

    static final class CalendarSegment {
        final int row;
        final int column;
        final int span;

        CalendarSegment(int row, int column, int span) {
            this.row = row;
            this.column = column;
            this.span = span;
        }
    }
}
