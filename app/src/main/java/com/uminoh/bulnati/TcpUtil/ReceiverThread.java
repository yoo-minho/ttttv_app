package com.uminoh.bulnati.TcpUtil;

import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.Socket;

public class ReceiverThread extends Thread {


    public interface OnReceiveListener {
        void onReceive(String message, String listener);
    }

    OnReceiveListener mListener;

    public void setOnReceiveListener(OnReceiveListener listener) {
        mListener = listener;
    }

    Socket socket;

    public ReceiverThread(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {

        try {
            Log.e("리시버쓰레드시작",mListener.toString());

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
        }
    }

}
