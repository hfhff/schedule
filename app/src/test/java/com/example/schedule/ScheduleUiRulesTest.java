package com.example.schedule;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public class ScheduleUiRulesTest {
    @Test
    public void weekdayShortcutAndAutomaticTimeSortingWork() {
        LocalDate sunday = LocalDate.of(2026, 8, 16);
        assertEquals(LocalDate.of(2026, 8, 17), ScheduleUiRules.nextWeekday(sunday, 1));
        assertEquals(sunday, ScheduleUiRules.nextWeekday(sunday, 0));

        ScheduleStore.Item noTimeNa = item(1, "나", null);
        ScheduleStore.Item late = item(2, "늦은 일정", LocalTime.of(15, 0));
        ScheduleStore.Item noTimeGa = item(3, "가", null);
        ScheduleStore.Item early = item(4, "이른 일정", LocalTime.of(8, 30));
        List<ScheduleStore.Item> items = new ArrayList<>(List.of(noTimeNa, late, noTimeGa, early));
        ScheduleUiRules.sortItems(items);
        assertEquals(List.of(early, late, noTimeGa, noTimeNa), items);

        LocalTime start = LocalTime.of(10, 0);
        assertEquals(LocalTime.of(11, 0), ScheduleUiRules.endTimeAfterStart(start, LocalTime.of(12, 0), false));
        assertEquals(LocalTime.of(12, 0), ScheduleUiRules.endTimeAfterStart(start, LocalTime.of(12, 0), true));
    }

    @Test
    public void calendarOnlyShowsNonQuickSchedules() {
        LocalDate start = LocalDate.of(2026, 8, 16);
        assertEquals(false, ScheduleUiRules.showOnCalendar(new ScheduleStore.Item(1, start, start, "할 일", false, null, null, true, false)));
        assertEquals(false, ScheduleUiRules.showOnCalendar(new ScheduleStore.Item(2, start, start.plusDays(6), "빠른 일정", false, null, null, false, true)));
        assertEquals(true, ScheduleUiRules.showOnCalendar(new ScheduleStore.Item(3, start, start, "당일 일정", false, null, null, false, false)));

        ScheduleStore.Item trip = new ScheduleStore.Item(4, LocalDate.of(2026, 8, 15), LocalDate.of(2026, 8, 18), "여행", false, null, null, false, false);
        List<ScheduleUiRules.CalendarSegment> segments = ScheduleUiRules.calendarSegments(trip, YearMonth.of(2026, 8));
        assertEquals(2, segments.size());
        assertEquals(1, segments.get(0).span);
        assertEquals(3, segments.get(1).span);

        ScheduleStore.Item quickTodo = new ScheduleStore.Item(5, start, start.plusDays(6), "운동", false, null, null, true, true);
        assertEquals(false, ScheduleUiRules.showRange(quickTodo));
        assertEquals(ScheduleUiRules.colorIndex(trip), ScheduleUiRules.colorIndex(trip));
        assertEquals(false, ScheduleUiRules.colorIndex(trip) == ScheduleUiRules.colorIndex(quickTodo));

        quickTodo.completedDates.add(start);
        assertEquals(true, quickTodo.completedFor(start));
        assertEquals(false, quickTodo.completedFor(start.plusDays(1)));

        ScheduleStore.Item quickSingleDay = new ScheduleStore.Item(6, start, start, "오늘만", false, null, null, true, true);
        quickSingleDay.completedDates.add(start);
        assertEquals(true, quickSingleDay.completedFor(start));
        assertEquals(false, quickSingleDay.completedFor(start.plusDays(1)));
    }

    private ScheduleStore.Item item(long id, String title, LocalTime time) {
        LocalDate date = LocalDate.of(2026, 8, 16);
        return new ScheduleStore.Item(id, date, date, title, false, time, time == null ? null : time.plusHours(1), true, false);
    }
}
