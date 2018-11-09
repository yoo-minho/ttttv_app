package com.uminoh.bulnati;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.TcpUtil.ReceiverThread;
import com.uminoh.bulnati.TcpUtil.SenderThread;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MyChatService extends Service implements ReceiverThread.OnReceiveListener{

    private Socket mSocket = null;
    Thread tr;
    ReceiverThread thread2;

    //쉐어드프리퍼런스 : 로그인 유지 및 로드
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list; //룸네임리스트
    String login_nick = "";
    String notiOn;

    //----------------------------------------------------------------------------------------------
    //온바인드

    @Override
    public IBinder onBind(Intent intent) {
        // Service 객체와 (화면단 Activity 사이에서)
        // 통신(데이터를 주고받을) 때 사용하는 메서드
        // 데이터를 전달할 필요가 없으면 return null;

        return null;
    }

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    public void onCreate() {
        super.onCreate();

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        login_nick = lp.getString("login_nick","");
        room_list = lp.getString("room_list","");

    }

    //----------------------------------------------------------------------------------------------
    //온스타트커맨드 : 서비스가 호출될 때마다 실행

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {

        //소켓생성
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    mSocket = new Socket(SecretKey.ip, SecretKey.port);
                    Log.e(mSocket.toString(),"서비스소켓시작");

                    thread2 = new ReceiverThread(mSocket, login_nick+">리시버", MyChatService.this);
                    thread2.setOnReceiveListener(MyChatService.this);
                    thread2.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        return super.onStartCommand(intent, flags, startId);
    }

    //----------------------------------------------------------------------------------------------
    //온데스트로이 : 서비스가 종료될 때 실행

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if(mSocket != null){
                mSocket.close();
                Log.e(mSocket.toString(),"서비스소켓종료");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(thread2 != null){
            thread2.interrupt();
            Log.e(mSocket.toString(),"리시버종료");
        }

    }

    //----------------------------------------------------------------------------------------------
    //리시버쓰레드에 따른 온리시버

    @Override
    public void onReceive(String message, String listener) {

        String[] split = message.split(">");

        // ~~가 입장하셨습니다 처리 무시
        if (split.length < 3) {
            Log.e("스플릿 길이 3보다 작음","gg");
            return;
        }

        String nickname = split[0];
        String msg = split[1].replace("%n", "\n");
        String room = split[2];

        Log.e("챗온리시브",message);

        //필터링 하지 않아도 됨, if(room_list.contains(room))



        if (isAppIsInBackground(getApplicationContext())) {

            notiOn = lp.getString("noti_on","  - 메시지 알람 Off");
            Log.e("앱 백그라운드 상태 ", "ㅇㅋ");
            if(notiOn.equals("  - 메시지 알람 On")){
                onNotification(msg, nickname, room);
            }

        } else {

            Log.e("닉:"+nickname+"/메:"+msg+"/룸:"+room, "서비스");
            Intent intent = new Intent("blackJinData");
            intent.putExtra("nick", nickname);
            intent.putExtra("msg", msg);
            intent.putExtra("room", room);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);

        }

        lEdit.putInt(room, lp.getInt(room,0)+1);
        lEdit.apply();

    }

    //----------------------------------------------------------------------------------------------
    //알림 : 오레오 버전 고려

    public void onNotification(String msg, String nick, String room){

        String channelId = "channelText";
        String channelName = "ChannelNameTest";

        NotificationManager notifManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(channelId, channelName, importance);
            notifManager.createNotificationChannel(mChannel);
        }

        int requestID = (int) System.currentTimeMillis();

        Resources res = getResources();
        Intent i = new Intent(this, MainActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        i.putExtra("notification_id", 9999);
        i.putExtra("room", room);
        PendingIntent pi = PendingIntent.getActivity(this,requestID,i,PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this,channelId);
        builder.setContentTitle(room)
                .setContentText(nick + " : " +msg)
                .setTicker("["+room + "]에 새로운 메시지가 왔습니다.")
                .setSmallIcon(R.mipmap.ic_launcher_ttttv)
                .setLargeIcon(BitmapFactory.decodeResource(res, R.mipmap.ic_launcher_ttttv))
                .setContentIntent(pi)
                .setAutoCancel(true)
                .setWhen(System.currentTimeMillis())
                .setDefaults(Notification.DEFAULT_ALL); //알림 사운드 진동설정

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            builder.setCategory(Notification.CATEGORY_MESSAGE)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setVisibility(Notification.VISIBILITY_PUBLIC);
        }

        notifManager.notify(1234, builder.build());

    }

    //----------------------------------------------------------------------------------------------
    //앱 상태 확인 : 포그라운드, 백그라운드

    private boolean isAppIsInBackground(Context context) {

        boolean isInBackground = true;
        ActivityManager am = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT_WATCH) {
            List<ActivityManager.RunningAppProcessInfo> runningProcesses = am.getRunningAppProcesses();
            for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
                if (processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                    for (String activeProcess : processInfo.pkgList) {
                        if (activeProcess.equals(context.getPackageName())) {
                            isInBackground = false;
                        }
                    }
                }
            }
        } else {
            List<ActivityManager.RunningTaskInfo> taskInfo = am.getRunningTasks(1);
            ComponentName componentInfo = taskInfo.get(0).topActivity;
            if (componentInfo.getPackageName().equals(context.getPackageName())) {
                isInBackground = false;
            }
        }

        return isInBackground;
    }

    //----------------------------------------------------------------------------------------------

}



