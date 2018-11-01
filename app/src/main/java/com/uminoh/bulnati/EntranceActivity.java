package com.uminoh.bulnati;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.muddzdev.styleabletoast.StyleableToast;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.URLEncoder;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EntranceActivity extends AppCompatActivity {

    String ent_program;
    String ent_img;
    String ent_broad;
    String ent_time;
    String ent_intro;
    String ent_rating;
    String ent_week;

    ImageView entImgView;
    ImageButton entBack;
    Button entOk;
    TextView entTitle;
    TextView entInfo;
    TextView entIntro;
    TextView entBroad;

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String room_list;
    String nick;

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_entrance);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        nick = lp.getString("login_nick","");

        Intent intent = getIntent();
        ent_program = intent.getStringExtra("program");
        ent_img = intent.getStringExtra("img");
        ent_broad = intent.getStringExtra("broad");
        ent_time = intent.getStringExtra("time");
        ent_intro = intent.getStringExtra("intro");
        ent_rating = intent.getStringExtra("rating");
        ent_week = intent.getStringExtra("week");

        entImgView = findViewById(R.id.ent_imgView);
        entBack = findViewById(R.id.ent_back);
        entOk = findViewById(R.id.ent_ok);
        entTitle = findViewById(R.id.ent_title);
        entInfo = findViewById(R.id.ent_info);
        entIntro = findViewById(R.id.ent_intro);
        entBroad = findViewById(R.id.ent_broad);

        Glide.with(getApplicationContext())
                .load(ent_img)
                .into(entImgView);

        entTitle.setText(ent_program);
        entBroad.setText(ent_broad);
        entInfo.setText(ent_rating+"\n"+ent_time);
        entIntro.setText(ent_intro);

        entBack.setOnClickListener(new View.OnClickListener() {
               @Override
               public void onClick(View view) {
                   onBackPressed();
               }
           }
        );

        entOk.setOnClickListener(new View.OnClickListener() {
                 @Override
                public void onClick(View view) {

                     onSetRoomUser();

                     Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
                     intent.putExtra("program",ent_program);
                     intent.putExtra("week",ent_week);
                     intent.putExtra("ent",true);
                     startActivity(intent);

                 }
             }
        );

    }

    //----------------------------------------------------------------------------------------------

    private void onSetRoomUser(){

        Call<ResponseBody> save_room_user = apiService.setRoomUser(ent_program, nick);
        save_room_user.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    String result = response.body().string();

                    if(result.equals("success")){
                        lEdit.putString("room_list",lp.getString("room_list","")+"/"+ent_program);
                        lEdit.apply();
                        finish();
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

}
