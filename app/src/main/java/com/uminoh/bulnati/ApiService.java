package com.uminoh.bulnati;

import com.uminoh.bulnati.CfrUtil.NaverRepo;

import okhttp3.MultipartBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Field;
import retrofit2.http.FormUrlEncoded;
import retrofit2.http.Header;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

public interface ApiService {

    //서버주소
    String API_URL = "http://"+ ConstantsTcp.ip;

    @FormUrlEncoded
    @POST("id_check.php")
    Call<ResponseBody> id_check(@Field("id") String id);

    @FormUrlEncoded
    @POST("nick_check.php")
    Call<ResponseBody> nick_check(@Field("nick") String nick);

    @FormUrlEncoded
    @POST("join.php")
    Call<ResponseBody> join(@Field("id") String Id,
                            @Field("pw") String pw,
                            @Field("nick") String nick,
                            @Field("pn") String pn);

    @FormUrlEncoded
    @POST("login.php")
    Call<ResponseBody> login(@Field("id") String Id,
                             @Field("pw") String pw);

    @FormUrlEncoded
    @POST("save_chat.php")
    Call<ResponseBody> saveChat(@Field("msg") String msg,
                                      @Field("nick") String nick,
                                      @Field("date") String date,
                                      @Field("room") String room);

    @FormUrlEncoded
    @POST("load_chat.php")
    Call<ResponseBody> loadChat(@Field("room") String room);


    @FormUrlEncoded
    @POST("create_program_room.php")
    Call<ResponseBody> createProgramRoom(@Field("week") String week,
                                         @Field("img") String img,
                                         @Field("broad") String broad,
                                         @Field("title") String title,
                                         @Field("time") String time,
                                         @Field("intro") String intro,
                                         @Field("rating") String rating);

    @FormUrlEncoded
    @POST("load_program_room.php")
    Call<ResponseBody> loadProgramRoom(@Field("week") String week);

    @FormUrlEncoded
    @POST("search_program_room.php")
    Call<ResponseBody> searchProgramRoom(@Field("text") String text);

    @FormUrlEncoded
    @POST("broad_program_room.php")
    Call<ResponseBody> broadProgramRoom(@Field("text") String text);

    @FormUrlEncoded
    @POST("video_list_up.php")
    Call<ResponseBody> videoListUp(@Field("room") String room);

    @FormUrlEncoded
    @POST("video_list_down.php")
    Call<ResponseBody> videoListDown(@Field("room") String room);

    @POST("video_list_show.php")
    Call<ResponseBody> videoListShow();

    @Multipart
    @POST("/v1/vision/celebrity")
    Call<NaverRepo> naverRepo(@Header("X-Naver-Client-Id") String id
            , @Header("X-Naver-Client-Secret") String secret
            , @Part MultipartBody.Part file);

    @Multipart
    @POST("/v1/vision/face")
    Call<NaverRepo> naverRepo2(@Header("X-Naver-Client-Id") String id
            ,@Header("X-Naver-Client-Secret") String secret
            ,@Part MultipartBody.Part file);

}
