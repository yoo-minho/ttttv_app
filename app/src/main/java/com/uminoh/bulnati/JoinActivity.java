package com.uminoh.bulnati;

import android.content.Intent;
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

import java.io.IOException;
import java.util.regex.Pattern;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class JoinActivity extends AppCompatActivity {

    EditText id_edit;
    Button idc_button;
    EditText pw_edit;
    EditText pwc_edit;
    TextView pwc_alert;
    EditText nick_edit;
    Button nick_button;
    EditText pn_edit;
    Button join_bn;

    String id;
    String pw;
    String pwc;
    String nick;
    String pn;

    Retrofit retrofit;
    ApiService apiService;

    //----------------------------------------------------------------------------------------------
    //온크리에이트

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_join);

        id_edit = findViewById(R.id.id_edit);
        idc_button = findViewById(R.id.idc_bn);
        pw_edit = findViewById(R.id.pw_edit);
        pwc_edit = findViewById(R.id.pwc_edit);
        pwc_alert = findViewById(R.id.pwc_alert);
        nick_edit = findViewById(R.id.nick_edit);
        nick_button = findViewById(R.id.nc_bn);
        pn_edit = findViewById(R.id.pn_edit);
        join_bn = findViewById(R.id.join_bn);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        //아이디 확인
        idc_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                id = id_edit.getText().toString();
                if (id.length() == 0) {
                    Toast.makeText(JoinActivity.this, "이메일을 적어주세요.", Toast.LENGTH_SHORT).show();
                } else if (!android.util.Patterns.EMAIL_ADDRESS.matcher(id).matches()) {
                    Toast.makeText(JoinActivity.this, "Email 형식으로 입력하세요.", Toast.LENGTH_SHORT).show();
                } else {
                    //레트로핏 API : 아이디 확인
                    Call<ResponseBody> call_id_check = apiService.id_check(id);
                    call_id_check.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {
                                String result = response.body().string();
                                if (result.equals("fail")) {
                                    idc_button.setText("확인완료");
                                    id_edit.setEnabled(false);
                                    idc_button.setEnabled(false);
                                    Toast.makeText(getApplicationContext(), "처음 가입하는 이메일입니다.", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(getApplicationContext(), "가입한 이메일이 있습니다.", Toast.LENGTH_SHORT).show();
                                    id_edit.setText("");
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
        });

        //패스워드
        pw_edit.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean b) {
                if (!b) {
                    pw = pw_edit.getText().toString();
                    if (pw.length() < 8) {
                        Toast.makeText(JoinActivity.this, "8~16자 비밀번호 형식을 지켜주세요", Toast.LENGTH_SHORT).show();
                        pw_edit.setText("");
                    } else if (pw.length() > 16) {
                        Toast.makeText(JoinActivity.this, "8~16자 비밀번호 형식을 지켜주세요", Toast.LENGTH_SHORT).show();
                        pw_edit.setText("");
                    }
                }
            }
        });

        //패스워드 확인
        pwc_edit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                pw = pw_edit.getText().toString();
                pwc = pwc_edit.getText().toString();
                if (pw.equals(pwc)) {
                    pwc_alert.setText("비밀번호가 일치합니다.");
                    pwc_alert.setTextColor(Color.BLUE);
                } else {
                    pwc_alert.setText("비밀번호가 일치하지 않습니다.");
                    pwc_alert.setTextColor(Color.RED);
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });

        //닉네임 확인
        nick_button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                nick = nick_edit.getText().toString();
                if (nick.length() < 3) {
                    Toast.makeText(getApplicationContext(), "3자 이상 닉네임으로 만들어주세요.", Toast.LENGTH_SHORT).show();
                } else if (nick.length() > 8) {
                    Toast.makeText(getApplicationContext(), "8자 이하 닉네임으로 만들어주세요.", Toast.LENGTH_SHORT).show();
                } else if (!Pattern.matches("^[가-힣]*$", nick)) {
                    Toast.makeText(getApplicationContext(), "한글로 만들어주세요.", Toast.LENGTH_SHORT).show();
                    nick_edit.setText("");
                } else {
                    //레트로핏 API : 닉네임 확인
                    Call<ResponseBody> call_nick_check = apiService.nick_check(nick);
                    call_nick_check.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {
                                String result = response.body().string();
                                if (result.equals("fail")) {
                                    Toast.makeText(getApplicationContext(), "사용가능한 닉네임입니다.", Toast.LENGTH_SHORT).show();
                                    nick_button.setText("확인완료");
                                    nick_edit.setEnabled(false);
                                    nick_button.setEnabled(false);
                                } else {
                                    Toast.makeText(getApplicationContext(), "이미 사용하고 있는 닉네임입니다.", Toast.LENGTH_SHORT).show();
                                    nick_edit.setText("");
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
        });

        //가입 버튼
        join_bn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                id = id_edit.getText().toString();
                pw = pw_edit.getText().toString();
                nick = nick_edit.getText().toString();
                pn = pn_edit.getText().toString();

                String idc = idc_button.getText().toString();
                String nc = nick_button.getText().toString();

                if (id.equals("") || pw.equals("") || nick.equals("") || pn.equals("")) {
                    Toast.makeText(JoinActivity.this, "빈칸을 확인해주세요!", Toast.LENGTH_SHORT).show();
                } else if (pn.length() != 11) {
                    Toast.makeText(JoinActivity.this, "핸드폰 번호를 제대로 적어주세요", Toast.LENGTH_SHORT).show();
                    pn_edit.setText("");
                } else if (!idc.equals("확인완료") || !nc.equals("확인완료")) {
                    Toast.makeText(JoinActivity.this, "아이디와 닉네임을 확인해주세요", Toast.LENGTH_SHORT).show();
                } else {
                    //레트로핏 API
                    Call<ResponseBody> call_join = apiService.join(id, pw, nick, pn);
                    call_join.enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            try {
                                String result = response.body().string();
                                Log.e("조인확인",result);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            Toast.makeText(JoinActivity.this, "회원가입 성공", Toast.LENGTH_SHORT).show();
                            Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                            startActivity(intent);
                            finish();
                        }

                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {
                            Toast.makeText(JoinActivity.this, "회원가입 실패", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }
        });

    }
}
