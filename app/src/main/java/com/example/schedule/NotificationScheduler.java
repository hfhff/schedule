package com.example.schedule;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.time.ZonedDateTime;

final class NotificationScheduler {
    static final String CHANNEL_ID = "daily_schedule";
    static final String EXTRA_PERIOD = "period";
    static final String MORNING = "morning";
    static final String EVENING = "evening";

    private NotificationScheduler() {}

    static void createChannel(Context context) {
        NotificationChannel channel = new NotificationChannel(
            CHANNEL_ID,
            "일정 알림",
            NotificationManager.IMPORTANCE_DEFAULT
        );
        channel.setDescription("오전 일정과 오후 미완료 일정을 알려줍니다.");
        context.getSystemService(NotificationManager.class).createNotificationChannel(channel);
    }

    static void scheduleAll(Context context) {
        ScheduleStore store = new ScheduleStore(context);
        schedule(context, MORNING, store.morningHour(), store.morningMinute());
        schedule(context, EVENING, store.eveningHour(), store.eveningMinute());
    }

    static void schedule(Context context, String period, int hour, int minute) {
        AlarmManager alarms = context.getSystemService(AlarmManager.class);
        if (alarms == null) return;

        ZonedDateTime now = ZonedDateTime.now();
        ZonedDateTime next = now.withHour(hour).withMinute(minute).withSecond(0).withNano(0);
        if (!next.isAfter(now)) next = next.plusDays(1);

        Intent intent = new Intent(context, ReminderReceiver.class).putExtra(EXTRA_PERIOD, period);
        int requestCode = MORNING.equals(period) ? 700 : 2300;
        PendingIntent pending = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, next.toInstant().toEpochMilli(), pending);
    }
}
