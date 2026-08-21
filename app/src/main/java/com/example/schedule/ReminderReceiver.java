package com.example.schedule;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String period = intent.getStringExtra(NotificationScheduler.EXTRA_PERIOD);
        if (period == null) period = NotificationScheduler.MORNING;

        ScheduleStore store = new ScheduleStore(context);
        List<String> lines = new ArrayList<>();
        String goal = store.goalFor(YearMonth.now());
        if (!goal.isEmpty()) lines.add("이달 목표 · " + goal);

        int scheduleCount = 0;
        for (ScheduleStore.Item item : store.itemsFor(LocalDate.now())) {
            if (NotificationScheduler.EVENING.equals(period) && (!item.todo || item.completed)) continue;
            String range = ScheduleUiRules.showRange(item)
                ? " · " + item.startDate.getMonthValue() + "/" + item.startDate.getDayOfMonth()
                    + "~" + item.endDate.getMonthValue() + "/" + item.endDate.getDayOfMonth()
                : "";
            String time = item.startTime == null ? "" : item.startTime + "–" + item.endTime + " ";
            String type = item.todo ? "[할 일] " : "[일정] ";
            lines.add((item.completed ? "✓ " : "• ") + type + time + item.title + range);
            scheduleCount++;
        }
        if (scheduleCount == 0) {
            lines.add(NotificationScheduler.EVENING.equals(period) ? "오늘 할 일 완료" : "오늘 일정 없음");
        }

        NotificationScheduler.createChannel(context);
        String fullText = String.join("\n", lines);
        Notification.BigTextStyle style = new Notification.BigTextStyle().bigText(fullText);

        Intent openApp = new Intent(context, MainActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
            context,
            0,
            openApp,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        String title = NotificationScheduler.EVENING.equals(period) ? "오늘의 미완료 일정" : "오늘의 일정";
        Notification notification = new Notification.Builder(context, NotificationScheduler.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(scheduleCount == 0 ? lines.get(lines.size() - 1) : scheduleCount + "개 항목")
            .setStyle(style)
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build();
        context.getSystemService(NotificationManager.class).notify(
            NotificationScheduler.EVENING.equals(period) ? 2300 : 700,
            notification
        );

        NotificationScheduler.schedule(
            context,
            period,
            NotificationScheduler.EVENING.equals(period) ? store.eveningHour() : store.morningHour(),
            NotificationScheduler.EVENING.equals(period) ? store.eveningMinute() : store.morningMinute()
        );
    }
}
