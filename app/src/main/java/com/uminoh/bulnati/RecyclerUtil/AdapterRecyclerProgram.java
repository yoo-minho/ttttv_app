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

import java.util.List;

public class AdapterRecyclerProgram extends RecyclerView.Adapter<AdapterRecyclerProgram.ViewHolder> {

    //받아올 데이터리스트
    private List<DataProgram> mProgramList;
    private Context context;
    private String room_list;

    //쉐어드프리퍼런스 : 로그인 유지 및 로드
    private SharedPreferences lp;
    private SharedPreferences.Editor lEdit;

    //----------------------------------------------------------------------------------------------

    //생성자
    public AdapterRecyclerProgram(Context context, List<DataProgram> mDataList) { //어레이리스트 셋온 (생성자)
        mProgramList = mDataList;
        this.context = context;

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
        TextView programTitle;
        TextView programTime;
        TextView programRating;
        TextView programIntro;
        TextView joinMsg;
        TextView totalText;


        //뷰홀더와 뷰 연결
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            programList = itemView.findViewById(R.id.program_list);
            programImage = itemView.findViewById(R.id.program_image);
            programBroad = itemView.findViewById(R.id.program_broad);
            programTitle = itemView.findViewById(R.id.program_title);
            programTime = itemView.findViewById(R.id.program_time);
            programRating = itemView.findViewById(R.id.program_rating);
            programIntro = itemView.findViewById(R.id.program_intro);
            joinMsg = itemView.findViewById(R.id.join_msg);
            totalText = itemView.findViewById(R.id.total_text);
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
        viewHolder.programTime.setText(mProgramList.get(i).getProgramTime());
        viewHolder.totalText.setText(mProgramList.get(i).getTotal()+"명");
        viewHolder.programRating.setText(mProgramList.get(i).getProgramRating());
        viewHolder.programIntro.setText(mProgramList.get(i).getProgramIntro());
        if(room_list.contains(mProgramList.get(i).getProgramTitle())){
            viewHolder.programTitle.setText(mProgramList.get(i).getProgramTitle() + " ★");
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

}
