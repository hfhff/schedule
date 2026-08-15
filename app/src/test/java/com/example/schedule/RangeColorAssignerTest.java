package com.example.schedule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import org.junit.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public class RangeColorAssignerTest {
    @Test
    public void overlappingRangesGetDifferentColorsAndLaterRangesReuseThem() {
        ScheduleStore.Item trip = item(1, 1, 5);
        ScheduleStore.Item conference = item(2, 3, 4);
        ScheduleStore.Item later = item(3, 6, 8);

        Map<Long, Integer> colors = RangeColorAssigner.assign(List.of(trip, conference, later));

        assertNotEquals(colors.get(trip.id), colors.get(conference.id));
        assertEquals(colors.get(trip.id), colors.get(later.id));
    }

    private ScheduleStore.Item item(long id, int startDay, int endDay) {
        return new ScheduleStore.Item(
            id,
            LocalDate.of(2026, 8, startDay),
            LocalDate.of(2026, 8, endDay),
            "일정 " + id,
            false,
            null,
            null,
            false,
            false
        );
    }
}
