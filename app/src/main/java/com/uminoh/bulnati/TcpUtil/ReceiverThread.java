package com.uminoh.bulnati.TcpUtil;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ReceiverThread extends Thread {

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    Context context;

    public interface OnReceiveListener {
        void onReceive(String message, String listener);
    }

    OnReceiveListener mListener;

    public void setOnReceiveListener(OnReceiveListener listener) {
        mListener = listener;
    }

    private Socket socket;
    private String name;
    private PrintWriter mWriter;

    public ReceiverThread(Socket socket, String name, Context context) {
        this.socket = socket;
        this.name = name;
        this.context = context;

        //쉐어드프리퍼런스 연결
        lp = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        lEdit = lp.edit();
    }

    @Override
    public void run() {

        try {

            Log.e("리시버쓰레드시작",mListener.toString());
            mWriter = new PrintWriter(socket.getOutputStream());
            // 제일 먼저 서버로 대화명을 송신합니다.
            mWriter.println(name);
            mWriter.flush();

            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            while (true) {
                String str = reader.readLine();
                if (mListener != null) {
                    mListener.onReceive(str, mListener.toString());
                } else {
                    break;
                }
            }

        } catch (Exception e) {

            Log.e("chat", "run: " + e.getMessage());
            lEdit.putBoolean("receiver_dead",true);
            lEdit.apply();

        }
    }

}
