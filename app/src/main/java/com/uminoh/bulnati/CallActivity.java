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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.WebRtcUtil.Audio.AppRTCAudioManager;
import com.uminoh.bulnati.WebRtcUtil.AppRTCClient;
import com.uminoh.bulnati.WebRtcUtil.ConstantsRtc;
import com.uminoh.bulnati.WebRtcUtil.PeerConnectionClient;
import com.uminoh.bulnati.WebRtcUtil.WebSocketRTCClient;

import org.webrtc.Camera1Enumerator;
import org.webrtc.Camera2Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.EglBase;
import org.webrtc.FileVideoCapturer;
import org.webrtc.IceCandidate;
import org.webrtc.Logging;
import org.webrtc.PeerConnectionFactory;
import org.webrtc.RendererCommon.ScalingType;
import org.webrtc.ScreenCapturerAndroid;
import org.webrtc.SessionDescription;
import org.webrtc.StatsReport;
import org.webrtc.SurfaceViewRenderer;
import org.webrtc.VideoCapturer;
import org.webrtc.VideoFileRenderer;
import org.webrtc.VideoRenderer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class CallActivity extends Activity implements AppRTCClient.SignalingEvents, PeerConnectionClient.PeerConnectionEvents, CallFragment.OnCallEvents {

      private static final String TAG = CallActivity.class.getSimpleName();
      private static final int CAPTURE_PERMISSION_REQUEST_CODE = 1;
      private static final String[] MANDATORY_PERMISSIONS = {"android.permission.MODIFY_AUDIO_SETTINGS",
          "android.permission.RECORD_AUDIO", "android.permission.INTERNET"};
      private static final int STAT_CALLBACK_PERIOD = 1000;

  //----------------------------------------------------------------------------------------------
  //중계화면

      private class ProxyRenderer implements VideoRenderer.Callbacks {
        private VideoRenderer.Callbacks target;
        synchronized public void renderFrame(VideoRenderer.I420Frame frame) {
          if (target == null) {
            Logging.d(TAG, "Dropping frame in proxy because target is null.");
            VideoRenderer.renderFrameDone(frame);
            return;
          }
          target.renderFrame(frame);
        }
        synchronized public void setTarget(VideoRenderer.Callbacks target) {
          this.target = target;
        }
      }

    //----------------------------------------------------------------------------------------------
    //변수 선언

      private final ProxyRenderer remoteProxyRenderer = new ProxyRenderer(); //프록시상대화면
      private final ProxyRenderer localProxyRenderer = new ProxyRenderer(); //프록시본인화면
      private PeerConnectionClient peerConnectionClient = null; //클라이언트
      private AppRTCClient appRtcClient;
      private AppRTCClient.SignalingParameters signalingParameters;
      private AppRTCAudioManager audioManager = null;
      private EglBase rootEglBase;
      private SurfaceViewRenderer pipRenderer;
      private SurfaceViewRenderer fullscreenRenderer;
      private VideoFileRenderer videoFileRenderer;
      private final List<VideoRenderer.Callbacks> remoteRenderers = new ArrayList<>();
      private Toast logToast;
      private AppRTCClient.RoomConnectionParameters roomConnectionParameters;
      private PeerConnectionClient.PeerConnectionParameters peerConnectionParameters;
      private boolean commandLineRun;
      private int runTimeMs;
      private boolean activityRunning;
      private boolean iceConnected;
      private boolean isError;
      private boolean callControlFragmentVisible = true;
      private long callStartedTimeMs = 0;
      private boolean micEnabled = true;
      private boolean screencaptureEnabled = false;
      private static Intent mediaProjectionPermissionResultData;
      private static int mediaProjectionPermissionResultCode;
      private boolean isSwappedFeeds; // True -> 로컬뷰풀스크린
      private com.uminoh.bulnati.CallFragment callFragment; //콜 아래 바

    //----------------------------------------------------------------------------------------------
    //온크리에이트

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    // 화면 풀스크린
    requestWindowFeature(Window.FEATURE_NO_TITLE);
    getWindow().addFlags(LayoutParams.FLAG_FULLSCREEN | LayoutParams.FLAG_KEEP_SCREEN_ON
        | LayoutParams.FLAG_DISMISS_KEYGUARD);
    getWindow().getDecorView().setSystemUiVisibility(getSystemUiVisibility());
    setContentView(com.uminoh.bulnati.R.layout.activity_call);

    //값 초기화
    iceConnected = false;
    signalingParameters = null;

    // 구성요소 XML 연결
    pipRenderer = findViewById(com.uminoh.bulnati.R.id.pip_video_view); //본인화면(우)
    fullscreenRenderer = findViewById(com.uminoh.bulnati.R.id.fullscreen_video_view); //상대화면(좌)
    callFragment = new com.uminoh.bulnati.CallFragment();

    // 본인화면(우)을 누르면 각 화면이 전환됨
    pipRenderer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        setSwappedFeeds(!isSwappedFeeds);
      }
    });

    // 상대화면(좌)을 누르면 통화버튼부분 쇼앤하이드됨
    fullscreenRenderer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        toggleCallControlFragmentVisibility();
      }
    });

    //리스트에 프록시상대화면 넣음
    remoteRenderers.add(remoteProxyRenderer);

    final Intent intent = getIntent();

    // 비디오렌더러를 만들자
    rootEglBase = EglBase.create();

    //본인화면(우)
    pipRenderer.init(rootEglBase.getEglBaseContext(), null);
    pipRenderer.setScalingType(ScalingType.SCALE_ASPECT_FIT);
    String saveRemoteVideoToFile = intent.getStringExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE);
   if (saveRemoteVideoToFile != null) {
      int videoOutWidth = intent.getIntExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_WIDTH, 0);
      int videoOutHeight = intent.getIntExtra(ConstantsRtc.EXTRA_SAVE_REMOTE_VIDEO_TO_FILE_HEIGHT, 0);
      try {
        videoFileRenderer = new VideoFileRenderer(
            saveRemoteVideoToFile, videoOutWidth, videoOutHeight, rootEglBase.getEglBaseContext());

        //비디오파일렌더러는 비디오렌더러콜백을 임플리먼트함 / 본인화면(우)
        remoteRenderers.add(videoFileRenderer);

      } catch (IOException e) {
        throw new RuntimeException(
            "Failed to open video file for output: " + saveRemoteVideoToFile, e);
      }
    }

    //상대화면(좌)
    fullscreenRenderer.init(rootEglBase.getEglBaseContext(), null);
    fullscreenRenderer.setScalingType(ScalingType.SCALE_ASPECT_FILL);

    pipRenderer.setZOrderMediaOverlay(true);
    pipRenderer.setEnableHardwareScaler(true /* enabled */);
    fullscreenRenderer.setEnableHardwareScaler(true /* enabled */);

    //로컬 상대화면(좌)로 시작하는데, 전화연결되면 본인화면(우)로 바뀜
    setSwappedFeeds(true /* isSwappedFeeds */);

    // 퍼미션 체크하고 권한 목록에 없으면 백
    for (String permission : MANDATORY_PERMISSIONS) {
      if (checkCallingOrSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
        logAndToast("Permission " + permission + " is not granted");
        setResult(RESULT_CANCELED);
        finish();
        return;
      }
    }

    //세팅차례대로
    //uri 가져오고
    Uri roomUri = intent.getData();
    if (roomUri == null) {
      logAndToast(getString(com.uminoh.bulnati.R.string.missing_url));
      Log.e(TAG, "Didn't get any URL in intent!");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    //방주소 가져오고
    String roomId = intent.getStringExtra(ConstantsRtc.EXTRA_ROOMID);
    Log.d(TAG, "Room ID: " + roomId);
    if (roomId == null || roomId.length() == 0) {
      logAndToast(getString(com.uminoh.bulnati.R.string.missing_url));
      Log.e(TAG, "Incorrect room ID in intent!");
      setResult(RESULT_CANCELED);
      finish();
      return;
    }

    //가져올 것들 다 가져와
    boolean loopback = intent.getBooleanExtra(ConstantsRtc.EXTRA_LOOPBACK, false);
    commandLineRun = intent.getBooleanExtra(ConstantsRtc.EXTRA_CMDLINE, false);
    runTimeMs = intent.getIntExtra(ConstantsRtc.EXTRA_RUNTIME, 0);

    boolean tracing = intent.getBooleanExtra(ConstantsRtc.EXTRA_TRACING, false);
    int videoWidth = intent.getIntExtra(ConstantsRtc.EXTRA_VIDEO_WIDTH, 0);
    int videoHeight = intent.getIntExtra(ConstantsRtc.EXTRA_VIDEO_HEIGHT, 0);

    screencaptureEnabled = intent.getBooleanExtra(ConstantsRtc.EXTRA_SCREENCAPTURE, false);
    if (screencaptureEnabled && videoWidth == 0 && videoHeight == 0) {
      DisplayMetrics displayMetrics = getDisplayMetrics();
      videoWidth = displayMetrics.widthPixels;
      videoHeight = displayMetrics.heightPixels;
    }
    PeerConnectionClient.DataChannelParameters dataChannelParameters = null;
    if (intent.getBooleanExtra(ConstantsRtc.EXTRA_DATA_CHANNEL_ENABLED, false)) {
      dataChannelParameters = new PeerConnectionClient.DataChannelParameters(intent.getBooleanExtra(ConstantsRtc.EXTRA_ORDERED, true),
          intent.getIntExtra(ConstantsRtc.EXTRA_MAX_RETRANSMITS_MS, -1),
          intent.getIntExtra(ConstantsRtc.EXTRA_MAX_RETRANSMITS, -1), intent.getStringExtra(ConstantsRtc.EXTRA_PROTOCOL),
          intent.getBooleanExtra(ConstantsRtc.EXTRA_NEGOTIATED, false), intent.getIntExtra(ConstantsRtc.EXTRA_ID, -1));
    }
    peerConnectionParameters =
        new PeerConnectionClient.PeerConnectionParameters(intent.getBooleanExtra(ConstantsRtc.EXTRA_VIDEO_CALL, true), loopback,
            tracing, videoWidth, videoHeight, intent.getIntExtra(ConstantsRtc.EXTRA_VIDEO_FPS, 0),
            intent.getIntExtra(ConstantsRtc.EXTRA_VIDEO_BITRATE, 0), intent.getStringExtra(ConstantsRtc.EXTRA_VIDEOCODEC),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_HWCODEC_ENABLED, true),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_FLEXFEC_ENABLED, false),
            intent.getIntExtra(ConstantsRtc.EXTRA_AUDIO_BITRATE, 0), intent.getStringExtra(ConstantsRtc.EXTRA_AUDIOCODEC),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_NOAUDIOPROCESSING_ENABLED, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_AECDUMP_ENABLED, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_OPENSLES_ENABLED, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AEC, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_AGC, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_DISABLE_BUILT_IN_NS, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_ENABLE_LEVEL_CONTROL, false),
            intent.getBooleanExtra(ConstantsRtc.EXTRA_DISABLE_WEBRTC_AGC_AND_HPF, false), dataChannelParameters);
    Log.d(TAG, "VIDEO_FILE: '" + intent.getStringExtra(ConstantsRtc.EXTRA_VIDEO_FILE_AS_CAMERA) + "'");

    //룸네임이 IP이 아니어야 웹소켓로 연결!
    if (loopback || !WebSocketRTCClient.IP_PATTERN.matcher(roomId).matches()) {
      appRtcClient = new WebSocketRTCClient(this);
    }

    // 룸연결변수들을 가져옴
    String urlParameters = intent.getStringExtra(ConstantsRtc.EXTRA_URLPARAMETERS);
    roomConnectionParameters = new AppRTCClient.RoomConnectionParameters(roomUri.toString(), roomId, loopback, urlParameters);

    // 프래그먼트에 인텐트를 보냄
    callFragment.setArguments(intent.getExtras());

    // 각 공간에 프래그먼트를 매칭함
    FragmentTransaction ft = getFragmentManager().beginTransaction();
    ft.add(com.uminoh.bulnati.R.id.call_fragment_container, callFragment);
    ft.commit();

    // 커맨드라인런과 런타임을 통해 종료시킴
    if (commandLineRun && runTimeMs > 0) {
      (new Handler()).postDelayed(new Runnable() {
        @Override
        public void run() {
          disconnect();
        }
      }, runTimeMs);
    }

    // 클라이언트 등장
    peerConnectionClient = PeerConnectionClient.getInstance();
    if (loopback) {
      PeerConnectionFactory.Options options = new PeerConnectionFactory.Options();
      options.networkIgnoreMask = 0;
      peerConnectionClient.setPeerConnectionFactoryOptions(options);
    }
    peerConnectionClient.createPeerConnectionFactory(
        getApplicationContext(), peerConnectionParameters, CallActivity.this);

    if (screencaptureEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      startScreenCapture();
    } else {
      startCall();
    }
  }

    //----------------------------------------------------------------------------------------------
    //Api 별 설정

  @TargetApi(17)
  private DisplayMetrics getDisplayMetrics() {
    DisplayMetrics displayMetrics = new DisplayMetrics();
    WindowManager windowManager =
        (WindowManager) getApplication().getSystemService(Context.WINDOW_SERVICE);
    windowManager.getDefaultDisplay().getRealMetrics(displayMetrics);
    return displayMetrics;
  }

  @TargetApi(19)
  private static int getSystemUiVisibility() {
    int flags = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_FULLSCREEN;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
      flags |= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
    }
    return flags;
  }

  @TargetApi(21)
  private void startScreenCapture() {
    MediaProjectionManager mediaProjectionManager =
        (MediaProjectionManager) getApplication().getSystemService(
            Context.MEDIA_PROJECTION_SERVICE);
    startActivityForResult(
        mediaProjectionManager.createScreenCaptureIntent(), CAPTURE_PERMISSION_REQUEST_CODE);
  }

  //21버전 포리저트
  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode != CAPTURE_PERMISSION_REQUEST_CODE)
      return;
    mediaProjectionPermissionResultCode = resultCode;
    mediaProjectionPermissionResultData = data;
    startCall();
  }

  private boolean useCamera2() {
    return Camera2Enumerator.isSupported(this) && getIntent().getBooleanExtra(ConstantsRtc.EXTRA_CAMERA2, true);
  }

  private boolean captureToTexture() {
    return getIntent().getBooleanExtra(ConstantsRtc.EXTRA_CAPTURETOTEXTURE_ENABLED, false);
  }

  private VideoCapturer createCameraCapturer(CameraEnumerator enumerator) {
    final String[] deviceNames = enumerator.getDeviceNames();

    // First, try to find front facing camera
    Logging.d(TAG, "Looking for front facing cameras.");
    for (String deviceName : deviceNames) {
      if (enumerator.isFrontFacing(deviceName)) {
        Logging.d(TAG, "Creating front facing camera capturer.");
        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

        if (videoCapturer != null) {
          return videoCapturer;
        }
      }
    }

    // Front facing camera not found, try something else
    Logging.d(TAG, "Looking for other cameras.");
    for (String deviceName : deviceNames) {
      if (!enumerator.isFrontFacing(deviceName)) {
        Logging.d(TAG, "Creating other camera capturer.");
        VideoCapturer videoCapturer = enumerator.createCapturer(deviceName, null);

        if (videoCapturer != null) {
          return videoCapturer;
        }
      }
    }

    return null;
  }

  @TargetApi(21)
  private VideoCapturer createScreenCapturer() {
    if (mediaProjectionPermissionResultCode != Activity.RESULT_OK) {
      reportError("User didn't give permission to capture the screen.");
      return null;
    }
    return new ScreenCapturerAndroid(
        mediaProjectionPermissionResultData, new MediaProjection.Callback() {
      @Override
      public void onStop() {
        reportError("User revoked permission to capture the screen.");
      }
    });
  }

  // Activity interfaces
  @Override
  public void onStop() {
    super.onStop();
    activityRunning = false;
    if (peerConnectionClient != null && !screencaptureEnabled) {
      peerConnectionClient.stopVideoSource();
    }
  }

  @Override
  public void onStart() {
    super.onStart();
    activityRunning = true;
    if (peerConnectionClient != null && !screencaptureEnabled) {
      peerConnectionClient.startVideoSource();
    }
  }

  @Override
  protected void onDestroy() {
    Thread.setDefaultUncaughtExceptionHandler(null);
    disconnect();
    if (logToast != null) {
      logToast.cancel();
    }
    activityRunning = false;
    rootEglBase.release();
    super.onDestroy();
  }

  // CallFragment.OnCallEvents interface implementation.
  @Override
  public void onCallHangUp() {
    disconnect();
  }

  @Override
  public void onCameraSwitch() {
    if (peerConnectionClient != null) {
      peerConnectionClient.switchCamera();
    }
  }

  @Override
  public boolean onToggleMic() {
    if (peerConnectionClient != null) {
      micEnabled = !micEnabled;
      peerConnectionClient.setAudioEnabled(micEnabled);
    }
    return micEnabled;
  }

  // Helper functions.
  private void toggleCallControlFragmentVisibility() {
    if (!iceConnected || !callFragment.isAdded()) {
      return;
    }
    // Show/hide call control fragment
    callControlFragmentVisible = !callControlFragmentVisible;
    FragmentTransaction ft = getFragmentManager().beginTransaction();
    if (callControlFragmentVisible) {
      ft.show(callFragment);
//      ft.show(hudFragment);
    } else {
      ft.hide(callFragment);
//      ft.hide(hudFragment);
    }
    ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
    ft.commit();
  }

  private void startCall() {
    if (appRtcClient == null) {
      Log.e(TAG, "AppRTC client is not allocated for a call.");
      return;
    }
    callStartedTimeMs = System.currentTimeMillis();

    // Start room connection.
    StyleableToast.makeText(CallActivity.this,
            "연결을 시작합니다.",
            Toast.LENGTH_SHORT).show();
    appRtcClient.connectToRoom(roomConnectionParameters);

    // Create and audio manager that will take care of audio routing,
    // audio modes, audio device enumeration etc.
    audioManager = AppRTCAudioManager.create(getApplicationContext());
    // Store existing audio settings and change audio mode to
    // MODE_IN_COMMUNICATION for best possible VoIP performance.
    Log.d(TAG, "Starting the audio manager...");
    audioManager.start(new AppRTCAudioManager.AudioManagerEvents() {
      // This method will be called each time the number of available audio
      // devices has changed.
      @Override
      public void onAudioDeviceChanged(
              AppRTCAudioManager.AudioDevice audioDevice, Set<AppRTCAudioManager.AudioDevice> availableAudioDevices) {
        onAudioManagerDevicesChanged(audioDevice, availableAudioDevices);
      }
    });
  }

  // Should be called from UI thread
  private void callConnected() {
    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    Log.i(TAG, "Call connected: delay=" + delta + "ms");
    if (peerConnectionClient == null || isError) {
      Log.w(TAG, "Call is connected in closed or error state");
      return;
    }
    // Enable statistics callback.
    peerConnectionClient.enableStatsEvents(true, STAT_CALLBACK_PERIOD);
    setSwappedFeeds(false /* isSwappedFeeds */);
  }

  // This method is called when the audio manager reports audio device change,
  // e.g. from wired headset to speakerphone.
  private void onAudioManagerDevicesChanged(
          final AppRTCAudioManager.AudioDevice device, final Set<AppRTCAudioManager.AudioDevice> availableDevices) {
    Log.d(TAG, "onAudioManagerDevicesChanged: " + availableDevices + ", "
            + "selected: " + device);
    // TODO(henrika): add callback handler.
  }

  // Disconnect from remote resources, dispose of local resources, and exit.
  private void disconnect() {
    activityRunning = false;
    remoteProxyRenderer.setTarget(null);
    localProxyRenderer.setTarget(null);
    if (appRtcClient != null) {
      appRtcClient.disconnectFromRoom();
      appRtcClient = null;
    }
    if (peerConnectionClient != null) {
      peerConnectionClient.close();
      peerConnectionClient = null;
    }
    if (pipRenderer != null) {
      pipRenderer.release();
      pipRenderer = null;
    }
    if (videoFileRenderer != null) {
      videoFileRenderer.release();
      videoFileRenderer = null;
    }
    if (fullscreenRenderer != null) {
      fullscreenRenderer.release();
      fullscreenRenderer = null;
    }
    if (audioManager != null) {
      audioManager.stop();
      audioManager = null;
    }
    if (iceConnected && !isError) {
      setResult(RESULT_OK);
    } else {
      setResult(RESULT_CANCELED);
    }
    finish();
  }

  private void disconnectWithErrorMessage(final String errorMessage) {
    if (commandLineRun || !activityRunning) {
      Log.e(TAG, "Critical error: " + errorMessage);
      disconnect();
    } else {
      new AlertDialog.Builder(this)
          .setTitle(getText(com.uminoh.bulnati.R.string.channel_error_title))
          .setMessage(errorMessage)
          .setCancelable(false)
          .setNeutralButton(com.uminoh.bulnati.R.string.ok,
              new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int id) {
                  dialog.cancel();
                  disconnect();
                }
              })
          .create()
          .show();
    }
  }

  // Log |msg| and Toast about it.
  private void logAndToast(String msg) {
    Log.d(TAG, msg);
    if (logToast != null) {
      logToast.cancel();
    }
    logToast = Toast.makeText(this, msg, Toast.LENGTH_LONG);
    logToast.show();
  }

  private void reportError(final String description) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!isError) {
          isError = true;
          disconnectWithErrorMessage(description);
        }
      }
    });
  }

  private VideoCapturer createVideoCapturer() {
    VideoCapturer videoCapturer = null;
    String videoFileAsCamera = getIntent().getStringExtra(ConstantsRtc.EXTRA_VIDEO_FILE_AS_CAMERA);
    if (videoFileAsCamera != null) {
      try {
        videoCapturer = new FileVideoCapturer(videoFileAsCamera);
      } catch (IOException e) {
        reportError("Failed to open video file for emulated camera");
        return null;
      }
    } else if (screencaptureEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      return createScreenCapturer();
    } else if (useCamera2()) {
      if (!captureToTexture()) {
        reportError(getString(com.uminoh.bulnati.R.string.camera2_texture_only_error));
        return null;
      }

      Logging.d(TAG, "Creating capturer using camera2 API.");
      videoCapturer = createCameraCapturer(new Camera2Enumerator(this));
    } else {
      Logging.d(TAG, "Creating capturer using camera1 API.");
      videoCapturer = createCameraCapturer(new Camera1Enumerator(captureToTexture()));
    }
    if (videoCapturer == null) {
      reportError("Failed to open camera");
      return null;
    }
    return videoCapturer;
  }

  private void setSwappedFeeds(boolean isSwappedFeeds) {
    Logging.d(TAG, "setSwappedFeeds: " + isSwappedFeeds);
    this.isSwappedFeeds = isSwappedFeeds;
    localProxyRenderer.setTarget(isSwappedFeeds ? fullscreenRenderer : pipRenderer);
    remoteProxyRenderer.setTarget(isSwappedFeeds ? pipRenderer : fullscreenRenderer);
    fullscreenRenderer.setMirror(isSwappedFeeds);
    pipRenderer.setMirror(!isSwappedFeeds);
  }

  // -----Implementation of AppRTCClient.AppRTCSignalingEvents ---------------
  // All callbacks are invoked from websocket signaling looper thread and
  // are routed to UI thread.
  private void onConnectedToRoomInternal(final AppRTCClient.SignalingParameters params) {
    final long delta = System.currentTimeMillis() - callStartedTimeMs;

    signalingParameters = params;
    StyleableToast.makeText(CallActivity.this,
            "상대방과 연결을 준비합니다.(걸린시간:"+delta/1000+"."+delta%1000+"초)",
            Toast.LENGTH_SHORT).show();
    VideoCapturer videoCapturer = null;
    if (peerConnectionParameters.videoCallEnabled) {
      videoCapturer = createVideoCapturer();
    }
    peerConnectionClient.createPeerConnection(rootEglBase.getEglBaseContext(), localProxyRenderer,
        remoteRenderers, videoCapturer, signalingParameters);

    if (signalingParameters.initiator) {
      StyleableToast.makeText(CallActivity.this,
              "연결중...",
              Toast.LENGTH_SHORT).show();
      // Create offer. Offer SDP will be sent to answering client in
      // PeerConnectionEvents.onLocalDescription event.
      peerConnectionClient.createOffer();
    } else {
      if (params.offerSdp != null) {
        peerConnectionClient.setRemoteDescription(params.offerSdp);
        StyleableToast.makeText(CallActivity.this,
                "상대방 확인하는 중...",
                Toast.LENGTH_SHORT).show();
        // Create answer. Answer SDP will be sent to offering client in
        // PeerConnectionEvents.onLocalDescription event.
        peerConnectionClient.createAnswer();
      }
      if (params.iceCandidates != null) {
        // Add remote ICE candidates from room.
        for (IceCandidate iceCandidate : params.iceCandidates) {
          peerConnectionClient.addRemoteIceCandidate(iceCandidate);
        }
      }
    }
  }

  @Override
  public void onConnectedToRoom(final AppRTCClient.SignalingParameters params) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        onConnectedToRoomInternal(params);
      }
    });
  }

  @Override
  public void onRemoteDescription(final SessionDescription sdp) {
    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received remote SDP for non-initilized peer connection.");
          return;
        }
        StyleableToast.makeText(CallActivity.this,
                "상대방 연결까지 대기시간 ("+delta/1000+"."+delta%1000+"초)",
                Toast.LENGTH_SHORT).show();
        peerConnectionClient.setRemoteDescription(sdp);
        if (!signalingParameters.initiator) {
          logAndToast("Creating ANSWER...");
          // Create answer. Answer SDP will be sent to offering client in
          // PeerConnectionEvents.onLocalDescription event.
          peerConnectionClient.createAnswer();
        }
      }
    });
  }

  @Override
  public void onRemoteIceCandidate(final IceCandidate candidate) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received ICE candidate for a non-initialized peer connection.");
          return;
        }
        peerConnectionClient.addRemoteIceCandidate(candidate);
      }
    });
  }

  @Override
  public void onRemoteIceCandidatesRemoved(final IceCandidate[] candidates) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (peerConnectionClient == null) {
          Log.e(TAG, "Received ICE candidate removals for a non-initialized peer connection.");
          return;
        }
        peerConnectionClient.removeRemoteIceCandidates(candidates);
      }
    });
  }

  @Override
  public void onChannelClose() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        StyleableToast.makeText(CallActivity.this,
                "상대방이 영상통화를 종료하였습니다!",
                Toast.LENGTH_SHORT).show();
        disconnect();
      }
    });
  }

  @Override
  public void onChannelError(final String description) {
    reportError(description);
  }

  // -----Implementation of PeerConnectionClient.PeerConnectionEvents.---------
  // Send local peer connection SDP and ICE candidates to remote party.
  // All callbacks are invoked from peer connection client looper thread and
  // are routed to UI thread.
  @Override
  public void onLocalDescription(final SessionDescription sdp) {
    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          StyleableToast.makeText(CallActivity.this,
                  "카메라 준비되었습니다. (걸린시간:"+delta/1000+"."+delta%1000+"초)",
                  Toast.LENGTH_SHORT).show();
          if (signalingParameters.initiator) {
            appRtcClient.sendOfferSdp(sdp);
          } else {
            appRtcClient.sendAnswerSdp(sdp);
          }
        }
        if (peerConnectionParameters.videoMaxBitrate > 0) {
          Log.d(TAG, "Set video maximum bitrate: " + peerConnectionParameters.videoMaxBitrate);
          peerConnectionClient.setVideoMaxBitrate(peerConnectionParameters.videoMaxBitrate);
        }
      }
    });
  }

  @Override
  public void onIceCandidate(final IceCandidate candidate) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          appRtcClient.sendLocalIceCandidate(candidate);
        }
      }
    });
  }

  @Override
  public void onIceCandidatesRemoved(final IceCandidate[] candidates) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (appRtcClient != null) {
          appRtcClient.sendLocalIceCandidateRemovals(candidates);
        }
      }
    });
  }

  @Override
  public void onIceConnected() {
    final long delta = System.currentTimeMillis() - callStartedTimeMs;
    runOnUiThread(new Runnable() {
      @Override
      public void run() {

        StyleableToast.makeText(CallActivity.this,
                "연결되었습니다. (최종대기시간:"+delta/1000+"."+delta%1000+"초)",
                Toast.LENGTH_SHORT).show();

        iceConnected = true;
        callConnected();
      }
    });
  }

  @Override
  public void onIceDisconnected() {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        StyleableToast.makeText(CallActivity.this,
                "연결이 끊어졌습니다.",
                Toast.LENGTH_SHORT).show();
        logAndToast("ICE disconnected");
        iceConnected = false;
        disconnect();
      }
    });
  }

  @Override
  public void onPeerConnectionClosed() {}

  @Override
  public void onPeerConnectionStatsReady(final StatsReport[] reports) {
    runOnUiThread(new Runnable() {
      @Override
      public void run() {
        if (!isError && iceConnected) {
        }
      }
    });
  }

  @Override
  public void onPeerConnectionError(final String description) {
    reportError(description);
  }
}
