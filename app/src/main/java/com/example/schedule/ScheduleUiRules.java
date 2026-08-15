package com.example.schedule;

import java.time.LocalDate;
import java.time.LocalTime;
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
}
