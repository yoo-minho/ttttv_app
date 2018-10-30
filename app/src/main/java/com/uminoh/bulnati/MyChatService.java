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
import android.util.Log;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.TcpUtil.ConstantsTcp;
import com.uminoh.bulnati.TcpUtil.ReceiverThread;
import com.uminoh.bulnati.TcpUtil.SenderThread;

import java.io.IOException;
import java.net.Socket;
import java.util.List;

public class MyChatService extends Service implements ReceiverThread.OnReceiveListener{

    private Socket mSocket = null;
    Thread tr;
    ReceiverThread thread2;
    SenderThread mThread1;
    private Handler mHandler;
    Intent i;

    //쉐어드프리퍼런스 : 로그인 유지 및 로드
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list; //룸네임리스트
    String login_id = "";
    String login_nick = "";

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

        mHandler = new Handler();

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        login_id = lp.getString("login_nick","");
        room_list = lp.getString("room_list","");

        //소켓생성
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(ConstantsTcp.ip, ConstantsTcp.port);

                    // 두번째 파라메터 로는 본인의 닉네임을 적어줍니다.
                    mThread1 = new SenderThread(mSocket,login_nick);
                    mThread1.start();

                    thread2 = new ReceiverThread(mSocket);
                    thread2.setOnReceiveListener(MyChatService.this);
                    thread2.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //----------------------------------------------------------------------------------------------
    //온스타트커맨드 : 서비스가 호출될 때마다 실행

    @Override
    public int onStartCommand(final Intent intent, int flags, int startId) {
        i=intent;
        return super.onStartCommand(intent, flags, startId);
    }

    //----------------------------------------------------------------------------------------------
    //온데스트로이 : 서비스가 종료될 때 실행

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (thread2 != null) {
            thread2.interrupt();
        }

        if (mThread1 != null) {
            mThread1.close();
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

        room_list = lp.getString("room_list","");
//        Log.e("룸네임리스트", room_list);

        //내가 입장해있는 방에만 저장과 알림을 받는다 : * 개선 필요

        if(room_list.contains(room)) {
            if (isAppIsInBackground(getApplicationContext())) {
                Log.e("앱 백그라운드 상태 ", "ㅇㅋ");
                onNotification(msg, nickname, room);
            } else {
//                Log.e("앱 포그라운드 상태", "ㅇㅋ");
                Log.e("메시지 : "+msg,listener);
                mHandler.post(new ToastRunnable("<" + room + "> 방에서\n새로운 메시지가 왔습니다!"));
            }
        }

        //서비스 센더 : 액티비티 메시지 전달
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Bundle bundle = new Bundle();
                ResultReceiver receiver = i.getParcelableExtra("RECEIVER");
                bundle.putString("msg","Succeed!!");
                receiver.send(1,bundle);
            }
        }).start();

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
    //서비스에서 토스트 사용

    private class ToastRunnable implements Runnable {

        String mText;

        public ToastRunnable(String text) {
            mText = text;
        }

        @Override
        public void run(){
            StyleableToast.makeText(getApplicationContext(), mText, Toast.LENGTH_SHORT, R.style.mytoast).show();
        }
    }

    //----------------------------------------------------------------------------------------------

}



