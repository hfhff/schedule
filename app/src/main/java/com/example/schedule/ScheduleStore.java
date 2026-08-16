package com.example.schedule;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

final class ScheduleStore {
    private static final String PREFS = "schedule_data";
    private static final String ITEMS = "items";
    private static final String GOALS = "goals";
    private final SharedPreferences preferences;

    ScheduleStore(Context context) {
        preferences = context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    synchronized List<Item> itemsFor(LocalDate date) {
        List<Item> result = new ArrayList<>();
        for (Item item : allItems()) {
            if (!date.isBefore(item.startDate) && !date.isAfter(item.endDate)) result.add(item);
        }
        ScheduleUiRules.sortItems(result);
        return result;
    }

    synchronized List<Item> allItems() {
        List<Item> items = new ArrayList<>();
        try {
            JSONArray array = new JSONArray(preferences.getString(ITEMS, "[]"));
            for (int i = 0; i < array.length(); i++) {
                JSONObject value = array.getJSONObject(i);
                String legacyDate = value.optString("date", LocalDate.now().toString());
                LocalDate start = LocalDate.parse(value.optString("startDate", legacyDate));
                LocalDate end = LocalDate.parse(value.optString("endDate", start.toString()));
                String storedStartTime = value.optString("startTime", value.optString("time"));
                LocalTime startTime = storedStartTime.isEmpty() ? null : LocalTime.parse(storedStartTime);
                String storedEndTime = value.optString("endTime");
                LocalTime endTime = storedEndTime.isEmpty() ? (startTime == null ? null : startTime.plusHours(1)) : LocalTime.parse(storedEndTime);
                boolean quick = value.optBoolean("quick");
                boolean todo = value.has("todo") ? value.optBoolean("todo") : quick || start.equals(end);
                items.add(new Item(
                    value.getLong("id"),
                    start,
                    end,
                    value.getString("title"),
                    value.optBoolean("completed"),
                    startTime,
                    endTime,
                    todo,
                    quick
                ));
            }
        } catch (JSONException | RuntimeException ignored) {
            // A corrupt local file is treated as empty; there is intentionally no cloud backup.
        }
        return items;
    }

    synchronized void add(LocalDate start, LocalDate end, String title, LocalTime startTime, LocalTime endTime, boolean todo, boolean quick) {
        List<Item> items = allItems();
        long id = preferences.getLong("next_id", 1L);
        items.add(new Item(id, start, end, title.trim(), false, startTime, endTime, todo, quick));
        saveItems(items);
        preferences.edit().putLong("next_id", id + 1).apply();
    }

    synchronized void update(long id, LocalDate start, LocalDate end, String title, LocalTime startTime, LocalTime endTime, boolean todo, boolean quick) {
        List<Item> items = allItems();
        for (Item item : items) {
            if (item.id == id) {
                item.startDate = start;
                item.endDate = end;
                item.title = title.trim();
                item.startTime = startTime;
                item.endTime = endTime;
                item.todo = todo;
                item.quick = quick;
            }
        }
        saveItems(items);
    }

    synchronized void setCompleted(long id, boolean completed) {
        List<Item> items = allItems();
        for (Item item : items) if (item.id == id) item.completed = completed;
        saveItems(items);
    }

    synchronized void delete(long id) {
        List<Item> items = allItems();
        items.removeIf(item -> item.id == id);
        saveItems(items);
    }

    synchronized String goalFor(YearMonth month) {
        try {
            return new JSONObject(preferences.getString(GOALS, "{}")).optString(month.toString(), "");
        } catch (JSONException ignored) {
            return "";
        }
    }

    synchronized void setGoal(YearMonth month, String goal) {
        try {
            JSONObject goals = new JSONObject(preferences.getString(GOALS, "{}"));
            if (goal.trim().isEmpty()) goals.remove(month.toString());
            else goals.put(month.toString(), goal.trim());
            preferences.edit().putString(GOALS, goals.toString()).apply();
        } catch (JSONException ignored) {
            preferences.edit().putString(GOALS, "{}").apply();
        }
    }

    int morningHour() { return preferences.getInt("morning_hour", 7); }
    int morningMinute() { return preferences.getInt("morning_minute", 0); }
    int eveningHour() { return preferences.getInt("evening_hour", 23); }
    int eveningMinute() { return preferences.getInt("evening_minute", 0); }

    void setMorningTime(int hour, int minute) {
        preferences.edit().putInt("morning_hour", hour).putInt("morning_minute", minute).apply();
    }

    void setEveningTime(int hour, int minute) {
        preferences.edit().putInt("evening_hour", hour).putInt("evening_minute", minute).apply();
    }

    private void saveItems(List<Item> items) {
        JSONArray array = new JSONArray();
        for (Item item : items) {
            JSONObject value = new JSONObject();
            try {
                value.put("id", item.id);
                value.put("startDate", item.startDate.toString());
                value.put("endDate", item.endDate.toString());
                value.put("title", item.title);
                value.put("completed", item.completed);
                if (item.startTime != null) value.put("startTime", item.startTime.toString());
                if (item.endTime != null) value.put("endTime", item.endTime.toString());
                value.put("todo", item.todo);
                value.put("quick", item.quick);
                array.put(value);
            } catch (JSONException ignored) {
                return;
            }
        }
        preferences.edit().putString(ITEMS, array.toString()).apply();
    }

    static final class Item {
        final long id;
        LocalDate startDate;
        LocalDate endDate;
        String title;
        boolean completed;
        LocalTime startTime;
        LocalTime endTime;
        boolean todo;
        boolean quick;

        Item(long id, LocalDate startDate, LocalDate endDate, String title, boolean completed, LocalTime startTime, LocalTime endTime, boolean todo, boolean quick) {
            this.id = id;
            this.startDate = startDate;
            this.endDate = endDate;
            this.title = title;
            this.completed = completed;
            this.startTime = startTime;
            this.endTime = endTime;
            this.todo = todo;
            this.quick = quick;
        }

        boolean isRange() {
            return !startDate.equals(endDate);
        }
    }
}
