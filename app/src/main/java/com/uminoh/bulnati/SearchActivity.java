package com.uminoh.bulnati;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.RecyclerUtil.AdapterRecyclerProgram;
import com.uminoh.bulnati.RecyclerUtil.DataProgram;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class SearchActivity extends AppCompatActivity implements AdapterRecyclerProgram.MyProgramRecyclerViewClickListener{

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;

    //아이템
    List<DataProgram> dataList; //아이템 리스트
    AdapterRecyclerProgram adapter; //리사이클러뷰 어댑터

    EditText searchEdit;
    Button kbs1Button;
    Button kbs2Button;
    Button mbcButton;
    Button sbsButton;
    Button tvnButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search);

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder().baseUrl(ApiService.API_URL).build();
        apiService = retrofit.create(ApiService.class);

        searchEdit = findViewById(R.id.search_edit);
        kbs1Button = findViewById(R.id.kbs1_filter);
        kbs2Button = findViewById(R.id.kbs2_filter);
        mbcButton = findViewById(R.id.mbc_filter);
        sbsButton = findViewById(R.id.sbs_filter);
        tvnButton = findViewById(R.id.tvn_filter);

        //기본요소
        dataList = new ArrayList<>();

        //리사이클러뷰 연결
        adapter = new AdapterRecyclerProgram(this, dataList);
        RecyclerView recyclerView = findViewById(R.id.search_recycler_view);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        adapter.setOnClickListener(this);
        recyclerView.setAdapter(adapter);

        kbs1Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBroadProgramRoom(kbs1Button.getText().toString());
            }
        });

        kbs2Button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBroadProgramRoom(kbs2Button.getText().toString());
            }
        });

        mbcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBroadProgramRoom(mbcButton.getText().toString());
            }
        });

        sbsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBroadProgramRoom(sbsButton.getText().toString());
            }
        });

        tvnButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBroadProgramRoom(tvnButton.getText().toString());
            }
        });


        searchEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                //입력되는 텍스터에 변화가 있을때
                if(!searchEdit.getText().toString().equals("") && searchEdit.getText().toString().length()>1) {
                    onSearchProgramRoom(searchEdit.getText().toString());
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {
                //입력이 끝났을때
                if(!searchEdit.getText().toString().equals("") && searchEdit.getText().toString().length()>1){
                    onSearchProgramRoom(searchEdit.getText().toString());
                }
            }
        });

    }

    //----------------------------------------------------------------------------------------------
    //DB에서 프로그램룸을 서칭함

    private void onSearchProgramRoom(String text){
        Call<ResponseBody> search_program_room = apiService.searchProgramRoom(text);
        search_program_room.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    dataList.clear();
                    String result = response.body().string();
                    if (!result.equals("fail")) {
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            if(jsonArray.length() != 0){
                                for(int i=0; i < jsonArray.length(); i++){
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    dataList.add(0, new DataProgram(
                                            jsonObject.getString("img"),
                                            jsonObject.getString("broad"),
                                            "["+jsonObject.getString("week")+"] "+jsonObject.getString("title"),
                                            jsonObject.getString("time"),
                                            -1,
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
    //DB에서 프로그램룸을 서칭함

    private void onBroadProgramRoom(String text){
        Call<ResponseBody> broad_program_room = apiService.broadProgramRoom(text);
        broad_program_room.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    dataList.clear();
                    String result = response.body().string();
                    if (!result.equals("fail")) {
                        try {
                            JSONArray jsonArray = new JSONArray(result);
                            if(jsonArray.length() != 0){
                                for(int i=0; i < jsonArray.length(); i++){
                                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                                    dataList.add(0, new DataProgram(
                                            jsonObject.getString("img"),
                                            jsonObject.getString("broad"),
                                            "["+jsonObject.getString("week")+"] "+jsonObject.getString("title"),
                                            jsonObject.getString("time"),
                                            -1,
                                            jsonObject.getString("intro"),
                                            jsonObject.getString("rating")
                                    ));
                                }
                                adapter.notifyDataSetChanged();
                            } else {
                                StyleableToast.makeText(SearchActivity.this, "검색 결과값이 없습니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
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


    public void back_search_room(View view) {
        onBackPressed();
    }

    @Override
    public void statItemBoxClicked(int i) {

    }

    @Override
    public void chatItemBoxClicked(int i) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra("program",dataList.get(i).getProgramTitle());
        startActivity(intent);
    }
}
