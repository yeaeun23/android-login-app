package kr.co.seoul;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class MyFirebaseMessagingService extends FirebaseMessagingService {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        String channelId = "channel_id";
        String channelName = "channel_name";
        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            notifManager.createNotificationChannel(new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_HIGH));
        }

        Intent notificationIntent = new Intent(getApplicationContext(), MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), channelId);
        builder.setContentTitle(remoteMessage.getData().get("title"));  // 제목(필수)
        builder.setContentText(remoteMessage.getData().get("body"));    // 내용(필수)
        builder.setDefaults(Notification.DEFAULT_ALL);                  // 소리, 진동 설정
        builder.setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION));
        builder.setSmallIcon(R.drawable.icon);                          // 아이콘 설정
        builder.setAutoCancel(true);                                    // 터치 시 반응 후 지우기
        builder.setContentIntent(PendingIntent.getActivity(getApplicationContext(), (int) System.currentTimeMillis(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT));

//        // 마시멜로(Android Ver 6.0.1) 이하일 때
//        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
//            builder.setColor(getResources().getColor(R.color.colorLogin));
//            builder.setSmallIcon(R.drawable.sis_icon);
//        }

        notifManager.notify(0, builder.build());
    }
}
