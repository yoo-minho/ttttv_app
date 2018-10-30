package com.uminoh.bulnati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.RecyclerUtil.AdapterRecyclerChat;
import com.uminoh.bulnati.RecyclerUtil.DataChat;
import com.uminoh.bulnati.TcpUtil.ReceiverThread;
import com.uminoh.bulnati.TcpUtil.SenderThread;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class ChatActivity extends AppCompatActivity implements View.OnClickListener, ReceiverThread.OnReceiveListener, AdapterRecyclerChat.MyChatRecyclerViewClickListener{

    //쉐어드프리퍼런스 (기본, 에딧, 룸리스트, 로그인닉, 로그인유무확인키)
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list = "";
    String login_nick = "";
    boolean login_key = false;

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;

    //뷰 구성요소 (풀투리프레시, 채팅방상단이름, 알림온, 오프, 센드에딧)
    SwipeRefreshLayout refreshLayout;
    TextView chat_room_id;
    ImageButton notiChatOn;
    ImageButton notiChatOff;
    Button sendButton;
    EditText mMessageEditText;

    //기본 구성요소 (룸네임, 현재 시간)
    String room_name;
    String date_str;

    //리사이클러뷰 (어댑터, 리스트, 리사이클러뷰)
    private AdapterRecyclerChat mAdapter;
    private List<DataChat> mChatDataList;
    RecyclerView recyclerView;

    //페이징 (페이징 변수, 한 번에 로드할 페이지 수)
    private int page = 0;
    private final int OFFSET = 15;

    //메시지 보내기 (소켓, 보내기쓰레드, 보내는 메시지창)
    private Socket mSocket = null;
    private SenderThread mThread1;

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        room_list = lp.getString("room_list","");
        login_nick = lp.getString("login_nick", "닉네임값없음");
        login_key = lp.getBoolean("login_key", false);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //요소 연결
        refreshLayout = findViewById(R.id.swipe_layout);
        chat_room_id = findViewById(R.id.chat_room_name);
        notiChatOn = findViewById(R.id.noti_chat_on); //알림 꺼져있음
        notiChatOff = findViewById(R.id.noti_chat_off); //알림 켜져있음
        sendButton = findViewById(R.id.send_button);

        //요소 세팅 (룸네임겟, 현재시간, 풀투리프레시, 룸네임입력, 센드클릭리스너, 노티표시, 노티클릭리스너)
        room_name = getIntent().getStringExtra("program");

        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyMM월 dd일HHmmssSSaa hh:mm", Locale.KOREA);
        Date currentTime = new Date();
        date_str = mSimpleDateFormat.format(currentTime);

        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                getPaging("next");
                refreshLayout.setRefreshing(false);
            }
        });
        refreshLayout.setColorSchemeResources(
                android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light
        );

        chat_room_id.setText(room_name);

        sendButton.setOnClickListener(this);

        if(room_list.contains(room_name)){
            notiChatOn.setVisibility(View.GONE);
            notiChatOff.setVisibility(View.VISIBLE);
        } else {
            notiChatOn.setVisibility(View.VISIBLE);
            notiChatOff.setVisibility(View.GONE);
        }

        //무조건 서비스 종료
        Intent intent = new Intent(getApplicationContext(),MyChatService.class);
        Log.e("MyChatService","STOP!!!");
        stopService(intent);

        notiChatOn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                notiChatOn.setVisibility(View.GONE);
                notiChatOff.setVisibility(View.VISIBLE);

                //추가
                if(!room_list.contains(room_name)){
                    lEdit.putString("room_list",room_list+"/"+room_name);
                    lEdit.commit();
                }

                StyleableToast.makeText(ChatActivity.this, "알림 설정!", Toast.LENGTH_SHORT,R.style.mytoast).show();
            }
        });

        notiChatOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                notiChatOn.setVisibility(View.VISIBLE);
                notiChatOff.setVisibility(View.GONE);

                //삭제
                if(room_list.contains(room_name)) {
                    lEdit.putString("room_list", room_list.replace("/" + room_name, ""));
                    lEdit.commit();
                }

                StyleableToast.makeText(ChatActivity.this, "알림 해제!", Toast.LENGTH_SHORT,R.style.mytoast).show();
            }
        });

        //리사이클러뷰 연결
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mChatDataList = new ArrayList<>();
        mAdapter = new AdapterRecyclerChat(getApplicationContext(), mChatDataList);
        mAdapter.setOnClickListener(this);
        recyclerView = findViewById(R.id.chat_recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        //페이징
        getPaging("first");
        recyclerView.scrollToPosition(mAdapter.getItemCount()-1);

        //메시지보내기
        mMessageEditText = findViewById(R.id.message_edit);
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    mSocket = new Socket(SecretKey.ip , SecretKey.port);

                    // 두번째 파라메터 로는 본인의 닉네임을 적어줍니다.
                    mThread1 = new SenderThread(mSocket, login_nick);
                    mThread1.start();

                    ReceiverThread thread2 = new ReceiverThread(mSocket);
                    thread2.setOnReceiveListener(ChatActivity.this);
                    thread2.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

    }

    //----------------------------------------------------------------------------------------------
    //레트로핏 API : 메시지 보내기

    @Override
    public void onClick(View v) {

        //메시지 가져오기
        String msg = mMessageEditText.getText().toString();

        //채팅방 표현
        onChatStr(msg,login_nick ,date_str, -1);

        //채팅내용 저장 및 전송
        if(!msg.equals("")){
            Call<ResponseBody> save_chat = apiService.saveChat(msg,login_nick,date_str,room_name);
            save_chat.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                }
                @Override
                public void onFailure(Call<ResponseBody> call, Throwable t) {
                }
            });
            mThread1.sendMessage(mMessageEditText.getText().toString(),room_name);
            mMessageEditText.setText("");
        } else {
            StyleableToast.makeText(this, "메시지가 비었습니다", Toast.LENGTH_SHORT, R.style.mytoast).show();
        }
    }

    //----------------------------------------------------------------------------------------------
    //리시버 쓰레드 : 메시지 받기

    @Override
    public void onReceive(final String message, String listener) {

        String[] split = message.split(">");

        if (split.length < 3) {
            Log.e("스플릿 길이 3보다 작음","gg");
            return;
        }

        String nickname = split[0];
        String msg = split[1].replace("%n", "\n");
        String room = split[2];

        //채팅화 : 룸 이름이 일치하거나, 닉네임이 같지 않을 때 (*개선필요:서버에서 나눠 오는 것으로!)
        if(room_name.equals(room) && !login_nick.equals(nickname)){
            onChatStr(msg, nickname, date_str, -1);
        }

    }

    //----------------------------------------------------------------------------------------------
    //채팅화면 표시

    public void onChatStr(final String msg, final String nick, final String date, final int i) {

        if(!msg.equals("")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                Boolean isMe = login_nick.equals(nick);
                if( i == -1 ){
                    mChatDataList.add(new DataChat(msg,nick,isMe,date));
                } else {
                    mChatDataList.add(i,new DataChat(msg,nick,isMe,date));
                }
                mAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(mAdapter.getItemCount()-1);
                }
            });
        }

    }

    //----------------------------------------------------------------------------------------------
    //화면 종료

    @Override
    protected void onDestroy() {
        try {
            mSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NullPointerException e){
            e.printStackTrace();
        }
        super.onDestroy();
    }

    //----------------------------------------------------------------------------------------------
    //리스트확인 서비스종료

    @Override
    protected void onRestart() {
        super.onRestart();

        room_list = lp.getString("room_list","");

        //룸리스트 확인 서비스 종료
        Intent intent = new Intent(getApplicationContext(),MyChatService.class);
        if(room_list.equals("")){
            Log.e("MyChatService","STOP!!!");
            stopService(intent);
        } else {
            Log.e("MyChatService","START!!!");
            startService(intent);
        }

    }

    //----------------------------------------------------------------------------------------------
    //백버튼 서비스종료

    public void back_chat_room(View view) {
        onBackPressed();
    }

    //----------------------------------------------------------------------------------------------
    //페이징 (*개선필요:DB 처음부터 알맞게 나오는 것으로!)

    private void getPaging(final String s){

        Call<ResponseBody> load_chat = apiService.loadChat(room_name);
        load_chat.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {

                    int start_num = 0;
                    int end_num = 0;
                    int chat_num = 0;

                    String str = response.body().string();
                    JSONArray jsonArray = new JSONArray(str);
                    if (jsonArray.length() - (OFFSET * (page + 1)) >= 0) {
                        start_num = jsonArray.length() - (OFFSET * (page + 1));
                    }
                    if (jsonArray.length() - (OFFSET * page) >= 0) {
                        end_num = jsonArray.length() - (OFFSET * page);
                    }
                    for (int j = start_num; j < end_num; j++) {
                        JSONObject jsonObject = jsonArray.getJSONObject(j);
                        String msg = jsonObject.getString("msg");
                        String nick = jsonObject.getString("nick");
                        String date = jsonObject.getString("date");
                        //차례대로 넣어줌
                        onChatStr(msg, nick, date, chat_num);
                        chat_num++;
                    }

                    page++;
                    mAdapter.notifyDataSetChanged();

                    if(s.equals("first")){
                        recyclerView.scrollToPosition(mAdapter.getItemCount()-1);
                    } else if (s.equals("next")) {
                        if(end_num == 0){
                            StyleableToast.makeText(ChatActivity.this, "가져올 채팅메시지가 없습니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
                            recyclerView.scrollToPosition(0);
                        } else if (end_num > 0 && end_num <= OFFSET) {
                            StyleableToast.makeText(ChatActivity.this, end_num+"개의 채팅메시지를 가져옵니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
                            recyclerView.scrollToPosition(end_num+8);
                        } else {
                            StyleableToast.makeText(ChatActivity.this, OFFSET+"개의 채팅메시지를 가져옵니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
                            recyclerView.scrollToPosition(OFFSET+8);
                        }
                    }

                } catch (IOException e) {
                    e.printStackTrace();
                } catch (JSONException e) {
                    e.printStackTrace();
                }

            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });

    }

    //----------------------------------------------------------------------------------------------
    //아이템클릭리스너

    @Override
    public void youItemBoxClicked(int i) {
        Toast.makeText(this, "추천기능 준비중", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void meItemBoxClicked(int i) {

    }

    //----------------------------------------------------------------------------------------------

}



