/*
 *  Copyright 2015 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.uminoh.bulnati;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import com.uminoh.bulnati.WebRtcUtil.ConstantsRtc;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Fragment for call control.
 */
public class CallFragment extends Fragment {

  private View controlView;
  private TextView contactView;
  private TextView timeCall;
  private Button disconnectButton;
  private Button cameraSwitchButton;
  private Button toggleMuteButton;
  private OnCallEvents callEvents;
  private boolean videoCallEnabled = true;

  private TimerTask mTask;
  private Timer mTimer;
  private int time = 0;

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;

  /**
   * Call control interface for container activity.
   */
  public interface OnCallEvents {
    void onCallHangUp();
    void onCameraSwitch();
    boolean onToggleMic();
  }

  @Override
  public View onCreateView(
      LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    controlView = inflater.inflate(R.layout.fragment_call, container, false);

    // Create UI controls.
    contactView = controlView.findViewById(R.id.contact_name_call);
    timeCall = controlView.findViewById(R.id.time_call);
    disconnectButton = controlView.findViewById(R.id.button_call_disconnect);
    cameraSwitchButton = controlView.findViewById(R.id.button_call_switch_camera);
    toggleMuteButton = controlView.findViewById(R.id.button_call_toggle_mic);

      //주소를 기반으로 객체 생성
      retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
      apiService = retrofit.create(ApiService.class);
      lp = getActivity().getSharedPreferences("login", Context.MODE_PRIVATE);
      lEdit = lp.edit();

    // Add buttons click events.
    disconnectButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {

          Log.e("비디오_리스트_다운 결과","시작");

          //레트로핏 API : 채팅방 생성!
          Call<ResponseBody> video_list_down = apiService.videoListDown(lp.getString("connect_room",""));
          video_list_down.enqueue(new Callback<ResponseBody>() {
              @Override
              public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                  try {
                      String result = response.body().string();
                      Log.e("비디오_리스트_다운 결과",result);
                      callEvents.onCallHangUp();
                  } catch (IOException e) {
                      e.printStackTrace();
                  }
              }
              @Override
              public void onFailure(Call<ResponseBody> call, Throwable t) {
                  Log.e("비디오_리스트_다운 결과","실패");
              }
          });

      }
    });

    //타이머
    mTask = new TimerTask() {
        @Override
        public void run() {

            time++;
            final int min = time/60;
            final int sec = time%60;

            if(getActivity() != null){
                getActivity().runOnUiThread(new Runnable() {
                    @SuppressLint("SetTextI18n")
                    @Override
                    public void run() {
                        if(min == 0){
                            if(sec < 10 ){
                                timeCall.setText("00:0"+sec);
                            } else {
                                timeCall.setText("00:"+sec);
                            }
                        } else if ( min < 10) {
                            if(sec < 10 ){
                                timeCall.setText("0"+min+":0"+sec);
                            } else {
                                timeCall.setText("0"+min+":"+sec);
                            }
                        } else {
                            if(sec < 10 ){
                                timeCall.setText(min+":0"+sec);
                            } else {
                                timeCall.setText(min+":"+sec);
                            }
                        }
                    }
                });
            }
        }
    };
    mTimer = new Timer();
    mTimer.schedule(mTask,0,1000);

    cameraSwitchButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        callEvents.onCameraSwitch();
      }
    });

    toggleMuteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        boolean enabled = callEvents.onToggleMic();
        toggleMuteButton.setAlpha(enabled ? 1.0f : 0.3f);
      }
    });

    return controlView;
  }

  @Override
  public void onStart() {
    super.onStart();

    Bundle args = getArguments();
    if (args != null) {
      String contactName = args.getString(ConstantsRtc.EXTRA_ROOMID);
      contactView.setText(contactName);
      videoCallEnabled = args.getBoolean(ConstantsRtc.EXTRA_VIDEO_CALL, true);
    }
    if (!videoCallEnabled) {
      cameraSwitchButton.setVisibility(View.INVISIBLE);
    }
  }

  // TODO(sakal): Replace with onAttach(Context) once we only support API level 23+.
  @SuppressWarnings("deprecation")
  @Override
  public void onAttach(Activity activity) {
    super.onAttach(activity);
    callEvents = (OnCallEvents) activity;
  }
}
