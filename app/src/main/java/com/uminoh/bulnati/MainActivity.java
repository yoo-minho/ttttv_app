package com.uminoh.bulnati;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.ResultReceiver;
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
import java.io.UnsupportedEncodingException;
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
    ImageButton updateButton;
    CardView chatRoomInfo;
    ListView listView = null;
    DrawerLayout drawer;
    ImageButton menuButton;

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list;
    String nick;

    //날짜
    String today;
    String clickToday;

    //서비스리시버
    ResultReceiver resultReceiver;

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

    int update_complete = 0;

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
        updateButton = findViewById(R.id.update_button);
        chatRoomInfo = findViewById(R.id.chat_room_info);
        drawer = findViewById(R.id.drawer);
        menuButton = findViewById(R.id.menu_button);

        //드롭레이아웃
        final String[] items = {"< SPECIAL MENU >"
                , "  - 알림 받는 채널"
                , "  - 랜덤 영상 통화 (1대1)"
                , "  - 연예인 닮은꼴 찾기"
                , "  - 미니게임"
                , "  - 환경설정"
                , "  - 로그아웃"} ;

        ArrayAdapter baseAdapter = new ArrayAdapter(getApplicationContext(), android.R.layout.simple_list_item_1, items) ;
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                drawer.openDrawer(listView);
            }
        });
        listView = findViewById(R.id.drawer_menulist) ;
        listView.setAdapter(baseAdapter) ;
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                switch (i) {
                    case 0 : // 스페셜메뉴
                        break;
                    case 1 : // 알림관리
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
                    case 4 : // 미니게임
                        break ;
                    case 5 : // 환경설정
                        break ;
                    case 6 : // 로그아웃
                        logout();
                        break ;}
                drawer.closeDrawer(Gravity.RIGHT) ;
            }
        });

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

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

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        nick = lp.getString("login_nick","");

        if(!lp.getBoolean("login_key",false)){
            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
            startActivity(intent);
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

        //리사이클러뷰 연결
        adapter = new AdapterRecyclerProgram(this, dataList);
        RecyclerView recyclerView = findViewById(R.id.recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter.setOnClickListener(this);
        recyclerView.setAdapter(adapter);

        //서비스 리시버
        Handler handler = new Handler();
        resultReceiver = new ResultReceiver(handler){
            @Override
            protected void onReceiveResult(int resultCode, Bundle resultData) {
                super.onReceiveResult(resultCode, resultData);
                if (resultCode == 1){
                    String msg = resultData.getString("msg");
                    if(msg.equals("Succeed!!")){
                        onLoadProgramRoom(clickToday);
                    }
                }
            }
        };

        //상황에 따라 서비스 실행
        onService();

        //요일별 클릭리스너
        Calendar cal = Calendar.getInstance();
        int num = cal.get(Calendar.DAY_OF_WEEK)-1;
        today = weekDay[num];
        clickToday = today+"예능";
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
        updateButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                for(int j = 0 ; j < weekDay.length ; j++){
                    onLoadProgram(weekDay[j]+"예능");
                }
                StyleableToast.makeText(getApplicationContext(), "방송 정보를 가져옵니다!", Toast.LENGTH_SHORT,R.style.mytoast).show();
                onLoadProgramRoom(clickToday);
            }
        });

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
                    if(update_complete == 7){
                        StyleableToast.makeText(getApplicationContext(), "업데이트 완료!", Toast.LENGTH_SHORT,R.style.mytoast).show();
                    }
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
                                            jsonObject.getString("rating")
                                    ));
                                }
                                adapter.notifyDataSetChanged();
                            }
                        } catch (JSONException e) {
                            e.printStackTrace();
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
                Elements images = doc.select("div.wrap_thumb").select("img");
                for(Element element : images) {
                    imgUrl.add(element.attr("src"));
                }

                //title
                Elements titles = doc.select("div.f_l > a");
                for(int j = 0; j < titles.size() ; j++) {
                    Element element = titles.get(j);
                    programTitle.add(element.text());
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
                                b = 1;
                            } else if (info.text().contains("시청률")){
                                programRating.add(info.text());
                                a = 1;
                            }
                        }
                        if( a == 0){
                            programRating.add("");
                        }
                        if( b == 0){
                            programIntro.add("");
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                //broad

                Elements broads = doc.select("div.f_l > dl");
                for(int k = 0; k < broads.size() ; k++) {
                    Element element = broads.get(k);
                    if(element.className().equals("dl_comm channel_info")){
                        broadcastStation.add(element.text());
                    } else if (element.className().equals("dl_comm time_info")){
                        programTime.add(element.text());
                    }
                }

                return null;
            }
            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                update_complete++;
                for (int j = 0 ; j < programTitle.size(); j++){
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
    public void chatItemBoxClicked(int i) {

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("program",dataList.get(i).getProgramTitle());
        startActivity(intent);

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

        onLoadProgramRoom(clickToday);
        onService();

    }

    //----------------------------------------------------------------------------------------------
    //로그아웃

    private void logout(){

        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(this);

        // 제목셋팅
        alertDialogBuilder.setTitle("로그아웃");

        // AlertDialog 셋팅
        alertDialogBuilder
                .setMessage("로그아웃하면 되겠습니가? (알림 초기화)")
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

        room_list = lp.getString("room_list","");

        Intent i = new Intent(getApplicationContext(),MyChatService.class);

        if(!isMyServiceRunning(MyChatService.class)){
            Log.e("MyChatService","NOT RUNNING");
            if(!room_list.equals("")){
                Log.e("MyChatService","Start!!!");
                i.putExtra("RECEIVER",resultReceiver);
                startService(i);
            }
        } else {
            Log.e("MyChatService","RUNNING");
            if(room_list.equals("")){
                Log.e("MyChatService","Stop!!!");
                stopService(i);
            } else {
                stopService(i);
                Log.e("MyChatService","First ReStart!!!");

                i.putExtra("RECEIVER",resultReceiver);
                startService(i);
            }
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

}
