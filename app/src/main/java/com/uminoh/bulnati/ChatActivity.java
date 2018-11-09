package com.uminoh.bulnati;

import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.NestedScrollView;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.OrientationHelper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
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

public class ChatActivity extends AppCompatActivity implements View.OnClickListener, AdapterRecyclerChat.MyChatRecyclerViewClickListener{

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
    ImageButton exitChat;
    ImageButton userListButton;
    Button sendButton;
    EditText mMessageEditText;

    //기본 구성요소 (룸네임, 현재 시간)
    String room_week;
    String room_name;
    String date_str;
    boolean ent_key;

    //리사이클러뷰 (어댑터, 리스트, 리사이클러뷰)
    private AdapterRecyclerChat mAdapter;
    private List<DataChat> mChatDataList;
    RecyclerView recyclerView;
    //NestedScrollView nestView;

    //페이징 (페이징 변수, 한 번에 로드할 페이지 수)
    private int page = 0;
    private final int OFFSET = 15;

    //List Dialog Adapter
    ArrayAdapter<String> adapter;

    //메시지 보내기 (소켓, 보내기쓰레드, 보내는 메시지창)
    private Socket mSocket = null;
    private SenderThread mThread1;

    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String getNick = intent.getStringExtra("nick");
            String getRoom = intent.getStringExtra("room");
            String getMsg = intent.getStringExtra("msg");
            Log.e("닉:"+getNick+"/메:"+getMsg+"/룸:"+getRoom, "챗");

            //필터링하지 않아도 됨, if(room_name.contains(getRoom))
            onChatStr(getMsg, getNick, date_str, -1);
        }
    };

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
        exitChat = findViewById(R.id.exit_chat);
        sendButton = findViewById(R.id.send_button);
        userListButton = findViewById(R.id.user_list_button);

        //요소 세팅 (룸네임겟, 현재시간, 풀투리프레시, 룸네임입력, 센드클릭리스너, 노티표시, 노티클릭리스너)
        room_name = getIntent().getStringExtra("program");
        ent_key = getIntent().getBooleanExtra("ent",false);
        room_week = getIntent().getStringExtra("week");

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

        if(room_week!=null){
            chat_room_id.setText("["+room_week.replace("요일예능","")+"] "+room_name);
        } else {
            chat_room_id.setText(room_name);
        }

        sendButton.setOnClickListener(this);

        //리사이클러뷰 연결
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this, OrientationHelper.VERTICAL, false);
        mChatDataList = new ArrayList<>();
        mAdapter = new AdapterRecyclerChat(getApplicationContext(), mChatDataList);
        mAdapter.setOnClickListener(this);

        //nestView = findViewById(R.id.nest_view2);

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
                    Log.e(mSocket.toString(),"센더소켓시작");
                    // 두번째 파라메터 로는 본인의 닉네임을 적어줍니다.
                    mThread1 = new SenderThread(mSocket, login_nick+">센더");
                    mThread1.start();

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        if(ent_key){

            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);
            // 제목셋팅
            alertDialogBuilder.setTitle("알림");
            // AlertDialog 셋팅
            alertDialogBuilder
                    .setMessage("비속어, 비방, 정치발언, 장애인비하, 도배금지 등 채팅 상식에 어긋난 행동은 삼가해주시길 바랍니다!")
                    .setCancelable(false)
                    .setPositiveButton("확인",
                            new DialogInterface.OnClickListener() {
                                public void onClick(
                                        DialogInterface dialog, int id) {

                                    if(mThread1 != null) {
                                        onChatStr("입장", login_nick, date_str, -1);
                                        mThread1.sendMessage("입장", room_name);
                                        Call<ResponseBody> save_chat = apiService.saveChat("입장", login_nick, date_str, room_name);
                                        save_chat.enqueue(new Callback<ResponseBody>() {
                                            @Override
                                            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {

                                            }

                                            @Override
                                            public void onFailure(Call<ResponseBody> call, Throwable t) {
                                            }
                                        });
                                    } else {
                                        StyleableToast.makeText(ChatActivity.this, "서버가 불안정합니다! 다시 접속해주세요!", Toast.LENGTH_SHORT, R.style.mytoast).show();
                                        finish();
                                    }
                                }
                            });

            // 다이얼로그 생성
            AlertDialog alertDialog = alertDialogBuilder.create();

            // 다이얼로그 보여주기
            alertDialog.show();

        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("blackJinData"));

        exitChat.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                AlertDialog.Builder alertDialogBuilder2 = new AlertDialog.Builder(ChatActivity.this);
                // 제목셋팅
                alertDialogBuilder2.setTitle("경고");
                // AlertDialog 셋팅
                alertDialogBuilder2
                        .setMessage("퇴장하면 채팅 내역을 볼 수 없습니다!")
                        .setCancelable(false)
                        .setPositiveButton("퇴장",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialog, int id) {

                                        if(mThread1 != null){
                                            Call<ResponseBody> exit_chat = apiService.exitChat(room_name, login_nick, date_str);
                                            exit_chat.enqueue(new Callback<ResponseBody>() {
                                                @Override
                                                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                                                    try {
                                                        String str = response.body().string();
                                                        Log.e("exit",str);
                                                        if(str.equals("success")){

                                                            mThread1.sendMessage("퇴장",room_name);
                                                            lEdit.putString("room_list",lp.getString("room_list","").replace("/"+room_name,""));
                                                            lEdit.apply();
                                                            onChatStr("퇴장",login_nick ,date_str, -1);
                                                            onBackPressed();

                                                        }
                                                    } catch (IOException e) {
                                                        e.printStackTrace();
                                                    }
                                                }

                                                @Override
                                                public void onFailure(Call<ResponseBody> call, Throwable t) {

                                                }
                                            });
                                        } else {
                                            StyleableToast.makeText(ChatActivity.this, "서버가 불안정합니다! 다시 접속해주세요!", Toast.LENGTH_SHORT, R.style.mytoast).show();
                                            finish();
                                        }

                                    }
                                })
                        .setNegativeButton("보류",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(
                                            DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                });

                // 다이얼로그 생성
                AlertDialog alertDialog2 = alertDialogBuilder2.create();

                // 다이얼로그 보여주기
                alertDialog2.show();

            }
        });

        exitChat.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Call<ResponseBody> get_all_room_user = apiService.getAllRoomUser();
                get_all_room_user.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String str = response.body().string();
                            mThread1.sendMessage(str, "룸셋");
                            Log.e("룸셋",str);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {

                    }
                });
                return false;
            }
        });

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1);

        userListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                adapter.clear();
                Call<ResponseBody> get_user_by_room = apiService.getUserByRoom(room_name);
                get_user_by_room.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String str = response.body().string();
                            Log.e("유저리스트",str);
                            String[] users = str.split("/");

                            if(users.length != 0 ){
                                for (int k = 0 ; k < users.length ; k++){
                                    if(users[k].equals(login_nick)){
                                        adapter.add(users[k]+" (★본인★)");
                                    } else {
                                        adapter.add(users[k]);
                                    }
                                }
                            } else {
                                adapter.add("유저 없음");
                            }

                            adapter.notifyDataSetChanged();

                            final AlertDialog.Builder alert = new AlertDialog.Builder(ChatActivity.this);
                            alert.setTitle("유저리스트 ("+users.length+"명, 첫번째부터 입장순)");     //타이틀
                            alert.setIcon(R.drawable.ic_people_black_24dp); //아이콘
                            alert.setAdapter(adapter, new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                  Log.e("유저선택",adapter.getItem(i));
                                  dialogInterface.dismiss();
                                }
                            });
                            alert.setNegativeButton("뒤로", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    dialogInterface.dismiss();
                                }
                            });

                            alert.show();

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

    }

    //----------------------------------------------------------------------------------------------
    //레트로핏 API : 메시지 보내기

    @Override
    public void onClick(View v) {

        //메시지 가져오기
        String msg = mMessageEditText.getText().toString();

//        //채팅방 표현
//        onChatStr(msg,login_nick ,date_str, -1);

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
            onChatStr(mMessageEditText.getText().toString(),login_nick ,date_str, -1);
            mThread1.sendMessage(mMessageEditText.getText().toString(),room_name);
            mMessageEditText.setText("");
        } else {
            StyleableToast.makeText(this, "메시지가 비었습니다", Toast.LENGTH_SHORT, R.style.mytoast).show();
        }
    }

    //----------------------------------------------------------------------------------------------
    //채팅화면 표시

    public void onChatStr(final String msg, final String nick, final String date, final int i) {

        if(!msg.equals("")){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                int type;
                if(login_nick.equals(nick)){
                    type = DataChat.ME_TYPE;
                } else {
                    if(msg.equals("입장") || msg.equals("퇴장")){
                        type = DataChat.ENTRY_TYPE;
                    } else {
                        type = DataChat.YOU_TYPE;
                    }
                }

                if( i == -1 ){
                    mChatDataList.add(new DataChat(type,msg,nick,date));
                } else {
                    mChatDataList.add(i,new DataChat(type,msg,nick,date));
                }

                mAdapter.notifyDataSetChanged();
                recyclerView.scrollToPosition(mAdapter.getItemCount()-1);
                //nestView.fullScroll(View.FOCUS_DOWN);
                //nestView.scrollTo(0,0)
                }
            });
        }

    }

    //----------------------------------------------------------------------------------------------
    //화면 종료

    @Override
    protected void onDestroy() {
        super.onDestroy();

        try {
            if(mSocket != null){
                mSocket.close();
                Log.e(mSocket.toString(),"채팅방소켓종료");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        if(mThread1 != null){
            mThread1.close();
        }

        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);

    }

    //----------------------------------------------------------------------------------------------
    //백버튼 서비스종료

    public void back_chat_room(View view) {
        onBackPressed();
    }

    //----------------------------------------------------------------------------------------------
    //페이징 (*개선필요:DB 처음부터 알맞게 나오는 것으로!)

    private void getPaging(final String s){

        Call<ResponseBody> load_chat = apiService.loadChat(room_name, login_nick);
        load_chat.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {

                    int start_num = 0;
                    int end_num = 0;
                    int chat_num = 0;

                    String str = response.body().string();
//                    Log.e("리절틑로로로",str);
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


    @Override
    public void onBackPressed() {
        super.onBackPressed();

        lEdit.putInt(room_name, 0);
        lEdit.apply();
    }
}



