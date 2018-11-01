package com.uminoh.bulnati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class NickActivity extends AppCompatActivity {

    EditText nick_edit;
    Button nick_edit_complete;

    Retrofit retrofit;
    ApiService apiService;

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;
    String nick;

    String nick_new;

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_nick);

        nick_edit = findViewById(R.id.nick_edit);
        nick_edit_complete = findViewById(R.id.nick_edit_complete);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();
        nick = lp.getString("login_nick","");

        nick_edit.setText(nick);

        nick_edit_complete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSaveNick();
            }
        });
    }

    private void onSaveNick(){

        nick_new = nick_edit.getText().toString();

        if(nick_new.equals(nick)){
            Toast.makeText(getApplicationContext(), "기존 아이디와 동일합니다.", Toast.LENGTH_SHORT).show();
        } else if (nick_new.length() < 3) {
            Toast.makeText(getApplicationContext(), "3자 이상 닉네임으로 만들어주세요.", Toast.LENGTH_SHORT).show();
        } else if (nick_new.length() > 8) {
            Toast.makeText(getApplicationContext(), "8자 이하 닉네임으로 만들어주세요.", Toast.LENGTH_SHORT).show();
        } else if (!Pattern.matches("^[가-힣]*$", nick_new)) {
            Toast.makeText(getApplicationContext(), "한글로 만들어주세요.", Toast.LENGTH_SHORT).show();
            nick_edit.setText("");
        } else {

            Call<ResponseBody> save_nick = apiService.saveNick(nick_new, nick);
            save_nick.enqueue(new Callback<ResponseBody>() {
                @Override
                public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                    try {
                        String result = response.body().string();
                        Log.e("리절트트트트",result);

                        if(result.equals("success")){

                            lEdit.putString("login_nick",nick_new);
                            lEdit.apply();
                            StyleableToast.makeText(getApplicationContext(), "닉네임 변경완료!", Toast.LENGTH_SHORT,R.style.mytoast).show();
                            finish();

                        } else {
                            StyleableToast.makeText(getApplicationContext(), "중복되는 아이디입니다.", Toast.LENGTH_SHORT,R.style.mytoast).show();
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
}
