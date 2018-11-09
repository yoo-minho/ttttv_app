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
    String API_URL = "http://"+ SecretKey.ip;

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //로그인 및 회원가입

    @FormUrlEncoded
    @POST("page_login/id_check.php")
    Call<ResponseBody> id_check(@Field("id") String id);

    @FormUrlEncoded
    @POST("page_login/nick_check.php")
    Call<ResponseBody> nick_check(@Field("nick") String nick);

    @FormUrlEncoded
    @POST("page_login/join.php")
    Call<ResponseBody> join(@Field("id") String Id,
                            @Field("pw") String pw,
                            @Field("nick") String nick,
                            @Field("pn") String pn);

    @FormUrlEncoded
    @POST("page_login/login.php")
    Call<ResponseBody> login(@Field("id") String Id,
                             @Field("pw") String pw);

    @FormUrlEncoded
    @POST("page_login/save_nick.php")
    Call<ResponseBody> saveNick(@Field("nick_new") String nick_new,
                                @Field("nick") String nick);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //채팅

    @FormUrlEncoded
    @POST("page_chat/save_chat.php")
    Call<ResponseBody> saveChat(@Field("msg") String msg,
                                      @Field("nick") String nick,
                                      @Field("date") String date,
                                      @Field("room") String room);

    @FormUrlEncoded
    @POST("page_chat/load_chat.php")
    Call<ResponseBody> loadChat(@Field("room") String room,
                                @Field("nick") String nick);

    @FormUrlEncoded
    @POST("page_chat/exit_chat.php")
    Call<ResponseBody> exitChat(@Field("room") String room,
                                @Field("nick") String nick,
                                @Field("date") String date);


    @POST("page_chat/get_all_room_user.php")
    Call<ResponseBody> getAllRoomUser();

    @FormUrlEncoded
    @POST("page_chat/get_user_by_room.php")
    Call<ResponseBody> getUserByRoom(@Field("room") String room);


    ////////////////////////////////////////////////////////////////////////////////////////////////
    //예능채팅방

    @FormUrlEncoded
    @POST("page_program/create_program_room.php")
    Call<ResponseBody> createProgramRoom(@Field("week") String week,
                                         @Field("img") String img,
                                         @Field("broad") String broad,
                                         @Field("title") String title,
                                         @Field("time") String time,
                                         @Field("intro") String intro,
                                         @Field("rating") String rating);

    @FormUrlEncoded
    @POST("page_program/load_program_room.php")
    Call<ResponseBody> loadProgramRoom(@Field("week") String week);

    @FormUrlEncoded
    @POST("page_program/search_program_room.php")
    Call<ResponseBody> searchProgramRoom(@Field("text") String text);

    @FormUrlEncoded
    @POST("page_program/broad_program_room.php")
    Call<ResponseBody> broadProgramRoom(@Field("text") String text);

    ////////////////////////////////////////////////////////////////////////////////////////////////
    //영상통화

    @FormUrlEncoded
    @POST("page_video/video_list_up.php")
    Call<ResponseBody> videoListUp(@Field("room") String room);

    @FormUrlEncoded
    @POST("page_video/video_list_down.php")
    Call<ResponseBody> videoListDown(@Field("room") String room);

    @POST("page_video/video_list_show.php")
    Call<ResponseBody> videoListShow();

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @Multipart
    @POST("/v1/vision/celebrity")
    Call<NaverRepo> naverRepo(@Header("X-Naver-Client-Id") String id
            , @Header("X-Naver-Client-Secret") String secret
            , @Part MultipartBody.Part file);

    ////////////////////////////////////////////////////////////////////////////////////////////////

    @FormUrlEncoded
    @POST("page_ent/set_room_user.php")
    Call<ResponseBody> setRoomUser(@Field("room") String room,
                                   @Field("nick") String nick);

    @FormUrlEncoded
    @POST("page_ent/get_room_by_user.php")
    Call<ResponseBody> getRoomByUser(@Field("nick") String nick);

}
