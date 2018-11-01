package com.uminoh.bulnati.RecyclerUtil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.uminoh.bulnati.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterRecyclerChat extends RecyclerView.Adapter<AdapterRecyclerChat.ViewHolder> {

    //받아올 데이터리스트
    private List<DataChat> mItemList;
    private Context context;

    //----------------------------------------------------------------------------------------------
    //생성자

    public AdapterRecyclerChat(Context context, List<DataChat> mDataList) { //어레이리스트 셋온 (생성자)
        mItemList = mDataList;
        this.context = context;
    }

    //----------------------------------------------------------------------------------------------
    //인터페이스(메쏘드를 공유하는 간이 클래스 = 도구)

    public interface MyChatRecyclerViewClickListener { //메쏘드 선언
        void youItemBoxClicked(int i);
        void meItemBoxClicked(int i);
    }

    private AdapterRecyclerChat.MyChatRecyclerViewClickListener mListener; //인터페이스 객체 선언

    public void setOnClickListener(AdapterRecyclerChat.MyChatRecyclerViewClickListener listener) { //객체 셋온
        mListener = listener;
    }

    //----------------------------------------------------------------------------------------------
    //뷰홀더(=틀, 리사이클러뷰 틀에 쓰일 아이템 선언)

    public static class ViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutMe;
        ConstraintLayout layoutYou;
        TextView nickname;
        TextView bubbleYou;
        TextView bubbleMe;
        TextView dateYou;
        TextView dateMe;
        TextView entText;
        TextView entText2;
        TextView entText3;
        LinearLayout entLinear;

        //뷰홀더와 뷰 연결
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutMe = itemView.findViewById(R.id.layout_me);
            layoutYou = itemView.findViewById(R.id.layout_you);
            nickname = itemView.findViewById(R.id.nickname_text);
            bubbleYou = itemView.findViewById(R.id.bubble_you_text);
            bubbleMe = itemView.findViewById(R.id.bubble_me_text);
            dateYou = itemView.findViewById(R.id.date_text_you);
            dateMe = itemView.findViewById(R.id.date_text_me);
            entText = itemView.findViewById(R.id.ent_text);
            entText2 = itemView.findViewById(R.id.ent_text2);
            entText3 = itemView.findViewById(R.id.ent_text3);
            entLinear = itemView.findViewById(R.id.ent_linear);
        }
    }

    //----------------------------------------------------------------------------------------------
    //뷰홀더 생성

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        //리스트뷰
        view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_chat, viewGroup, false);
        return new ViewHolder(view);
    }

    //----------------------------------------------------------------------------------------------
    //뷰홀더 연결

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(@NonNull final ViewHolder viewHolder, @SuppressLint("RecyclerView") final int i) {

                String date_str;
                String date_item;
                SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyMM월 dd일HHmmssSSaa hh:mm", Locale.KOREA);
                Date currentTime = new Date();
                date_str = mSimpleDateFormat.format(currentTime);
                date_str = date_str.substring(0,9);

                Boolean isMe = mItemList.get(i).getIsMe();

                int showMe = 0;
                int showYou = 0;
                int showEnt = 0;

                //시작
                if (mItemList.get(i).getDate() == null || mItemList.get(i).getDate().length() < 17) {
                    viewHolder.dateYou.setText(mItemList.get(i).getDate());
                    viewHolder.dateMe.setText(mItemList.get(i).getDate());
                } else {
                    //오늘과 비교하여, 시간, 날짜 표기법 달리함
                    date_item = mItemList.get(i).getDate().substring(0,9);
                    if(date_item.equals(date_str)){
                        viewHolder.dateYou.setText(mItemList.get(i).getDate().substring(17));
                        viewHolder.dateMe.setText(mItemList.get(i).getDate().substring(17));
                    } else {
                        viewHolder.dateYou.setText(mItemList.get(i).getDate().substring(2,9));
                        viewHolder.dateMe.setText(mItemList.get(i).getDate().substring(2,9));
                    }
                }

                if(mItemList.get(i).getMessage().equals("입장")){

                    viewHolder.entText.setText(mItemList.get(i).getNickname()+"님이 들어왔습니다.");
                    viewHolder.entText2.setText("운영 정책을 위반한 메시지로");
                    viewHolder.entText3.setText("채팅 이용에 제한이 있을 수 있습니다.");
                    showYou = View.INVISIBLE;
                    showMe = View.INVISIBLE;

                } else if (mItemList.get(i).getMessage().equals("퇴장")){

                    viewHolder.entText.setText(mItemList.get(i).getNickname()+"님이 나갔습니다.");
                    viewHolder.entText2.setText("위 인원이 채팅 규칙을 준수하지 않았다면");
                    viewHolder.entText3.setText("신고 해주시길 바랍니다!");
                    showYou = View.INVISIBLE;
                    showMe = View.INVISIBLE;

                } else {

                    //오늘과 비교하여, 시간, 날짜 표기법 달리함
                    if(isMe){
                        viewHolder.nickname.setText(mItemList.get(i).getNickname()+"(본인)");
                        viewHolder.bubbleYou.setText("");
                        viewHolder.bubbleMe.setText(mItemList.get(i).getMessage());
                        showYou = View.INVISIBLE;
                        showEnt = View.INVISIBLE;
                    } else {
                        viewHolder.nickname.setText(mItemList.get(i).getNickname());
                        viewHolder.bubbleYou.setText(mItemList.get(i).getMessage());
                        viewHolder.bubbleMe.setText("");
                        showMe = View.INVISIBLE;
                        showEnt = View.INVISIBLE;
                    }

                }

                viewHolder.layoutMe.setVisibility(showMe);
                viewHolder.layoutYou.setVisibility(showYou);
                viewHolder.entLinear.setVisibility(showEnt);

                if (mListener != null) {
                    viewHolder.bubbleMe.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.meItemBoxClicked(i);
                        }
                    });
                    viewHolder.bubbleYou.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mListener.youItemBoxClicked(i);
                        }
                    });
                }
    }

    //----------------------------------------------------------------------------------------------

    //리사이클러뷰 사이즈 필수 메쏘드
    @Override
    public int getItemCount() {
        return mItemList.size();
    }

    //----------------------------------------------------------------------------------------------

}

