package com.uminoh.bulnati;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.RecyclerUtil.AdapterRecyclerNoti;
import com.uminoh.bulnati.RecyclerUtil.DataNoti;

import java.io.IOException;
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

public class NotiActivity extends AppCompatActivity implements AdapterRecyclerNoti.AdapterRecyclerNotiClickListener {

    //쉐어드프리퍼런스 (기본, 에딧, 룸리스트, 로그인닉, 로그인유무확인키)
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list = "";
    String login_nick = "";
    boolean login_key = false;

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;

    String date_str;


    //리사이클러뷰 (어댑터, 리스트, 리사이클러뷰)
    private AdapterRecyclerNoti mAdapter;
    private List<DataNoti> mChatDataList;
    RecyclerView recyclerView;

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_noti);

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        room_list = lp.getString("room_list","");
        login_nick = lp.getString("login_nick", "닉네임값없음");
        login_key = lp.getBoolean("login_key", false);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyMM월 dd일HHmmssSSaa hh:mm", Locale.KOREA);
        Date currentTime = new Date();
        date_str = mSimpleDateFormat.format(currentTime);

        //리사이클러뷰 연결
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        mChatDataList = new ArrayList<>();
        mAdapter = new AdapterRecyclerNoti(this, mChatDataList);
        mAdapter.setOnClickListener(this);
        recyclerView = findViewById(R.id.noti_recycler_view);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(mAdapter);

        //리스트 로드
        getNotiList();

        if(room_list.equals("")){
            StyleableToast.makeText(this, "내 채팅방이 없습니다!", Toast.LENGTH_SHORT, R.style.mytoast).show();
        }

    }

    //----------------------------------------------------------------------------------------------
    //아이템클릭리스너

    @Override
    public void ItemBoxClicked(int i) {

        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("program",mChatDataList.get(i).getTitle());
        startActivity(intent);

    }

    //----------------------------------------------------------------------------------------------
    //백버튼

    public void back_noti_room(View view) {
        onBackPressed();
    }

    //----------------------------------------------------------------------------------------------
    //알림리스트 로드

    private void getNotiList(){

        mChatDataList.clear();
        String[] rooms = room_list.split("/");
        for(int i = 0 ; i< rooms.length ; i++){
            if(!rooms[i].equals("")){
                mChatDataList.add(new DataNoti(rooms[i],
                        "",
                        ""));
            }
        }
        mAdapter.notifyDataSetChanged();

        if(room_list.equals("")){
            StyleableToast.makeText(this, "내 채팅방이 없습니다!", Toast.LENGTH_SHORT, R.style.mytoast).show();
        }

    }

    //----------------------------------------------------------------------------------------------
    //온리스타트 : 리스트재로드

    @Override
    protected void onRestart() {
        super.onRestart();

        //리스트 로드
        room_list = lp.getString("room_list","");
        getNotiList();
    }
}
