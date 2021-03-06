package com.uminoh.bulnati;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.muddzdev.styleabletoast.StyleableToast;
import com.uminoh.bulnati.CfrUtil.Faces;
import com.uminoh.bulnati.CfrUtil.NaverRepo;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class CfrActivity extends AppCompatActivity {

    //웹 통신
    Retrofit retrofit;
    ApiService apiService;
    File mFile;

    private final int CAMERA_CODE = 1111;
    private final int GALLERY_CODE = 1112;

    ImageView cfrView;
    Button cameraButton;
    Button cameraButton2;
    Button galleryButton;
    ProgressBar cfrProgressBar;
    TextView firstCfr;
    ImageButton backCfr;
    ImageView cfrResultView;

    //크롤링
    Document doc = null;
    String imgStr = null;
    String jobsStr = "";

    //권한 선언
    private String[] permissions = {Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}; //권한 설정 변수
    private static final int MULTIPLE_PERMISSIONS = 102; //권한 동의 여부 문의 후 콜백 함수에 쓰일 변수

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cfr);

        //권한 묻기
        checkPermissions();

        //주소를 기반으로 객체 생성
        retrofit = new Retrofit.Builder()
                .baseUrl(SecretKey.cfrUrl)
                .addConverterFactory(GsonConverterFactory.create())
                .build();
        apiService = retrofit.create(ApiService.class);

        //구성요소 연결
        cfrView = findViewById(R.id.cfrView);
        cameraButton = findViewById(R.id.camera_front_button);
        cameraButton2 = findViewById(R.id.camera_back_button);
        galleryButton = findViewById(R.id.gallery_button);
        firstCfr = findViewById(R.id.first_cfr);
        cfrProgressBar = findViewById(R.id.cfr_progressBar);
        backCfr = findViewById(R.id.back_cfr);
        cfrResultView = findViewById(R.id.cfr_result_view);

        backCfr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        cfrProgressBar.setVisibility(View.GONE);

        cameraButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto(true);
            }
        });

        cameraButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectPhoto(false);
            }
        });

        galleryButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectGallery();
            }
        });

    }

    //----------------------------------------------------------------------------------------------
    //온액티비티리절트

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case GALLERY_CODE:
                    sendPicture(data.getData()); //갤러리에서 가져오기
                    cfrProgressBar.setVisibility(View.VISIBLE);
                    onCfr();
                    break;
                case CAMERA_CODE:
                    Uri uri = data.getData();
                    cfrView.setImageURI(uri);//이미지 뷰에 비트맵 넣기
                    mFile = new File(uri.getPath());
                    cfrProgressBar.setVisibility(View.VISIBLE);
                    onCfr();
                    break;
                default:
                    break;
            }
        }
    }

    //----------------------------------------------------------------------------------------------
    //갤러리 혹은 카메라

    private void selectGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setData(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, GALLERY_CODE);
    }

    private void selectPhoto(boolean b) {
        StyleableToast.makeText(this, "가로모드로 실행됩니다!", Toast.LENGTH_SHORT, R.style.mytoast).show();
        Intent intent = new Intent(getApplicationContext(), CameraActivity.class);
        intent.putExtra("front",b);
        startActivityForResult(intent, CAMERA_CODE);
    }

    //----------------------------------------------------------------------------------------------
    //이미지 파일 만들기

    private void sendPicture(Uri imgUri) {

        String imagePath = getRealPathFromURI(imgUri); // path 경로
        ExifInterface exif = null;
        int exifDegree = 0;
        try {
            exif = new ExifInterface(imagePath);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(exif != null){
            int exifOrientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            exifDegree = exifOrientationToDegrees(exifOrientation);
        }
        Bitmap bitmap = BitmapFactory.decodeFile(imagePath);//경로를 통해 비트맵으로 전환
        cfrView.setImageBitmap(rotate(bitmap, exifDegree));//이미지 뷰에 비트맵 넣기

        mFile = new File(imagePath);
        Log.e("갤러리 실제경로", imagePath);
    }


    private int exifOrientationToDegrees(int exifOrientation) {
        if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_90) {
            return 90;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_180) {
            return 180;
        } else if (exifOrientation == ExifInterface.ORIENTATION_ROTATE_270) {
            return 270;
        }
        return 0;
    }

    private Bitmap rotate(Bitmap src, float degree) {

        // Matrix 객체 생성
        Matrix matrix = new Matrix();
        // 회전 각도 셋팅
        matrix.postRotate(degree);
        // 이미지와 Matrix 를 셋팅해서 Bitmap 객체 생성
        return Bitmap.createBitmap(src, 0, 0, src.getWidth(),
                src.getHeight(), matrix, true);
    }

    private String getRealPathFromURI(Uri contentUri) {
        int column_index=0;
        String[] proj = {MediaStore.Images.Media.DATA};
        Cursor cursor = getContentResolver().query(contentUri, proj, null, null, null);
        if(cursor.moveToFirst()){
            column_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
        }
        return cursor.getString(column_index);
    }

    private void onCfr(){
        if(mFile != null){
            RequestBody reBody = RequestBody.create(MediaType.parse("image/jpeg"), mFile);
            MultipartBody.Part body = MultipartBody.Part.createFormData("image",mFile.getName(), reBody);
            Call<NaverRepo> call = apiService.naverRepo(SecretKey.clientId, SecretKey.clientSecret, body);
            call.enqueue(new Callback<NaverRepo>() {
                @Override
                public void onResponse(Call<NaverRepo> call, Response<NaverRepo> response) {
                    Log.e("얼굴값리턴","성공");
                    if (response.isSuccessful()) {
                        Faces[] str = response.body().getFaces();
                        if(str != null){
                            try {
                                String celebrity = str[0].getCelebrity().getValue();
                                String percent = Math.round(str[0].getCelebrity().getConfidence()*100)+"%";
                                onSearchWho(celebrity, percent, cfrResultView);
                            } catch (IndexOutOfBoundsException e){
                                e.printStackTrace();
                            }
                        } else {
                            firstCfr.setText("얼굴이 인식되지 않습니다!");
                            Glide.with(getApplicationContext())
                                    .load(R.drawable.celebrity)
                                    .into(cfrResultView);
                            cfrProgressBar.setVisibility(View.GONE);
                        }
                    } else {
                        firstCfr.setText("일치하는 연예인을 찾지 못했습니다! 이목구비 선명하게 사진을 찍어주세요!");
                        cfrProgressBar.setVisibility(View.GONE);
                        Glide.with(getApplicationContext())
                                .load(R.drawable.celebrity)
                                .into(cfrResultView);
                        cfrProgressBar.setVisibility(View.GONE);
                    }

                }
                @Override
                public void onFailure(Call<NaverRepo> call, Throwable t) {
                    Log.e("얼굴값리턴","실패");
                    cfrProgressBar.setVisibility(View.GONE);
                    firstCfr.setText("일치하는 연예인을 찾지 못했습니다! 이목구비 선명하게 사진을 찍어주세요!");
                    cfrProgressBar.setVisibility(View.GONE);
                    Glide.with(getApplicationContext())
                            .load(R.drawable.celebrity)
                            .into(cfrResultView);
                }
            });
        } else {
            StyleableToast.makeText(CfrActivity.this, "이미지를 선택해주세요!", Toast.LENGTH_SHORT, R.style.mytoast).show();
        }
    }

    //----------------------------------------------------------------------------------------------

    private void onSearchWho(final String who, final String percent, final ImageView imgView){

        imgStr = null;
        jobsStr = null;

        //AsyncTask 객체 생성
        @SuppressLint("StaticFieldLeak")
        AsyncTask asyncTask = new AsyncTask() {
            @Override
            protected Object doInBackground(Object[] params) {

                String baseUrl = "https://search.naver.com/search.naver?query=";
                try {
                    String subUrl = URLEncoder.encode(who,"UTF-8");
                    doc = Jsoup.connect(baseUrl+subUrl).get();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                //image

                try{
                    Elements images = doc.select("div.profile_wrap").select("img");
                    imgStr = images.get(0).attr("src");

                    //jobs
                    Elements jobs = doc.select("div.profile_wrap").select("span");
                    jobsStr = jobs.get(0).text();
                } catch (IndexOutOfBoundsException e){
                    e.printStackTrace();
                }

                return null;
            }
            @SuppressLint("SetTextI18n")
            @Override
            protected void onPostExecute(Object o) {
                super.onPostExecute(o);
                if(imgStr != null){
                    Glide.with(getApplicationContext())
                            .load(imgStr)
                            .into(imgView);
                    firstCfr.setText(jobsStr+"\n("+percent+")");
                } else {
                    Glide.with(getApplicationContext())
                            .load(R.drawable.celebrity)
                            .into(imgView);
                    firstCfr.setText("일치하는 연예인을 찾지 못했습니다!");
                }
                cfrProgressBar.setVisibility(View.GONE);
            }
        };

        asyncTask.execute();

    }


    //----------------------------------------------------------------------------------------------

    //권한체크
    private boolean checkPermissions() {
        int result;
        List<String> permissionList = new ArrayList<>(); //권한 배열리스트 하나 만든다
        for (String pm : permissions) { // 세 권한 다 따져봐
            result = ContextCompat.checkSelfPermission(this, pm); //셀프로 권한 체크해본 결과를 담아서
            if (result != PackageManager.PERMISSION_GRANTED) { //부여된 권한과 비교해서 없으면
                permissionList.add(pm); //리스트에 추가하자.
            }
        }
        if (!permissionList.isEmpty()) { //부여 권한이 아닌 애들이 존재한다면
            ActivityCompat.requestPermissions(this, permissionList.toArray(new String[permissionList.size()]),
                    MULTIPLE_PERMISSIONS); //사용자에게 리스트의 권한 요청 페이지를 차례대로 요청하자 + 콜백할 때 멀티플 퍼미션 변수 쓰자.
            return false; // 권한 요청할게 있네
        }
        return true; // 권한 다 충족하네
    }

    //----------------------------------------------------------------------------------------------

    //권한요청
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch (requestCode) {
            case MULTIPLE_PERMISSIONS: {
                if (grantResults.length > 0) { //동의했든 안했든 권한이 1개라도 있다면
                    for (int i = 0; i < permissions.length; i++) { //그 권한 개수만큼 따져보자
                        if (permissions[i].equals(this.permissions[0])) { //저장소읽기 권한이랑 같은 권한이 있을때
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) { //부여된 권한이 아니라면
                                showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                                Log.e("토스트","토스트1");
                            }
                        } else if (permissions[i].equals(this.permissions[1])) { //저장소쓰기 권한이랑 같은 권한이 있을때
                            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) { //부여된 권한이 아니라면
                                showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                                Log.e("토스트","토스트2");
                            }
                        }
                    }
                } else {
                    showNoPermissionToastAndFinish(); //토스트 보여주고 꺼버려
                }
                return;
            }
        }
    }

    //----------------------------------------------------------------------------------------------

    //권한토스트
    private void showNoPermissionToastAndFinish() {
        StyleableToast.makeText(this, "권한 요청에 동의 해주셔야 이용가능합니다.\n"
                + "설정에서 권한을 허용하시기 바랍니다.", Toast.LENGTH_SHORT, R.style.mytoast).show();
        finish();
    }


}
