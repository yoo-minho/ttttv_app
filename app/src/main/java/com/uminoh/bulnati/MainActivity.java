package com.uminoh.bulnati;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.RecyclerUtil.AdapterRecyclerProgram;
import com.uminoh.bulnati.RecyclerUtil.DataProgram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MainActivity extends AppCompatActivity implements AdapterRecyclerProgram.MyProgramRecyclerViewClickListener{

    TextView reload;
    Document doc = null;
    Document doc2 = null;
    Button weekButton1;
    Button weekButton2;
    Button weekButton3;
    Button weekButton4;
    Button weekButton5;
    Button weekButton6;
    Button weekButton7;
    CardView chatRoomInfo;
    ListView listView = null;
    DrawerLayout drawer;
    ImageButton menuButton;

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String roomList;
    String nick;
    String notiOn;

    //날짜
    String today;
    String clickToday;

    //로컬브로드캐스트리시버
    BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String getNick = intent.getStringExtra("nick");
            String getRoom = intent.getStringExtra("room");
            String getMsg = intent.getStringExtra("msg");
//            Toast.makeText(getApplicationContext(), "<"+getRoom+">방에서 새메세지!", Toast.LENGTH_SHORT).show();

            for(int w = 0 ; w < dataList.size() ; w++){

                if(dataList.get(w).getProgramTitle().equals(getRoom)){
                    if(getMsg.equals("입장")){
                        dataList.get(w).setTotal(dataList.get(w).getTotal()+1);
                    } else if(getMsg.equals("퇴장")){
                        dataList.get(w).setTotal(dataList.get(w).getTotal()-1);
                    }

                    dataList.get(w).setMsgNew(dataList.get(w).getMsgNew()+1);

                    adapter.notifyItemChanged(w);
                }

            }

        }
    };

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;

    //아이템
    List<DataProgram> dataList; //아이템 리스트
    AdapterRecyclerProgram adapter; //리사이클러뷰 어댑터
    private List<String> imgUrl;
    private List<String> broadcastStation;
    private List<String> programTitle;
    private List<String> programTime;

    private List<String> programRating;
    private List<String> programIntro;

    //노티피케이션
    String room;
    int id = 0;

    String[] weekDay = { "일요일", "월요일", "화요일", "수요일", "목요일", "금요일", "토요일" };

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //뷰요소 연결
        reload = findViewById(R.id.reload);
        weekButton7 = findViewById(R.id.week_button7);
        weekButton1 = findViewById(R.id.week_button1);
        weekButton2 = findViewById(R.id.week_button2);
        weekButton3 = findViewById(R.id.week_button3);
        weekButton4 = findViewById(R.id.week_button4);
        weekButton5 = findViewById(R.id.week_button5);
        weekButton6 = findViewById(R.id.week_button6);
        chatRoomInfo = findViewById(R.id.chat_room_info);
        drawer = findViewById(R.id.drawer);
        menuButton = findViewById(R.id.menu_button);
        listView = findViewById(R.id.drawer_menulist) ;

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        nick = lp.getString("login_nick","");
        if(!lp.getBoolean("login_key",false)){
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
            finish();
        }

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //룸리스트를 db -> sp로
        onLoadRoomList();

        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                notiOn = lp.getString("noti_on","  - 메시지 알람 Off");

                //드롭레이아웃
                final String[] items = {"<"+nick+">"
                        , "  - 내 채팅방"
                        , "  - 랜덤 영상 통화 (1대1)"
                        , "  - 연예인 닮은꼴 찾기"
                        , notiOn
                        , "  - 문의하기"
                        , "  - 닉네임 변경"
                        , "  - 로그아웃"} ;

                ArrayAdapter baseAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, items) ;

                listView.setAdapter(baseAdapter) ;

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                        switch (i) {
                            case 0 : // 스페셜메뉴
                                break;
                            case 1 : // 내 채팅방
                                Intent i1 = new Intent(getApplicationContext(), NotiActivity.class);
                                startActivity(i1);
                                break ;
                            case 2 : // 랜덤영상채팅
                                Intent i2 = new Intent(getApplicationContext(), ConnectActivity.class);
                                startActivity(i2);
                                break;
                            case 3 : // 연예인 닮은꼴 찾기
                                Intent i3 = new Intent(getApplicationContext(), CfrActivity.class);
                                startActivity(i3);
                                break ;
                            case 4 : // 전체알람켜기 vs 전체알림끄기
                                if(notiOn.equals("  - 메시지 알람 On")){

                                    lEdit.putString("noti_on","  - 메시지 알람 Off");
                                    lEdit.apply();
                                    StyleableToast.makeText(MainActivity.this, "알람 해제!", Toast.LENGTH_SHORT,R.style.mytoast).show();

                                } else if (notiOn.equals("  - 메시지 알람 Off")){

                                    lEdit.putString("noti_on","  - 메시지 알람 On");
                                    lEdit.apply();
                                    StyleableToast.makeText(MainActivity.this, "알람 설정!", Toast.LENGTH_SHORT,R.style.mytoast).show();

                                }
                                break ;
                            case 5 : // 문의하기
                                Intent i5 = new Intent(Intent.ACTION_SEND);
                                i5.setType("plain/text");
                                String[] address = { "dellose@naver.com" } ;
                                i5.putExtra(Intent.EXTRA_EMAIL, address); //배열을 받으므로 위와 같이 선언해야합니다.
                                i5.putExtra(Intent.EXTRA_SUBJECT, "티티티티비에 이런 점을 문의합니다!");
                                startActivityForResult(i5, 9991);
                                break ;
                            case 6 : // 닉네임변경
                                Intent i6 = new Intent(getApplicationContext(), NickActivity.class);
                                startActivity(i6);
                                break ;
                            case 7 : // 로그아웃
                                logout();
                                break ;}
                        drawer.closeDrawer(Gravity.RIGHT) ;
                    }
                });

                drawer.openDrawer(listView);
            }
        });

        menuButton.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                for(int j = 0 ; j < weekDay.length ; j++){
                    onLoadProgram(weekDay[j]+"예능");
                }
                StyleableToast.makeText(getApplicationContext(), "방송 최신 정보를 가져옵니다!", Toast.LENGTH_SHORT,R.style.mytoast).show();
                onLoadProgramRoom(clickToday);
                return false;
            }
        });

        //노티피케이션
        Bundle extras = getIntent().getExtras();
        if(extras != null){
            id = extras.getInt("notification_id");
            if(id == 9999){
                room = extras.getString("room");
                Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                intent.putExtra("program", room);
                startActivity(intent);
                NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                nm.cancel(id);
            }
        }

        //크롤링요소
        imgUrl = new ArrayList<>();
        broadcastStation = new ArrayList<>();
        programTitle = new ArrayList<>();
        programTime = new ArrayList<>();
        programIntro = new ArrayList<>();
        programRating = new ArrayList<>();

        //기본요소
        dataList = new ArrayList<>();

        //요일별 클릭리스너
        Calendar cal = Calendar.getInstance();
        int num = cal.get(Calendar.DAY_OF_WEEK)-1;
        today = weekDay[num];
        clickToday = today+"예능";

        //리사이클러뷰 연결
        adapter = new AdapterRecyclerProgram(this, dataList, clickToday);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter.setOnClickListener(this);
        recyclerView.setAdapter(adapter);

        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("blackJinData"));

        //상황에 따라 서비스 실행
        onService();
        onLoadProgramRoom(today+"예능");
        onClickWeekColor(today+"예능");
        weekButton7.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                    onClickWeek(weekButton7.getText().toString()+"요일예능");
               }
           }
        );
        weekButton1.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickWeek(weekButton1.getText().toString()+"요일예능");
                   }
               }
        );
        weekButton2.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickWeek(weekButton2.getText().toString()+"요일예능");
                   }
               }
        );
        weekButton3.setOnClickListener(new View.OnClickListener() {
                                           @Override
                                           public void onClick(View view) {
                   onClickWeek(weekButton3.getText().toString()+"요일예능");
           }
       }
        );
        weekButton4.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickWeek(weekButton4.getText().toString()+"요일예능");
                   }
       }
        );
        weekButton5.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickWeek(weekButton5.getText().toString()+"요일예능");
                   }
       }
        );
        weekButton6.setOnClickListener(new View.OnClickListener() {
                   @Override
                   public void onClick(View view) {
                       onClickWeek(weekButton6.getText().toString()+"요일예능");
                   }
       }
        );

    }

    //----------------------------------------------------------------------------------------------
    //프로그램 데이터 저장

    private void onCreateProgramRoom(String week, String img, String broad, String title, String time, String intro, String rating){
        Call<ResponseBody> create_chat_room = apiService.createProgramRoom(week, img, broad, title, time, intro, rating);
        create_chat_room.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                String result = null;
                try {
                    result = response.body().string();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                Log.e("로드 리절트", result);
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
            }
        });
    }

    //----------------------------------------------------------------------------------------------
    //DB에서 프로그램룸을 가져옴

    private void onLoadProgramRoom(String week){
        Call<ResponseBody> load_program_room = apiService.loadProgramRoom(week);
        load_program_room.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    dataList.clear();
                    if(response.body() != null){
                        String result = response.body().string();
                        if (!result.equals("fail")) {
                            try {
                                JSONArray jsonArray = new JSONArray(result);
                                if(jsonArray.length() == 0){
                                    chatRoomInfo.setVisibility(View.VISIBLE);
                                } else {
                                    chatRoomInfo.setVisibility(View.GONE);
                                    for(int i=0; i < jsonArray.length(); i++){
                                        JSONObject jsonObject = jsonArray.getJSONObject(i);
                                        dataList.add(0, new DataProgram(
                                                jsonObject.getString("img"),
                                                jsonObject.getString("broad"),
                                                jsonObject.getString("title"),
                                                jsonObject.getString("time"),
                                                jsonObject.getInt("total"),
                                                jsonObject.getString("intro"),
                                                jsonObject.getString("rating"),
                                                lp.getInt(jsonObject.getString("title"),0)
                                        ));
                                    }
                                    adapter.notifyDataSetChanged();
                                }
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
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
    //프로그램을 크롤링해서 DB에 넣음

    private void onLoadProgram(final String week) {

        //AsyncTask 객체 생성
        @SuppressLint("StaticFieldLeak")
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {

                String baseUrl = "https://search.daum.net/search?w=tot&q=";
                try {
                    String subUrl = URLEncoder.encode(week,"UTF-8");
                    doc = Jsoup.connect(baseUrl+subUrl).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                broadcastStation.clear();
                programTitle.clear();
                programTime.clear();
                programIntro.clear();
                programRating.clear();

                //image
                Elements images = doc.select("a.wrap_thumb").select("img");
                for(Element element : images) {
//                    Log.e("시작","이미지"+element.attr("src"));
                    imgUrl.add(element.attr("src"));
                }

                //title
                Elements titles = doc.select("div.wrap_cont > strong");
                for(int j = 0; j < titles.size() ; j++) {
                    Element element = titles.get(j);
                    programTitle.add(element.text());
//                    Log.e("시작","타이틀"+element.text());
                    try {
                        String subUrl2 = URLEncoder.encode(element.text(),"UTF-8");
                        doc2 = Jsoup.connect(baseUrl+subUrl2).get();
                        int a = 0 ;
                        int b = 0 ;
                        Elements infos = doc2.select("div.info_cont > dl");
                        for(int k = 0; k < infos.size() ; k++) {
                            Element info = infos.get(k);
                            if(info.text().contains("소개")){
                                programIntro.add(info.text());
//                                Log.e("시작","인트로"+info.text());
                                b = 1;
                            }
                        }
                        if( b == 0){
                            programIntro.add("");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //broad

                Elements broads = doc.select("div.wrap_cont > span");
                for(int k = 0; k < broads.size() ; k++) {
                    Element element = broads.get(k);
                    if(k % 2 == 0 ){
                        if(element.text().contains("오전") || element.text().contains("오후")
                                || element.text().contains("낮") || element.text().contains("밤"))
                        programTime.add(element.text().substring(4));
//                        Log.e("시작","타임"+element.text().substring(4));
                    } else {
                        String[] s  = element.text().split(" ");
                        if(s.length == 2){
                            if(element.text().contains("%")){
                                broadcastStation.add(s[0]);
                                programRating.add(s[1]);
                            } else {
                                broadcastStation.add(s[0]+" "+s[1]);
                                programRating.add("");
                            }
//                            Log.e("시작","브로드"+element.text());
                        } else {
                            broadcastStation.add(s[0]);
                            programRating.add("");
                        }
                    }
                }

                return null;
            }
            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                for (int j = 0 ; j < imgUrl.size(); j++){
                    onCreateProgramRoom(week,
                            imgUrl.get(j),
                            broadcastStation.get(j),
                            programTitle.get(j),
                            programTime.get(j),
                            programIntro.get(j),
                            programRating.get(j));
                }
                imgUrl.clear();
                adapter.notifyDataSetChanged();
            }
        };

        asyncTask.execute();
    }

    //----------------------------------------------------------------------------------------------
    //프로그램아이템클릭리스너

    @Override
    public void statItemBoxClicked(int i) {


    }

    @Override
    public void chatItemBoxClicked(final int i) {

        roomList = lp.getString("room_list","");
        Log.e("챗클릭룸네임리스트",roomList);

        if(roomList.contains(dataList.get(i).getProgramTitle())){

            Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
            intent.putExtra("program",dataList.get(i).getProgramTitle());
            intent.putExtra("week",clickToday);
            startActivity(intent);

        } else {

            Intent intent = new Intent(getApplicationContext(), EntranceActivity.class);
            intent.putExtra("program",dataList.get(i).getProgramTitle());
            intent.putExtra("img",dataList.get(i).getImgUrl());
            intent.putExtra("broad",dataList.get(i).getBroadcastStation());
            intent.putExtra("time",dataList.get(i).getProgramTime());
            intent.putExtra("intro",dataList.get(i).getProgramIntro());
            intent.putExtra("rating",dataList.get(i).getProgramRating());
            intent.putExtra("week",clickToday);
            startActivity(intent);

        }
    }

    //----------------------------------------------------------------------------------------------
    //백버튼 설정

    private final long FINISH_INTERVAL_TIME = 2000;
    private long backPressedTime = 0;

    public void onBackPressed() {

        if(drawer.isDrawerVisible(listView)){
            drawer.closeDrawer(Gravity.RIGHT) ;
        } else {
            long tempTime = System.currentTimeMillis();
            long intervalTime = tempTime - backPressedTime;
            if (0 <= intervalTime && FINISH_INTERVAL_TIME >= intervalTime) {
                overridePendingTransition(0, 0);
                super.onBackPressed();
            } else {
                backPressedTime = tempTime;
                StyleableToast.makeText(getApplicationContext(), "뒤로 버튼을 한번 더 누르시면 종료됩니다.", Toast.LENGTH_SHORT,R.style.mytoast).show();
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //리스타트 : 프로그램룸 갱신, 상황에 따라 서비스 실행

    @Override
    protected void onRestart() {
        super.onRestart();

        onService();
        roomList = lp.getString("room_list","");
        LocalBroadcastManager.getInstance(this).registerReceiver(mBroadcastReceiver,
                new IntentFilter("blackJinData"));
        onLoadProgramRoom(clickToday);

    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mBroadcastReceiver);
    }

    //----------------------------------------------------------------------------------------------
    //로그아웃

    private void logout(){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // 제목셋팅
        alertDialogBuilder.setTitle("로그아웃");

        // AlertDialog 셋팅
        alertDialogBuilder
                .setMessage("로그아웃하면 되겠습니까?")
                .setCancelable(false)
                .setPositiveButton("네",
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {

                                lEdit.putString("room_list","");
                                lEdit.putString("login_id","");
                                lEdit.putString("login_nick","");
                                lEdit.putBoolean("login_key",false);
                                lEdit.apply();

                                Intent i = new Intent(getApplicationContext(), MyChatService.class);
                                stopService(i);

                                Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                                startActivity(intent);
                                finish();

                                StyleableToast.makeText(getApplicationContext(), "로그아웃하였습니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
                            }
                        })
                .setNegativeButton("아니오",
                        new DialogInterface.OnClickListener() {
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                dialog.cancel();
                            }
                        });

        // 다이얼로그 생성
        AlertDialog alertDialog = alertDialogBuilder.create();

        // 다이얼로그 보여주기
        alertDialog.show();

    }

    //----------------------------------------------------------------------------------------------
    //상황에 따라 서비스 실행

    private void onService(){

        Intent i = new Intent(getApplicationContext(),MyChatService.class);
        if(lp.getBoolean("login_key",false)){

            if(!isMyServiceRunning(MyChatService.class)){
                Log.e("스타트중아님,서비스시작","ㅋㅋ");
                startService(i);
            } else {
                Log.e("스타트중이라,서비스시작안함","ㅋㅋ");
            }
        }

        if(lp.getBoolean("receiver_dead",false)){
            Log.e("데이터 문제 발생","재시작");
            lEdit.putBoolean("receiver_dead",false);
            lEdit.apply();
            startService(i);
        } else {
            Log.e("데이터 문제 발생","없음");
        }

    }

    //----------------------------------------------------------------------------------------------
    //요일 버튼 클릭에 따른 색 조절

    private void onClickWeekColor(String week){

        weekButton7.setTextColor(getResources().getColor(R.color.black));
        weekButton1.setTextColor(getResources().getColor(R.color.black));
        weekButton2.setTextColor(getResources().getColor(R.color.black));
        weekButton3.setTextColor(getResources().getColor(R.color.black));
        weekButton4.setTextColor(getResources().getColor(R.color.black));
        weekButton5.setTextColor(getResources().getColor(R.color.black));
        weekButton6.setTextColor(getResources().getColor(R.color.black));

        switch (week){
            case "일요일예능" :
                weekButton7.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "월요일예능" :
                weekButton1.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "화요일예능" :
                weekButton2.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "수요일예능" :
                weekButton3.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "목요일예능" :
                weekButton4.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "금요일예능" :
                weekButton5.setTextColor(getResources().getColor(R.color.chat_back));
                break;
            case "토요일예능" :
                weekButton6.setTextColor(getResources().getColor(R.color.chat_back));
                break;
        }

    }

    //----------------------------------------------------------------------------------------------
    //요일 버튼 클릭 리스너 : 색, 프로그램 로드

    private void onClickWeek(String week){
        onClickWeekColor(week);
        onLoadProgramRoom(week);
        clickToday = week;
        adapter.onLoadWeek(clickToday);
    }

    //----------------------------------------------------------------------------------------------
    //현재 서비스 상태 확인

    //서비스 러닝 확인
    private boolean isMyServiceRunning(Class<?> serviceClass) {
        ActivityManager manager = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    //----------------------------------------------------------------------------------------------
    //프로그램찾기

    public void searchProgram(View view) {
        Intent i = new Intent(getApplicationContext(), SearchActivity.class);
        startActivity(i);
    }

    //----------------------------------------------------------------------------------------------

    private void onLoadRoomList() {

        Call<ResponseBody> get_room_by_user = apiService.getRoomByUser(nick);
        get_room_by_user.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {

//                    Log.e("넣는값",response.body().string());
                    String str3 = response.body().string();

                    lEdit.putString("room_list",str3);
                    lEdit.apply();

                    String str2 = lp.getString("room_list","메롱");

                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {

            }
        });
    }

}
