package com.example.schedule;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

final class RangeColorAssigner {
    private RangeColorAssigner() {}

    static Map<Long, Integer> assign(List<ScheduleStore.Item> source) {
        List<ScheduleStore.Item> items = new ArrayList<>(source);
        items.sort(Comparator
            .comparing((ScheduleStore.Item item) -> item.startDate)
            .thenComparing(item -> item.endDate)
            .thenComparingLong(item -> item.id));

        List<LocalDate> laneEnds = new ArrayList<>();
        Map<Long, Integer> colors = new HashMap<>();
        for (ScheduleStore.Item item : items) {
            int lane = 0;
            while (lane < laneEnds.size() && !laneEnds.get(lane).isBefore(item.startDate)) lane++;
            if (lane == laneEnds.size()) laneEnds.add(item.endDate);
            else laneEnds.set(lane, item.endDate);
            colors.put(item.id, lane);
        }
        return colors;
    }
}
