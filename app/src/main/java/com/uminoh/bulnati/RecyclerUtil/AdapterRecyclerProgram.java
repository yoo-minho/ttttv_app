package com.uminoh.bulnati.RecyclerUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.uminoh.bulnati.R;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterRecyclerProgram extends RecyclerView.Adapter<AdapterRecyclerProgram.ViewHolder> {

    //받아올 데이터리스트
    private List<DataProgram> mProgramList;
    private Context context;
    private String room_list;
    String week;
    String currentStr;

    //쉐어드프리퍼런스 : 로그인 유지 및 로드
    private SharedPreferences lp;
    private SharedPreferences.Editor lEdit;

    //----------------------------------------------------------------------------------------------

    //생성자
    public AdapterRecyclerProgram(Context context, List<DataProgram> mDataList, String week) { //어레이리스트 셋온 (생성자)
        this.mProgramList = mDataList;
        this.context = context;
        this.week = week;

        //쉐어드프리퍼런스 연결
        lp = context.getSharedPreferences("login", Context.MODE_PRIVATE);
        lEdit = lp.edit();
        room_list = lp.getString("room_list","");

    }

    //----------------------------------------------------------------------------------------------

    //인터페이스(메쏘드를 공유하는 간이 클래스 = 도구)
    public interface MyProgramRecyclerViewClickListener { //메쏘드 선언
        void statItemBoxClicked(int i);
        void chatItemBoxClicked(int i);
    }

    private AdapterRecyclerProgram.MyProgramRecyclerViewClickListener mListener; //인터페이스 객체 선언

    public void setOnClickListener(AdapterRecyclerProgram.MyProgramRecyclerViewClickListener listener) { //객체 셋온
        mListener = listener;
    }

    //----------------------------------------------------------------------------------------------

    //뷰홀더(=틀, 리사이클러뷰 틀에 쓰일 아이템 선언)
    public static class ViewHolder extends RecyclerView.ViewHolder {

        LinearLayout programList;
        ImageView programImage;
        TextView programBroad;
        TextView programLive;
        TextView programTitle;
        TextView programTime;
        TextView programRating;
        TextView programIntro;
        TextView totalText;
        TextView msgTotal;


        //뷰홀더와 뷰 연결
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            programList = itemView.findViewById(R.id.program_list);
            programImage = itemView.findViewById(R.id.program_image);
            programBroad = itemView.findViewById(R.id.program_broad);
            programLive = itemView.findViewById(R.id.program_live);
            programTitle = itemView.findViewById(R.id.program_title);
            programTime = itemView.findViewById(R.id.program_time);
            programRating = itemView.findViewById(R.id.program_rating);
            programIntro = itemView.findViewById(R.id.program_intro);
            totalText = itemView.findViewById(R.id.total_text);
            msgTotal = itemView.findViewById(R.id.msg_total);
        }
    }

    //----------------------------------------------------------------------------------------------

    //뷰홀더 생성
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        //리스트뷰
        view = LayoutInflater.from(viewGroup.getContext()).
                inflate(R.layout.item_program, viewGroup, false);
        return new ViewHolder(view);
    }

    //----------------------------------------------------------------------------------------------

    //뷰홀더 연결
    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, @SuppressLint("RecyclerView") final int i) {

        room_list = lp.getString("room_list","");
        Glide.with(context).load(mProgramList.get(i).getImgUrl()).into(viewHolder.programImage);
        viewHolder.programBroad.setText(mProgramList.get(i).getBroadcastStation());
        viewHolder.programTitle.setText(mProgramList.get(i).getProgramTitle());
        String getTime = mProgramList.get(i).getProgramTime();
        if(onLive(getTime)){
            Log.e("뭐지0",mProgramList.get(i).getProgramTitle());
            viewHolder.programLive.setVisibility(View.VISIBLE);
        } else {
            viewHolder.programLive.setVisibility(View.INVISIBLE);
        }
        viewHolder.programTime.setText(getTime);
        viewHolder.totalText.setText(mProgramList.get(i).getTotal()+"명");
        viewHolder.programRating.setText(mProgramList.get(i).getProgramRating());
        viewHolder.programIntro.setText(mProgramList.get(i).getProgramIntro());
        if(room_list.contains(mProgramList.get(i).getProgramTitle())){
            viewHolder.totalText.setText(mProgramList.get(i).getTotal()+"명 ★");
        } else {
            viewHolder.totalText.setText(mProgramList.get(i).getTotal()+"명");
        }
        if(mProgramList.get(i).getMsgNew()!=0){
            viewHolder.msgTotal.setVisibility(View.VISIBLE);
            viewHolder.msgTotal.setText("+"+mProgramList.get(i).getMsgNew());
        } else {
            viewHolder.msgTotal.setVisibility(View.INVISIBLE);
        }

        //인터페이스 체크
        if (mListener != null) {
            viewHolder.programList.setOnLongClickListener(new View.OnLongClickListener() {
                  @Override
                  public boolean onLongClick(View view) {
                      mListener.statItemBoxClicked(i);
                      return false;
                  }
              });
            viewHolder.programList.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mListener.chatItemBoxClicked(i);
                }
            });
        }
    }

    //----------------------------------------------------------------------------------------------

    //리사이클러뷰 사이즈 필수 메쏘드
    @Override
    public int getItemCount() {
        return mProgramList.size();
    }

    //----------------------------------------------------------------------------------------------

    private boolean onLive(String time){

        Calendar cal = Calendar.getInstance();
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String korDayOfWeek = "";
        switch (dayOfWeek){
            case 1 : korDayOfWeek ="일"; break;
            case 2 : korDayOfWeek ="월"; break;
            case 3 : korDayOfWeek ="화"; break;
            case 4 : korDayOfWeek ="수"; break;
            case 5 : korDayOfWeek ="목"; break;
            case 6 : korDayOfWeek ="금"; break;
            case 7 : korDayOfWeek ="토"; break;
        }

        if(week != null){
            if(week.replace("요일예능","").equals(korDayOfWeek)){

                SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("Hmm", Locale.KOREA);
                Date currentTime = new Date();
                currentStr = mSimpleDateFormat.format(currentTime);

                int amrm = 0;

                time = time.replace("방송시작 : ","")
                        .replace("분 ~","")
                        .replace("시 ","");

                if(time.contains("오전")){
                    amrm = 0;
                    time = time.replace("오전 ","");
                } else if(time.contains("낮")){
                    amrm = 0;
                    time = time.replace("낮 ","");
                } else if(time.contains("밤")){
                    amrm = 1200;
                    time = time.replace("밤 ","");
                } else if(time.contains("오후")){
                    amrm = 1200;
                    time = time.replace("오후 ","");
                }

                int broad_time = Integer.valueOf(time)+amrm;
                int real_time = Integer.valueOf(currentStr);
                if( real_time > broad_time && real_time < broad_time+100 ){
                    return true;
                } else {
                    return false;
                }
            }
        }
        return false;
    }

    public void onLoadWeek(String week){
        this.week = week;
    }

}
