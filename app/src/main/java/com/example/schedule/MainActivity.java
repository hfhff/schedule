package com.example.schedule;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class MainActivity extends Activity {
    private static final int PURPLE = Color.rgb(107, 93, 211);
    private static final int INK = Color.rgb(48, 45, 60);
    private static final int MUTED = Color.rgb(112, 106, 126);
    private static final int BACKGROUND = Color.rgb(247, 244, 239);
    private static final int SUNDAY = Color.rgb(210, 65, 65);
    private static final int SATURDAY = Color.rgb(52, 99, 190);
    private final DateTimeFormatter dayFormat = DateTimeFormatter.ofPattern("M월 d일 EEEE", Locale.KOREAN);
    private final DateTimeFormatter rangeFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.KOREAN);
    private final DateTimeFormatter timeFormat = DateTimeFormatter.ofPattern("HH:mm", Locale.KOREAN);

    private ScheduleStore store;
    private LocalDate selectedDate = LocalDate.now();
    private YearMonth visibleMonth = YearMonth.now();
    private TextView goalText;
    private TextView monthTitle;
    private TextView dateTitle;
    private GridLayout calendarGrid;
    private LinearLayout scheduleList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        store = new ScheduleStore(this);
        getWindow().setStatusBarColor(BACKGROUND);
        getWindow().setNavigationBarColor(BACKGROUND);
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        setContentView(buildScreen());
        NotificationScheduler.createChannel(this);
        NotificationScheduler.scheduleAll(this);
        requestNotificationPermission();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (scheduleList != null) refresh();
    }

    private View buildScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(8), dp(16), dp(10));
        root.setBackgroundColor(BACKGROUND);

        LinearLayout header = new LinearLayout(this);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.addView(text("나의 시간", 28, INK, Typeface.BOLD), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button settings = button("설정");
        settings.setOnClickListener(view -> showSettings());
        header.addView(settings);
        root.addView(header);

        goalText = text("", 16, Color.rgb(91, 64, 0), Typeface.BOLD);
        goalText.setPadding(dp(16), dp(13), dp(16), dp(13));
        goalText.setBackground(rounded(Color.rgb(255, 239, 184), 18));
        goalText.setOnClickListener(view -> editGoal());
        LinearLayout.LayoutParams goalParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        goalParams.setMargins(0, dp(10), 0, dp(6));
        root.addView(goalText, goalParams);

        LinearLayout monthHeader = new LinearLayout(this);
        monthHeader.setGravity(Gravity.CENTER_VERTICAL);
        Button previous = button("‹");
        previous.setTextSize(28);
        previous.setOnClickListener(view -> changeMonth(-1));
        monthHeader.addView(previous);
        monthTitle = text("", 20, INK, Typeface.BOLD);
        monthTitle.setGravity(Gravity.CENTER);
        monthHeader.addView(monthTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button next = button("›");
        next.setTextSize(28);
        next.setOnClickListener(view -> changeMonth(1));
        monthHeader.addView(next);
        root.addView(monthHeader);

        calendarGrid = new GridLayout(this);
        calendarGrid.setColumnCount(7);
        calendarGrid.setRowCount(7);
        root.addView(calendarGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(384)));

        LinearLayout dateHeader = new LinearLayout(this);
        dateHeader.setGravity(Gravity.CENTER_VERTICAL);
        dateTitle = text("", 20, INK, Typeface.BOLD);
        dateHeader.addView(dateTitle, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button add = button("+ 추가");
        add.setOnClickListener(view -> showScheduleDialog(null));
        dateHeader.addView(add);
        LinearLayout.LayoutParams dateParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        dateParams.setMargins(0, dp(4), 0, dp(6));
        root.addView(dateHeader, dateParams);

        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        scheduleList = new LinearLayout(this);
        scheduleList.setOrientation(LinearLayout.VERTICAL);
        scroll.addView(scheduleList, new ScrollView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        root.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1));
        return root;
    }

    private void changeMonth(int amount) {
        visibleMonth = visibleMonth.plusMonths(amount);
        selectedDate = visibleMonth.atDay(1);
        refresh();
    }

    private void refresh() {
        monthTitle.setText(getString(R.string.month_title, visibleMonth.getYear(), visibleMonth.getMonthValue()));
        String goal = store.goalFor(visibleMonth);
        goalText.setText(goal.isEmpty()
            ? visibleMonth.getMonthValue() + "월 목표를 설정하세요  +"
            : visibleMonth.getMonthValue() + "월 목표\n" + goal);
        dateTitle.setText(selectedDate.format(dayFormat));
        renderCalendar();
        renderSchedules(store.itemsFor(selectedDate));
    }

    private void renderCalendar() {
        calendarGrid.removeAllViews();
        String[] headers = {"일", "월", "화", "수", "목", "금", "토"};
        for (int column = 0; column < headers.length; column++) {
            TextView label = text(headers[column], 13, weekendColor(column, MUTED), Typeface.BOLD);
            label.setGravity(Gravity.CENTER);
            calendarGrid.addView(label, gridParams(0, column, dp(28)));
        }

        List<ScheduleStore.Item> allItems = store.allItems();
        allItems.removeIf(item -> !ScheduleUiRules.showOnCalendar(item));
        Map<Long, Integer> colors = RangeColorAssigner.assign(allItems);
        int offset = visibleMonth.atDay(1).getDayOfWeek().getValue() % 7;
        for (int position = 0; position < 42; position++) {
            int day = position - offset + 1;
            int row = position / 7 + 1;
            int column = position % 7;
            View cell = day >= 1 && day <= visibleMonth.lengthOfMonth()
                ? dayCell(visibleMonth.atDay(day), column, allItems, colors)
                : new View(this);
            calendarGrid.addView(cell, gridParams(row, column, dp(58)));
        }
    }

    private View dayCell(LocalDate date, int column, List<ScheduleStore.Item> allItems, Map<Long, Integer> colors) {
        boolean selected = date.equals(selectedDate);
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER_HORIZONTAL);
        cell.setPadding(dp(2), dp(3), dp(2), dp(2));
        cell.setBackground(roundedWithStroke(selected ? PURPLE : Color.WHITE, Color.rgb(231, 227, 220), 10));
        cell.setOnClickListener(view -> {
            selectedDate = date;
            refresh();
        });

        TextView number = text(String.valueOf(date.getDayOfMonth()), 13, selected ? Color.WHITE : weekendColor(column, INK), Typeface.BOLD);
        number.setGravity(Gravity.CENTER);
        cell.addView(number, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(22)));

        int shown = 0;
        int total = 0;
        for (ScheduleStore.Item item : allItems) {
            if (date.isBefore(item.startDate) || date.isAfter(item.endDate)) continue;
            total++;
            if (shown == 2) continue;
            int lane = colors.getOrDefault(item.id, 0);
            TextView chip = text(item.title, 9, eventTextColor(lane), Typeface.BOLD);
            chip.setSingleLine(true);
            chip.setEllipsize(TextUtils.TruncateAt.END);
            chip.setGravity(Gravity.CENTER);
            chip.setBackground(rounded(eventColor(lane), 5));
            LinearLayout.LayoutParams chipParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(15));
            chipParams.setMargins(0, dp(1), 0, 0);
            cell.addView(chip, chipParams);
            shown++;
        }
        if (total > shown) {
            TextView more = text("+" + (total - shown), 9, selected ? Color.WHITE : MUTED, Typeface.BOLD);
            more.setGravity(Gravity.CENTER);
            cell.addView(more, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(13)));
        }
        return cell;
    }

    private void renderSchedules(List<ScheduleStore.Item> items) {
        scheduleList.removeAllViews();
        Map<Long, Integer> colors = RangeColorAssigner.assign(store.allItems());
        if (items.isEmpty()) {
            TextView empty = text("이 날짜에는 일정이나 할 일이 없습니다.", 15, MUTED, Typeface.NORMAL);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(26), 0, 0);
            scheduleList.addView(empty);
            return;
        }

        for (ScheduleStore.Item item : items) {
            int lane = colors.getOrDefault(item.id, 0);
            LinearLayout card = new LinearLayout(this);
            card.setGravity(Gravity.CENTER_VERTICAL);
            card.setPadding(dp(10), dp(6), dp(6), dp(6));
            card.setBackground(rounded(eventColor(lane), 14));

            LinearLayout details = new LinearLayout(this);
            details.setOrientation(LinearLayout.VERTICAL);
            String label = "[" + (item.todo ? "할 일" : "일정") + "]  " + timeSpan(item) + item.title;
            if (item.todo) {
                CheckBox check = new CheckBox(this);
                check.setText(label);
                check.setTextSize(16);
                check.setTextColor(item.completed ? MUTED : INK);
                check.setChecked(item.completed);
                if (item.completed) check.setPaintFlags(check.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                check.setOnCheckedChangeListener((button, completed) -> {
                    store.setCompleted(item.id, completed);
                    refresh();
                });
                details.addView(check);
            } else {
                TextView schedule = text(label, 16, INK, Typeface.BOLD);
                schedule.setPadding(dp(12), dp(10), 0, dp(8));
                details.addView(schedule);
            }
            if (item.isRange()) {
                TextView period = text(item.startDate.format(rangeFormat) + " — " + item.endDate.format(rangeFormat), 12, eventTextColor(lane), Typeface.BOLD);
                period.setPadding(dp(12), 0, 0, dp(3));
                details.addView(period);
            }
            card.addView(details, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));

            Button edit = button("편집");
            edit.setOnClickListener(view -> showScheduleDialog(item));
            card.addView(edit);

            LinearLayout.LayoutParams cardParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            cardParams.setMargins(0, 0, 0, dp(8));
            scheduleList.addView(card, cardParams);
        }
    }

    private void showScheduleDialog(ScheduleStore.Item existing) {
        LocalDate[] range = existing == null
            ? new LocalDate[]{selectedDate, selectedDate}
            : new LocalDate[]{existing.startDate, existing.endDate};
        LocalTime[] times = {
            existing == null ? null : existing.startTime,
            existing == null ? null : existing.endTime
        };
        boolean[] endTimeEdited = {existing != null && existing.endTime != null};
        boolean[] todo = {existing == null || existing.todo};
        boolean[] quick = {existing != null && existing.quick};

        LinearLayout form = new LinearLayout(this);
        form.setOrientation(LinearLayout.VERTICAL);
        form.setPadding(dp(18), 0, dp(18), 0);

        RadioGroup typeGroup = new RadioGroup(this);
        typeGroup.setOrientation(RadioGroup.HORIZONTAL);
        RadioButton scheduleType = new RadioButton(this);
        scheduleType.setId(View.generateViewId());
        scheduleType.setText("일정");
        typeGroup.addView(scheduleType, new RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        RadioButton todoType = new RadioButton(this);
        todoType.setId(View.generateViewId());
        todoType.setText("할 일");
        typeGroup.addView(todoType, new RadioGroup.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        typeGroup.check(todo[0] ? todoType.getId() : scheduleType.getId());
        typeGroup.setOnCheckedChangeListener((group, checkedId) -> todo[0] = checkedId == todoType.getId());
        form.addView(typeGroup);

        EditText input = input("제목");
        if (existing != null) {
            input.setText(existing.title);
            input.setSelection(input.length());
        }
        form.addView(input);

        Button start = button("");
        Button end = button("");
        updateRangeLabels(start, end, range);

        LinearLayout timeRow = new LinearLayout(this);
        timeRow.setGravity(Gravity.CENTER_VERTICAL);
        Button startTime = button("");
        Button endTime = button("");
        updateTimeLabels(startTime, endTime, times);
        startTime.setOnClickListener(view -> {
            LocalTime initial = times[0] == null ? LocalTime.of(9, 0) : times[0];
            new TimePickerDialog(this, (picker, hour, minute) -> {
                LocalTime picked = LocalTime.of(hour, minute);
                times[0] = picked;
                times[1] = ScheduleUiRules.endTimeAfterStart(picked, times[1], endTimeEdited[0]);
                ensureValidEndDate(range, times);
                updateRangeLabels(start, end, range);
                updateTimeLabels(startTime, endTime, times);
            }, initial.getHour(), initial.getMinute(), true).show();
        });
        endTime.setOnClickListener(view -> {
            if (times[0] == null) {
                Toast.makeText(this, "시작 시간을 먼저 설정하세요.", Toast.LENGTH_SHORT).show();
                return;
            }
            LocalTime initial = times[1] == null ? times[0].plusHours(1) : times[1];
            new TimePickerDialog(this, (picker, hour, minute) -> {
                times[1] = LocalTime.of(hour, minute);
                endTimeEdited[0] = true;
                ensureValidEndDate(range, times);
                updateRangeLabels(start, end, range);
                updateTimeLabels(startTime, endTime, times);
            }, initial.getHour(), initial.getMinute(), true).show();
        });
        Button clearTime = button("시간 없음");
        clearTime.setOnClickListener(view -> {
            times[0] = null;
            times[1] = null;
            endTimeEdited[0] = false;
            updateTimeLabels(startTime, endTime, times);
        });
        timeRow.addView(startTime, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        timeRow.addView(endTime, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        timeRow.addView(clearTime);
        form.addView(timeRow);

        start.setOnClickListener(view -> pickDate(range[0], picked -> {
            quick[0] = false;
            range[0] = picked;
            if (range[1].isBefore(picked)) range[1] = picked;
            ensureValidEndDate(range, times);
            updateRangeLabels(start, end, range);
            updateTimeLabels(startTime, endTime, times);
        }));
        end.setOnClickListener(view -> pickDate(range[1], picked -> {
            boolean invalidTime = times[0] != null && times[1] != null
                && picked.equals(range[0]) && !times[1].isAfter(times[0]);
            if (picked.isBefore(range[0]) || invalidTime) {
                Toast.makeText(this, "끝 날짜와 시간은 시작보다 늦어야 합니다.", Toast.LENGTH_SHORT).show();
                return;
            }
            quick[0] = false;
            range[1] = picked;
            updateRangeLabels(start, end, range);
        }));
        form.addView(start);
        form.addView(end);

        LinearLayout quickRange = new LinearLayout(this);
        quickRange.setGravity(Gravity.CENTER_VERTICAL);
        quickRange.addView(text("빠른 기간", 13, MUTED, Typeface.BOLD),
            new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1));
        Button week = button("1주일");
        week.setOnClickListener(view -> {
            quick[0] = true;
            range[1] = range[0].plusDays(6);
            updateRangeLabels(start, end, range);
        });
        quickRange.addView(week);
        Button month = button("한 달");
        month.setOnClickListener(view -> {
            quick[0] = true;
            range[1] = range[0].plusMonths(1).minusDays(1);
            updateRangeLabels(start, end, range);
        });
        quickRange.addView(month);
        form.addView(quickRange);

        form.addView(text("요일 바로가기 · 단일 날짜", 13, MUTED, Typeface.BOLD));
        GridLayout weekdays = new GridLayout(this);
        weekdays.setColumnCount(7);
        weekdays.setRowCount(1);
        String[] weekdayLabels = {"일", "월", "화", "수", "목", "금", "토"};
        for (int column = 0; column < weekdayLabels.length; column++) {
            int weekday = column;
            Button day = button(weekdayLabels[column]);
            day.setTextColor(weekendColor(column, PURPLE));
            day.setOnClickListener(view -> {
                quick[0] = true;
                range[0] = ScheduleUiRules.nextWeekday(range[0], weekday);
                range[1] = range[0];
                ensureValidEndDate(range, times);
                updateRangeLabels(start, end, range);
            });
            weekdays.addView(day, gridParams(0, column, dp(42)));
        }
        form.addView(weekdays);

        AlertDialog.Builder dialog = new AlertDialog.Builder(this)
            .setTitle(existing == null ? "일정·할 일 추가" : "일정·할 일 편집")
            .setView(form)
            .setPositiveButton(existing == null ? "추가" : "저장", (ignored, which) -> {
                String title = input.getText().toString().trim();
                if (title.isEmpty()) return;
                if (existing == null) store.add(range[0], range[1], title, times[0], times[1], todo[0], quick[0]);
                else store.update(existing.id, range[0], range[1], title, times[0], times[1], todo[0], quick[0]);
                selectedDate = range[0];
                visibleMonth = YearMonth.from(selectedDate);
                refresh();
            })
            .setNegativeButton("취소", null);
        if (existing != null) {
            dialog.setNeutralButton("삭제", (ignored, which) -> {
                store.delete(existing.id);
                refresh();
            });
        }
        dialog.show();
    }

    private void pickDate(LocalDate initial, DateConsumer consumer) {
        new DatePickerDialog(this, (view, year, month, day) -> consumer.accept(LocalDate.of(year, month + 1, day)),
            initial.getYear(), initial.getMonthValue() - 1, initial.getDayOfMonth()).show();
    }

    private void updateRangeLabels(Button start, Button end, LocalDate[] range) {
        start.setText(getString(R.string.start_date, range[0].format(rangeFormat)));
        end.setText(getString(R.string.end_date, range[1].format(rangeFormat)));
    }

    private void updateTimeLabels(Button start, Button end, LocalTime[] times) {
        start.setText(times[0] == null ? getString(R.string.add_start_time) : getString(R.string.start_time, times[0].format(timeFormat)));
        end.setText(times[1] == null ? getString(R.string.add_end_time) : getString(R.string.end_time, times[1].format(timeFormat)));
    }

    private void ensureValidEndDate(LocalDate[] range, LocalTime[] times) {
        if (times[0] == null || times[1] == null) return;
        LocalDateTime start = range[0].atTime(times[0]);
        if (!range[1].atTime(times[1]).isAfter(start)) {
            range[1] = range[0].plusDays(1);
        }
    }

    private String timeSpan(ScheduleStore.Item item) {
        if (item.startTime == null) return "";
        if (item.endTime == null) return item.startTime.format(timeFormat) + "  ";
        return item.startTime.format(timeFormat) + "–" + item.endTime.format(timeFormat) + "  ";
    }

    private void editGoal() {
        EditText input = input("이번 달 목표");
        input.setText(store.goalFor(visibleMonth));
        input.setSelection(input.length());
        new AlertDialog.Builder(this)
            .setTitle(visibleMonth.getYear() + "년 " + visibleMonth.getMonthValue() + "월 목표")
            .setView(input)
            .setPositiveButton("저장", (dialog, which) -> {
                store.setGoal(visibleMonth, input.getText().toString());
                refresh();
            })
            .setNeutralButton("삭제", (dialog, which) -> {
                store.setGoal(visibleMonth, "");
                refresh();
            })
            .setNegativeButton("취소", null)
            .show();
    }

    private void showSettings() {
        String morning = String.format(Locale.KOREA, "알림 1  %02d:%02d", store.morningHour(), store.morningMinute());
        String evening = String.format(Locale.KOREA, "알림 2  %02d:%02d", store.eveningHour(), store.eveningMinute());
        new AlertDialog.Builder(this)
            .setTitle("알림 설정")
            .setItems(new String[]{morning, evening}, (dialog, which) -> pickTime(which == 0))
            .setNegativeButton("닫기", null)
            .show();
    }

    private void pickTime(boolean morning) {
        int hour = morning ? store.morningHour() : store.eveningHour();
        int minute = morning ? store.morningMinute() : store.eveningMinute();
        new TimePickerDialog(this, (view, pickedHour, pickedMinute) -> {
            if (morning) store.setMorningTime(pickedHour, pickedMinute);
            else store.setEveningTime(pickedHour, pickedMinute);
            NotificationScheduler.scheduleAll(this);
            Toast.makeText(this, "알림 시간을 변경했습니다.", Toast.LENGTH_SHORT).show();
        }, hour, minute, true).show();
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    private GridLayout.LayoutParams gridParams(int row, int column, int height) {
        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.height = height;
        params.columnSpec = GridLayout.spec(column, 1f);
        params.rowSpec = GridLayout.spec(row);
        params.setMargins(dp(1), dp(1), dp(1), dp(1));
        return params;
    }

    private int weekendColor(int column, int weekdayColor) {
        if (column == 0) return SUNDAY;
        if (column == 6) return SATURDAY;
        return weekdayColor;
    }

    private int eventColor(int lane) {
        return Color.HSVToColor(new float[]{(lane * 67f + 205f) % 360f, 0.24f, 0.97f});
    }

    private int eventTextColor(int lane) {
        return Color.HSVToColor(new float[]{(lane * 67f + 205f) % 360f, 0.70f, 0.48f});
    }

    private EditText input(String hint) {
        EditText input = new EditText(this);
        input.setHint(hint);
        input.setSingleLine(false);
        input.setPadding(dp(18), dp(8), dp(18), dp(8));
        return input;
    }

    private Button button(String label) {
        Button button = new Button(this);
        button.setText(label);
        button.setTextSize(13);
        button.setTextColor(PURPLE);
        button.setAllCaps(false);
        button.setMinWidth(0);
        button.setMinimumWidth(0);
        button.setMinHeight(0);
        button.setMinimumHeight(0);
        return button;
    }

    private TextView text(String value, int size, int color, int style) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        view.setTypeface(Typeface.DEFAULT, style);
        return view;
    }

    private GradientDrawable rounded(int color, int radiusDp) {
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(radiusDp));
        return background;
    }

    private GradientDrawable roundedWithStroke(int color, int stroke, int radiusDp) {
        GradientDrawable background = rounded(color, radiusDp);
        background.setStroke(dp(1), stroke);
        return background;
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }

    private interface DateConsumer {
        void accept(LocalDate date);
    }
}
