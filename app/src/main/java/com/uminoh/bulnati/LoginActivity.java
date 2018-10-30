package com.uminoh.bulnati;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class LoginActivity extends AppCompatActivity {


    EditText id_edit;
    EditText pw_edit;
    Button login_bn;
    Button join_bn;

    String id;
    String pw;

    Retrofit retrofit;
    ApiService apiService;

    //쉐어드프리퍼런스 : 로그인 유지
    SharedPreferences lp;
    SharedPreferences.Editor lEdit;

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        id_edit = findViewById(R.id.id_edit);
        pw_edit = findViewById(R.id.pw_edit);
        login_bn = findViewById(R.id.login_bn);
        join_bn = findViewById(R.id.join_bn);

        //쉐어드프리퍼런스 연결
        lp = getSharedPreferences("login", MODE_PRIVATE);
        lEdit = lp.edit();

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //로그인버튼
        login_bn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                id = id_edit.getText().toString();
                pw = pw_edit.getText().toString();

                //레트로핏 API : 로그인
                Call<ResponseBody> call_login = apiService.login(id, pw);
                call_login.enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        try {
                            String result = response.body().string();
                            if (!result.equals("fail")) {

                                Toast.makeText(getBaseContext(), "로그인 성공", Toast.LENGTH_SHORT).show();

                                //로그인 시 아이디, 닉, 로그인 상태 저장
                                lEdit.putString("login_id", id);
                                lEdit.putString("login_nick", result);
                                lEdit.putBoolean("login_key", true);
                                lEdit.apply();

                                Intent intent = new Intent(getApplicationContext(), MainActivity.class);
                                startActivity(intent);
                                finish();

                            } else {
                                Toast.makeText(getApplicationContext(), "아이디와 비밀번호를 다시 확인해주세요.", Toast.LENGTH_SHORT).show();
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
        });

        //회원가입 버튼
        join_bn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), JoinActivity.class);
                startActivity(intent);
                finish();
            }
        });

    }

    //----------------------------------------------------------------------------------------------
    //테스트를 위한 로그인 기능

    public void a_login(View view) {
        Toast.makeText(getBaseContext(), "[에이네임]님! 로그인 성공!", Toast.LENGTH_SHORT).show();

        //로그인 시 아이디, 닉, 로그인 상태 저장
        lEdit.putString("login_id", "aellose@naver.com");
        lEdit.putString("login_nick", "에이네임");
        lEdit.putBoolean("login_key", true);
        lEdit.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();

        finish();
    }

    public void b_login(View view) {
        Toast.makeText(getBaseContext(), "[비네임]님! 로그인 성공!", Toast.LENGTH_SHORT).show();

        //로그인 시 아이디, 닉, 로그인 상태 저장
        lEdit.putString("login_id", "bellose@naver.com");
        lEdit.putString("login_nick", "비네임");
        lEdit.putBoolean("login_key", true);
        lEdit.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void c_login(View view) {
        Toast.makeText(getBaseContext(), "[제트네임]님! 로그인 성공!", Toast.LENGTH_SHORT).show();

        //로그인 시 아이디, 닉, 로그인 상태 저장
        lEdit.putString("login_id", "zellose@naver.com");
        lEdit.putString("login_nick", "제트네임");
        lEdit.putBoolean("login_key", true);
        lEdit.apply();

        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
        startActivity(intent);
        finish();
    }

    //----------------------------------------------------------------------------------------------
}
