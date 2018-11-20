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
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;

import com.uminoh.bulnati.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterRecyclerChat extends RecyclerView.Adapter {

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


    public static class MeViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutMe;
        TextView bubbleMe;
        TextView dateMe;

        //뷰홀더와 뷰 연결
        public MeViewHolder( View itemView) {
            super(itemView);

            layoutMe = itemView.findViewById(R.id.layout_me);
            bubbleMe = itemView.findViewById(R.id.bubble_me_text);
            dateMe = itemView.findViewById(R.id.date_text_me);
        }
    }

    public static class YouViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout layoutYou;
        TextView nickname;
        TextView bubbleYou;
        TextView dateYou;

        //뷰홀더와 뷰 연결
        public YouViewHolder(View itemView) {
            super(itemView);
            layoutYou = itemView.findViewById(R.id.layout_you);
            nickname = itemView.findViewById(R.id.nickname_text);
            bubbleYou = itemView.findViewById(R.id.bubble_you_text);
            dateYou = itemView.findViewById(R.id.date_text_you);
        }
    }

    public static class EntryViewHolder extends RecyclerView.ViewHolder {

        TextView entText;
        TextView entText2;
        TextView entText3;
        LinearLayout entLinear;

        //뷰홀더와 뷰 연결
        public EntryViewHolder(View itemView) {
            super(itemView);
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
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        //리스트뷰
        switch (i) {
            case DataChat.ME_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_chat_me, viewGroup, false);
                return new MeViewHolder(view);
            case DataChat.YOU_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_chat_you, viewGroup, false);
                return new YouViewHolder(view);
            case DataChat.ENTRY_TYPE:
                view = LayoutInflater.from(viewGroup.getContext()).inflate(R.layout.item_chat_entry, viewGroup, false);
                return new EntryViewHolder(view);
        }
        return null;



//        view = LayoutInflater.from(viewGroup.getContext())
//                .inflate(R.layout.item_chat, viewGroup, false);
//        return new ViewHolder(view);
    }

    @Override
    public int getItemViewType(int position) {
        switch (mItemList.get(position).type) {
            case 0:
                return DataChat.ME_TYPE;
            case 1:
                return DataChat.YOU_TYPE;
            case 2:
                return DataChat.ENTRY_TYPE;
            default:
                return -1;
        }
    }


    //----------------------------------------------------------------------------------------------
    //뷰홀더 연결

    @SuppressLint("ResourceAsColor")
    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder viewHolder, final int i) {

        String date_str;
        String date_item;
        SimpleDateFormat mSimpleDateFormat = new SimpleDateFormat("yyMM월 dd일HHmmssSSaa hh:mm", Locale.KOREA);
        Date currentTime = new Date();
        date_str = mSimpleDateFormat.format(currentTime);
        date_str = date_str.substring(0,9);

        DataChat object = mItemList.get(i);
        if(object != null) {

            String mDate;
            if (object.getDate() == null || object.getDate().length() < 17) {
                mDate = object.getDate();
            } else {
                //오늘과 비교하여, 시간, 날짜 표기법 달리함
                date_item =object.getDate().substring(0,9);
                if(date_item.equals(date_str)){
                    mDate = object.getDate().substring(17);
                } else {
                    mDate = object.getDate().substring(2,9);
                }
            }

            switch (object.type) {
                case DataChat.ME_TYPE:
                    ((MeViewHolder) viewHolder).bubbleMe.setText(object.getMessage());
                    ((MeViewHolder) viewHolder).bubbleMe.setLayoutParams(
                            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    ((MeViewHolder) viewHolder).bubbleMe.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            mListener.meItemBoxClicked(i);
                        }
                    });
                    ((MeViewHolder) viewHolder).dateMe.setText(mDate);
                    break;

                case DataChat.YOU_TYPE:
                    ((YouViewHolder) viewHolder).bubbleYou.setText(object.getMessage());
                    ((YouViewHolder) viewHolder).bubbleYou.setLayoutParams(
                            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
                    ((YouViewHolder) viewHolder).bubbleYou.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mListener.youItemBoxClicked(i);
                        }
                    });
                    ((YouViewHolder) viewHolder).dateYou.setText(mDate);
                    ((YouViewHolder) viewHolder).nickname.setText(object.getNickname());
                    break;

                case DataChat.ENTRY_TYPE:
                    if (object.getMessage().equals("입장")) {
                        ((EntryViewHolder) viewHolder).entText.setText(object.getNickname() + "님이 들어왔습니다.");
                        ((EntryViewHolder) viewHolder).entText2.setText("운영 정책을 위반한 메시지로");
                        ((EntryViewHolder) viewHolder).entText3.setText("채팅 이용에 제한이 있을 수 있습니다.");
                    } else {
                        ((EntryViewHolder) viewHolder).entText.setText(object.getNickname() + "님이 나갔습니다.");
                        ((EntryViewHolder) viewHolder).entText2.setText("위 인원이 채팅 규칙을 준수하지 않았다면");
                        ((EntryViewHolder) viewHolder).entText3.setText("신고 해주시길 바랍니다!");
                    }

                    break;
            }
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

