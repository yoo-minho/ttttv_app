/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package com.uminoh.bulnati;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.WebRtcUtil.ConstantsRtc;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

/**
 * Handles the initial setup where the user selects which room to join.
 */
public class ConnectActivity extends Activity {
  private static final String TAG = "ConnectActivity";
  private static final int CONNECTION_REQUEST = 1;
  private static boolean commandLineRun = false;

  private SharedPreferences sharedPref;
  private String keyprefVideoCallEnabled;
  private String keyprefScreencapture;
  private String keyprefCamera2;
  private String keyprefResolution;
  private String keyprefFps;
  private String keyprefCaptureQualitySlider;
  private String keyprefVideoBitrateType;
  private String keyprefVideoBitrateValue;
  private String keyprefVideoCodec;
  private String keyprefAudioBitrateType;
  private String keyprefAudioBitrateValue;
  private String keyprefAudioCodec;
  private String keyprefHwCodecAcceleration;
  private String keyprefCaptureToTexture;
  private String keyprefFlexfec;
  private String keyprefNoAudioProcessingPipeline;
  private String keyprefAecDump;
  private String keyprefOpenSLES;
  private String keyprefDisableBuiltInAec;
  private String keyprefDisableBuiltInAgc;
  private String keyprefDisableBuiltInNs;
  private String keyprefEnableLevelControl;
  private String keyprefDisableWebRtcAGCAndHPF;
  private String keyprefDisplayHud;
  private String keyprefTracing;
  private String keyprefRoomServerUrl;
  private String keyprefRoom;
  private String keyprefRoomList;
  private ArrayList<String> roomList;
  private ArrayAdapter<String> adapter;
  private String keyprefEnableDataChannel;
  private String keyprefOrdered;
  private String keyprefMaxRetransmitTimeMs;
  private String keyprefMaxRetransmits;
  private String keyprefDataProtocol;
  private String keyprefNegotiated;
  private String keyprefDataId;

  //웹 통신
  Retrofit retrofit;
  ApiService apiService;
  SharedPreferences lp;
  SharedPreferences.Editor lEdit;

  //버튼
  String randomRoom;
  TextView backRandomChat;
  TextView totalRandom;
  TextView currentWait;
  Button currentMatch;
  ImageView imageRandom;

  //권한 선언
  private String[] permissions = {android.Manifest.permission.RECORD_AUDIO, android.Manifest.permission.READ_PHONE_STATE, Manifest.permission.CAMERA}; //권한 설정 변수
  private static final int MULTIPLE_PERMISSIONS = 101; //권한 동의 여부 문의 후 콜백 함수에 쓰일 변수

  //----------------------------------------------------------------------------------------------
  //온크리에이트

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // Get setting keys.
    sharedPref = PreferenceManager.getDefaultSharedPreferences(this);
    keyprefVideoCallEnabled = getString(R.string.pref_videocall_key);
    keyprefScreencapture = getString(R.string.pref_screencapture_key);
    keyprefCamera2 = getString(R.string.pref_camera2_key);
    keyprefResolution = getString(R.string.pref_resolution_key);
    keyprefFps = getString(R.string.pref_fps_key);
    keyprefCaptureQualitySlider = getString(R.string.pref_capturequalityslider_key);
    keyprefVideoBitrateType = getString(R.string.pref_maxvideobitrate_key);
    keyprefVideoBitrateValue = getString(R.string.pref_maxvideobitratevalue_key);
    keyprefVideoCodec = getString(R.string.pref_videocodec_key);
    keyprefHwCodecAcceleration = getString(R.string.pref_hwcodec_key);
    keyprefCaptureToTexture = getString(R.string.pref_capturetotexture_key);
    keyprefFlexfec = getString(R.string.pref_flexfec_key);
    keyprefAudioBitrateType = getString(R.string.pref_startaudiobitrate_key);
    keyprefAudioBitrateValue = getString(R.string.pref_startaudiobitratevalue_key);
    keyprefAudioCodec = getString(R.string.pref_audiocodec_key);
    keyprefNoAudioProcessingPipeline = getString(R.string.pref_noaudioprocessing_key);
    keyprefAecDump = getString(R.string.pref_aecdump_key);
    keyprefOpenSLES = getString(R.string.pref_opensles_key);
    keyprefDisableBuiltInAec = getString(R.string.pref_disable_built_in_aec_key);
    keyprefDisableBuiltInAgc = getString(R.string.pref_disable_built_in_agc_key);
    keyprefDisableBuiltInNs = getString(R.string.pref_disable_built_in_ns_key);
    keyprefEnableLevelControl = getString(R.string.pref_enable_level_control_key);
    keyprefDisableWebRtcAGCAndHPF = getString(R.string.pref_disable_webrtc_agc_and_hpf_key);
    keyprefDisplayHud = getString(R.string.pref_displayhud_key);
    keyprefTracing = getString(R.string.pref_tracing_key);
    keyprefRoomServerUrl = getString(R.string.pref_room_server_url_key);
    keyprefRoom = getString(R.string.pref_room_key);
    keyprefRoomList = getString(R.string.pref_room_list_key);
    keyprefEnableDataChannel = getString(R.string.pref_enable_datachannel_key);
    keyprefOrdered = getString(R.string.pref_ordered_key);
    keyprefMaxRetransmitTimeMs = getString(R.string.pref_max_retransmit_time_ms_key);
    keyprefMaxRetransmits = getString(R.string.pref_max_retransmits_key);
    keyprefDataProtocol = getString(R.string.pref_data_protocol_key);
    keyprefNegotiated = getString(R.string.pref_negotiated_key);
    keyprefDataId = getString(R.string.pref_data_id_key);

    setContentView(R.layout.activity_connect);

    //권한 묻기
    checkPermissions();

    //주소를 기반으로 객체 생성
    retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
    apiService = retrofit.create(ApiService.class);
    lp =getSharedPreferences("login",MODE_PRIVATE);
    lEdit = lp.edit();

    backRandomChat = findViewById(R.id.back_random_chat);
    totalRandom = findViewById(R.id.total_random);
    currentWait = findViewById(R.id.current_wait);
    currentMatch = findViewById(R.id.current_match);
    imageRandom = findViewById(R.id.image_random);

    Glide.with(getApplicationContext())
            .load(R.raw.watch)
            .into(imageRandom);

    backRandomChat.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {
        onBackPressed();
      }
    });

    onVideoListShow();

    currentMatch.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View view) {

        randomRoom = Integer.toString((new Random()).nextInt(100000000));

        //레트로핏 API : 비디오 리스트 고려 실행!
        Call<ResponseBody> video_list_up = apiService.videoListUp(randomRoom);
        video_list_up.enqueue(new Callback<ResponseBody>() {
          @Override
          public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
            try {
              String result = response.body().string();
              connectToRoom(result, false, false, false, 0);
              lEdit.putString("connect_room",randomRoom);
              lEdit.apply();
            } catch (IOException e) {
              e.printStackTrace();
            }
          }
          @Override
          public void onFailure(Call<ResponseBody> call, Throwable t) {
          }
        });

      }
    });

    // 이 앱이 불려졌을때, 액션뷰상태라면 방을 연결해줘! 이 때만 커맨드라인 true 됨!
    final Intent intent = getIntent();
    if ("android.intent.action.VIEW".equals(intent.getAction()) && !commandLineRun) {

      boolean loopback = intent.getBooleanExtra(ConstantsRtc.EXTRA_LOOPBACK, false);
      int runTimeMs = intent.getIntExtra(ConstantsRtc.EXTRA_RUNTIME, 0);
      boolean useValuesFromIntent =
          intent.getBooleanExtra(ConstantsRtc.EXTRA_USE_VALUES_FROM_INTENT, false);

      String room = sharedPref.getString(keyprefRoom, "");

      /**
       ※ connectToRoom 간단 요약
       - connectToRoom(스트링, 불린, 불린, 불린, 인트);
       - connectToRoom(룸, 커맨드라인, 루프, 인텐트밸류, 런타임);
       - 스트링 : 룸이름(단순전달) - 기본값
       - 불린 : 커맨드라인(단순전달) - 트루, 인텐트로 돌아왔을때 이 과정 진행 안함!
       - 불린 : 트루면 룸아이디로 랜덤을 가지고옴, 펄스면 에딧값 - 인텐트 없으면 펄스
       - 불린 : 각종 기능에 대하여 트루면 콜액티비티에서온값(새로운값), 펄스면 쉐어드값(원래값) - 인텐트 없으면 펄스
       - 인트 : 런타임값(단순전달) - 인텐트 없으면 0
       */

      connectToRoom(room, true, loopback, useValuesFromIntent, runTimeMs);
    }

  }

  @Override
  public void onPause() {
    super.onPause();
    //쓰던 룸 이름 저장
    SharedPreferences.Editor editor = sharedPref.edit();
    editor.putString(keyprefRoom, randomRoom);
    editor.commit();
  }

  @Override
  protected void onRestart() {
    super.onRestart();

    onVideoListShow();
  }

  @Override
  public void onResume() {
    super.onResume();

    //다시 켜더라도 룸 이름 그대로 에딧에 넣어둬
    String room = sharedPref.getString(keyprefRoom, "");
  }

  //connectToRoom 따른 리절트 함수
  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == CONNECTION_REQUEST && commandLineRun) {
      Log.d(TAG, "Return: " + resultCode);
      setResult(resultCode);
      commandLineRun = false;
      finish();
    }
  }

  //쉐어드함수 공통, 4번째가 트루면 인텐트+2번째 값, 펄스면 쉐어드+1번째값
  //쉐어드에서 스트링 값을 가져옴
  private String sharedPrefGetString(
      int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    String defaultValue = getString(defaultId);
    if (useFromIntent) {
      String value = getIntent().getStringExtra(intentName);
      if (value != null) {
        return value;
      }
      return defaultValue;
    } else {
      String attributeName = getString(attributeId);
      return sharedPref.getString(attributeName, defaultValue);
    }
  }

  //쉐어드에서 불린 값을 가져옴
  private boolean sharedPrefGetBoolean(
      int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    boolean defaultValue = Boolean.valueOf(getString(defaultId));
    if (useFromIntent) {
      return getIntent().getBooleanExtra(intentName, defaultValue);
    } else {
      String attributeName = getString(attributeId);
      return sharedPref.getBoolean(attributeName, defaultValue);
    }
  }

  //쉐어드에서 인트 값을 가져옴
  private int sharedPrefGetInteger(
      int attributeId, String intentName, int defaultId, boolean useFromIntent) {
    String defaultString = getString(defaultId);
    int defaultValue = Integer.parseInt(defaultString);
    if (useFromIntent) {
      return getIntent().getIntExtra(intentName, defaultValue);
    } else {
      String attributeName = getString(attributeId);
      String value = sharedPref.getString(attributeName, defaultString);
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        Log.e(TAG, "Wrong setting for: " + attributeName + ":" + value);
        return defaultValue;
      }
    }
  }

  /**
   ※ connectToRoom 간단 요약
   - connectToRoom(스트링, 불린, 불린, 불린, 인트);
   - connectToRoom(룸, 커맨드라인, 루프, 인텐트밸류, 런타임);
   - 스트링 : 룸이름(단순전달)
   - 불린 : 커맨드라인(단순전달)
   - 불린 : 트루면 룸아이디로 랜덤을 가지고옴, 펄스면 에딧값
   - 불린 : 각종 기능에 대하여 트루면 콜액티비티에서온값(새로운값), 펄스면 쉐어드값(원래값)
   - 인트 : 런타임값(단순전달)
   */

  private void connectToRoom(String roomId, boolean commandLineRun, boolean loopback,
      boolean useValuesFromIntent, int runTimeMs) {
    this.commandLineRun = commandLineRun;

    if (loopback) {
      roomId = Integer.toString((new Random()).nextInt(100000000));
    }

    String roomUrl = sharedPref.getString(
        keyprefRoomServerUrl, getString(R.string.pref_room_server_url_default));

    //세팅차례대로
    //각종 기능 적용 : 유즈밸류가 트루면 2번째 활용 콜액티비티에서온 값, 펄스면 1번째 활용 쉐어드 값
    boolean videoCallEnabled = sharedPrefGetBoolean(R.string.pref_videocall_key, ConstantsRtc.EXTRA_VIDEO_CALL, R.string.pref_videocall_default, useValuesFromIntent);
    boolean useScreencapture = sharedPrefGetBoolean(R.string.pref_screencapture_key, ConstantsRtc.EXTRA_SCREENCAPTURE, R.string.pref_screencapture_default, useValuesFromIntent);
    boolean useCamera2 = sharedPrefGetBoolean(R.string.pref_camera2_key, ConstantsRtc.EXTRA_CAMERA2, R.string.pref_camera2_default, useValuesFromIntent);
    boolean hwCodec = sharedPrefGetBoolean(R.string.pref_hwcodec_key, ConstantsRtc.EXTRA_HWCODEC_ENABLED, R.string.pref_hwcodec_default, useValuesFromIntent);
    boolean captureToTexture = sharedPrefGetBoolean(R.string.pref_capturetotexture_key, ConstantsRtc.EXTRA_CAPTURETOTEXTURE_ENABLED, R.string.pref_capturetotexture_default, useValuesFromIntent);
    boolean flexfecEnabled = sharedPrefGetBoolean(R.string.pref_flexfec_key, ConstantsRtc.EXTRA_FLEXFEC_ENABLED, R.string.pref_flexfec_default, useValuesFromIntent);
    boolean noAudioProcessing = sharedPrefGetBoolean(R.string.pref_noaudioprocessing_key, ConstantsRtc.EXTRA_NOAUDIOPROCESSING_ENABLED, R.string.pref_noaudioprocessing_default, useValuesFromIntent);
    boolean aecDump = sharedPrefGetBoolean(R.string.pref_aecdump_key, ConstantsRtc.EXTRA_AECDUMP_ENABLED, R.string.pref_aecdump_default, useValuesFromIntent);
    boolean useOpenSLES = sharedPrefGetBoolean(R.string.pref_opensles_key, ConstantsRtc.EXTRA_OPENSLES_ENABLED, R.string.pref_opensles_default, useValuesFromIntent);
    boolean disableBuiltInAEC = sharedPrefGetBoolean(R.string.pref_disable_built_in_aec_key, ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AEC, R.string.pref_disable_built_in_aec_default, useValuesFromIntent);
    boolean disableBuiltInAGC = sharedPrefGetBoolean(R.string.pref_disable_built_in_agc_key, ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AGC, R.string.pref_disable_built_in_agc_default, useValuesFromIntent);
    boolean disableBuiltInNS = sharedPrefGetBoolean(R.string.pref_disable_built_in_ns_key, ConstantsRtc.EXTRA_DISABLE_BUILT_IN_NS, R.string.pref_disable_built_in_ns_default, useValuesFromIntent);
    boolean enableLevelControl = sharedPrefGetBoolean(R.string.pref_enable_level_control_key, ConstantsRtc.EXTRA_ENABLE_LEVEL_CONTROL, R.string.pref_enable_level_control_key, useValuesFromIntent);
    boolean disableWebRtcAGCAndHPF = sharedPrefGetBoolean(R.string.pref_disable_webrtc_agc_and_hpf_key, ConstantsRtc.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, R.string.pref_disable_webrtc_agc_and_hpf_key, useValuesFromIntent);
    boolean captureQualitySlider = sharedPrefGetBoolean(R.string.pref_capturequalityslider_key, ConstantsRtc.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, R.string.pref_capturequalityslider_default, useValuesFromIntent);
    boolean displayHud = sharedPrefGetBoolean(R.string.pref_displayhud_key, ConstantsRtc.EXTRA_DISPLAY_HUD, R.string.pref_displayhud_default, useValuesFromIntent);
    boolean tracing = sharedPrefGetBoolean(R.string.pref_tracing_key, ConstantsRtc.EXTRA_TRACING, R.string.pref_tracing_default, useValuesFromIntent);
    boolean dataChannelEnabled = sharedPrefGetBoolean(R.string.pref_enable_datachannel_key, ConstantsRtc.EXTRA_DATA_CHANNEL_ENABLED, R.string.pref_enable_datachannel_default, useValuesFromIntent);
    boolean ordered = sharedPrefGetBoolean(R.string.pref_ordered_key, ConstantsRtc.EXTRA_ORDERED, R.string.pref_ordered_default, useValuesFromIntent);
    boolean negotiated = sharedPrefGetBoolean(R.string.pref_negotiated_key, ConstantsRtc.EXTRA_NEGOTIATED, R.string.pref_negotiated_default, useValuesFromIntent);
    int maxRetrMs = sharedPrefGetInteger(R.string.pref_max_retransmit_time_ms_key, ConstantsRtc.EXTRA_MAX_RETRANSMITS_MS, R.string.pref_max_retransmit_time_ms_default, useValuesFromIntent);
    int maxRetr = sharedPrefGetInteger(R.string.pref_max_retransmits_key, ConstantsRtc.EXTRA_MAX_RETRANSMITS, R.string.pref_max_retransmits_default, useValuesFromIntent);
    int id = sharedPrefGetInteger(R.string.pref_data_id_key, ConstantsRtc.EXTRA_ID, R.string.pref_data_id_default, useValuesFromIntent);
    String protocol = sharedPrefGetString(R.string.pref_data_protocol_key, ConstantsRtc.EXTRA_PROTOCOL, R.string.pref_data_protocol_default, useValuesFromIntent);
    String videoCodec = sharedPrefGetString(R.string.pref_videocodec_key, ConstantsRtc.EXTRA_VIDEOCODEC, R.string.pref_videocodec_default, useValuesFromIntent);
    String audioCodec = sharedPrefGetString(R.string.pref_audiocodec_key, ConstantsRtc.EXTRA_AUDIOCODEC, R.string.pref_audiocodec_default, useValuesFromIntent);

    // 해상도 세팅
    int videoWidth = 0;
    int videoHeight = 0;
    if (useValuesFromIntent) {
      videoWidth = getIntent().getIntExtra(ConstantsRtc.EXTRA_VIDEO_WIDTH, 0);
      videoHeight = getIntent().getIntExtra(ConstantsRtc.EXTRA_VIDEO_HEIGHT, 0);
    }
    if (videoWidth == 0 && videoHeight == 0) {
      String resolution = sharedPref.getString(keyprefResolution, getString(R.string.pref_resolution_default));
      String[] dimensions = resolution.split("[ x]+");
      if (dimensions.length == 2) {
        try {
          videoWidth = Integer.parseInt(dimensions[0]);
          videoHeight = Integer.parseInt(dimensions[1]);
        } catch (NumberFormatException e) {
          videoWidth = 0;
          videoHeight = 0;
          Log.e(TAG, "Wrong video resolution setting: " + resolution);
        }
      }
    }

    // FPS 세팅
    int cameraFps = 0;
    if (useValuesFromIntent) {
      cameraFps = getIntent().getIntExtra(ConstantsRtc.EXTRA_VIDEO_FPS, 0);
    }
    if (cameraFps == 0) {
      String fps = sharedPref.getString(keyprefFps, getString(R.string.pref_fps_default));
      String[] fpsValues = fps.split("[ x]+");
      if (fpsValues.length == 2) {
        try {
          cameraFps = Integer.parseInt(fpsValues[0]);
        } catch (NumberFormatException e) {
          cameraFps = 0;
          Log.e(TAG, "Wrong camera fps setting: " + fps);
        }
      }
    }

    // 비디오,오디오 진동 세팅
    int videoStartBitrate = 0;
    if (useValuesFromIntent) {
      videoStartBitrate = getIntent().getIntExtra(ConstantsRtc.EXTRA_VIDEO_BITRATE, 0);
    }
    if (videoStartBitrate == 0) {
      String bitrateTypeDefault = getString(R.string.pref_maxvideobitrate_default);
      String bitrateType = sharedPref.getString(keyprefVideoBitrateType, bitrateTypeDefault);
      if (!bitrateType.equals(bitrateTypeDefault)) {
        String bitrateValue = sharedPref.getString(
            keyprefVideoBitrateValue, getString(R.string.pref_maxvideobitratevalue_default));
        videoStartBitrate = Integer.parseInt(bitrateValue);
      }
    }
    int audioStartBitrate = 0;
    if (useValuesFromIntent) {
      audioStartBitrate = getIntent().getIntExtra(ConstantsRtc.EXTRA_AUDIO_BITRATE, 0);
    }
    if (audioStartBitrate == 0) {
      String bitrateTypeDefault = getString(R.string.pref_startaudiobitrate_default);
      String bitrateType = sharedPref.getString(keyprefAudioBitrateType, bitrateTypeDefault);
      if (!bitrateType.equals(bitrateTypeDefault)) {
        String bitrateValue = sharedPref.getString(
            keyprefAudioBitrateValue, getString(R.string.pref_startaudiobitratevalue_default));
        audioStartBitrate = Integer.parseInt(bitrateValue);
      }
    }

    //액티비티를 가기 위한 인텐트 준비
    Log.d(TAG, "Connecting to room " + roomId + " at URL " + roomUrl);
    if (validateUrl(roomUrl)) {
      Uri uri = Uri.parse(roomUrl);
      Intent intent = new Intent(this, CallActivity.class);
      intent.setData(uri); //
      intent.putExtra(ConstantsRtc.EXTRA_ROOMID, roomId); //
      intent.putExtra(ConstantsRtc.EXTRA_LOOPBACK, loopback); //
      intent.putExtra(ConstantsRtc.EXTRA_CMDLINE, commandLineRun); //
      intent.putExtra(ConstantsRtc.EXTRA_RUNTIME, runTimeMs); //

      //세팅차례대로
      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_CALL, videoCallEnabled);
      intent.putExtra(ConstantsRtc.EXTRA_SCREENCAPTURE, useScreencapture);
      intent.putExtra(ConstantsRtc.EXTRA_CAMERA2, useCamera2);
      intent.putExtra(ConstantsRtc.EXTRA_HWCODEC_ENABLED, hwCodec);
      intent.putExtra(ConstantsRtc.EXTRA_CAPTURETOTEXTURE_ENABLED, captureToTexture);
      intent.putExtra(ConstantsRtc.EXTRA_FLEXFEC_ENABLED, flexfecEnabled);
      intent.putExtra(ConstantsRtc.EXTRA_NOAUDIOPROCESSING_ENABLED, noAudioProcessing);
      intent.putExtra(ConstantsRtc.EXTRA_AECDUMP_ENABLED, aecDump);
      intent.putExtra(ConstantsRtc.EXTRA_OPENSLES_ENABLED, useOpenSLES);
      intent.putExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AEC, disableBuiltInAEC);
      intent.putExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AGC, disableBuiltInAGC);
      intent.putExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_NS, disableBuiltInNS);
      intent.putExtra(ConstantsRtc.EXTRA_ENABLE_LEVEL_CONTROL, enableLevelControl);
      intent.putExtra(ConstantsRtc.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, disableWebRtcAGCAndHPF);
      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_CAPTUREQUALITYSLIDER_ENABLED, captureQualitySlider);
      intent.putExtra(ConstantsRtc.EXTRA_DISPLAY_HUD, displayHud);
      intent.putExtra(ConstantsRtc.EXTRA_TRACING, tracing); //
      intent.putExtra(ConstantsRtc.EXTRA_DATA_CHANNEL_ENABLED, dataChannelEnabled);
      if (dataChannelEnabled) {
        intent.putExtra(ConstantsRtc.EXTRA_ORDERED, ordered);
        intent.putExtra(ConstantsRtc.EXTRA_MAX_RETRANSMITS_MS, maxRetrMs);
        intent.putExtra(ConstantsRtc.EXTRA_MAX_RETRANSMITS, maxRetr);
        intent.putExtra(ConstantsRtc.EXTRA_NEGOTIATED, negotiated);
        intent.putExtra(ConstantsRtc.EXTRA_ID, id);
        intent.putExtra(ConstantsRtc.EXTRA_PROTOCOL, protocol);
      }
      intent.putExtra(ConstantsRtc.EXTRA_VIDEOCODEC, videoCodec);
      intent.putExtra(ConstantsRtc.EXTRA_AUDIOCODEC, audioCodec);

      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_WIDTH, videoWidth);
      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_HEIGHT, videoHeight);
      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_FPS, cameraFps);
      intent.putExtra(ConstantsRtc.EXTRA_VIDEO_BITRATE, videoStartBitrate);
      intent.putExtra(ConstantsRtc.EXTRA_AUDIO_BITRATE, audioStartBitrate);

      //추가로 넣는 것들
      if (useValuesFromIntent) {
        if (getIntent().hasExtra(ConstantsRtc.EXTRA_VIDEO_FILE_AS_CAMERA)) {
          String videoFileAsCamera =
              getIntent().getStringExtra(ConstantsRtc.EXTRA_VIDEO_FILE_AS_CAMERA);
          intent.putExtra(ConstantsRtc.EXTRA_VIDEO_FILE_AS_CAMERA, videoFileAsCamera);
        }

        if (getIntent().hasExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE)) {
          String saveRemoteVideoToFile =
              getIntent().getStringExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
          intent.putExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE, saveRemoteVideoToFile);
        }

        if (getIntent().hasExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH)) {
          int videoOutWidth =
              getIntent().getIntExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
          intent.putExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, videoOutWidth);
        }

        if (getIntent().hasExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT)) {
          int videoOutHeight =
              getIntent().getIntExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
          intent.putExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, videoOutHeight);
        }
      }

      startActivityForResult(intent, CONNECTION_REQUEST);
    }
  }

  private boolean validateUrl(String url) {
    if (URLUtil.isHttpsUrl(url) || URLUtil.isHttpUrl(url)) {
      return true;
    }
    new AlertDialog.Builder(this)
        .setTitle(getText(R.string.invalid_url_title))
        .setMessage(getString(R.string.invalid_url_text, url))
        .setCancelable(false)
        .setNeutralButton(R.string.ok,
            new DialogInterface.OnClickListener() {
              public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
              }
            })
        .create()
        .show();
    return false;
  }

  private void onVideoListShow(){
    //레트로핏 API : 채팅방 생성!
    Call<ResponseBody> video_list_show = apiService.videoListShow();
    video_list_show.enqueue(new Callback<ResponseBody>() {
      @Override
      public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
        try {
          String result = response.body().string();
          if(result.equals("fail")){
            Log.e("비디오_리스트_쇼","실패");
          } else {
            String[] rd = result.split("%");
            totalRandom.setText( "누적 채팅인원 : "+rd[0]+"쌍");
            if(!rd[1].equals("0")){
              currentWait.setText( "현재 대기인원 : 있음!!!");
            } else {
              currentWait.setText( "현재 대기인원 : 없음");
            }
          }

        } catch (IOException e) {
          e.printStackTrace();
        }
      }
      @Override
      public void onFailure(Call<ResponseBody> call, Throwable t) {
      }
    });
  }

  //----------------------------------------------------------------------------------------------

  //권한체크
  private boolean checkPermissions() {
    int result;
    List<String> permissionList = new ArrayList<>(); //권한 배열리스트 하나 만든다
    for (String pm : permissions) { // 세 권한 다 따져봐
      result = ContextCompat.checkSelfPermission(this, pm); //셀프로 권한 체크해본 결과를 담아서
      if (result != PackageManager.PERMISSION_GRANTED) { //부여된 권한과 비교해서 없으면
        permissionList.add(pm); //리스트에 추가하자.
      }
    }
    if (!permissionList.isEmpty()) { //부여 권한이 아닌 애들이 존재한다면
      ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]),
              MULTIPLE_PERMISSIONS); //사용자에게 리스트의 권한 요청 페이지를 차례대로 요청하자 + 콜백할 때 멀티플 퍼미션 변수 쓰자.
      return false; // 권한 요청할게 있네
    }
    return true; // 권한 다 충족하네
  }

  //----------------------------------------------------------------------------------------------

  //권한요청
  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    switch (requestCode) {
      case MULTIPLE_PERMISSIONS: {
        if (grantResults.length > 0) { //동의했든 안했든 권한이 1개라도 있다면
          for (int i = 0; i < permissions.length; i++) { //그 권한 개수만큼 따져보자
            if (permissions[i].equals(this.permissions[0])) { //저장소읽기 권한이랑 같은 권한이 있을때
              if (grantResults[i] != PackageManager.PERMISSION_GRANTED) { //부여된 권한이 아니라면
                showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                Log.e("토스트","토스트1");
              }
            } else if (permissions[i].equals(this.permissions[1])) { //저장소쓰기 권한이랑 같은 권한이 있을때
              if (grantResults[i] != PackageManager.PERMISSION_GRANTED) { //부여된 권한이 아니라면
                showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                Log.e("토스트","토스트2");
              }
            } else if (permissions[i].equals(this.permissions[2])) { //저장소쓰기 권한이랑 같은 권한이 있을때
              if (grantResults[i] != PackageManager.PERMISSION_GRANTED) { //부여된 권한이 아니라면
                showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                Log.e("토스트","토스트3");
              }
            }
          }
        } else {
          showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
        }
        return;
      }
    }
  }

  //----------------------------------------------------------------------------------------------

  //권한토스트
  private void showNoPermissionToastAndFinish() {
    StyleableToast.makeText(this, "권한 요청에 동의 해주셔야 이용가능합니다.\n"
            + "설정에서 권한을 허용하시기 바랍니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
    finish();
  }

}
