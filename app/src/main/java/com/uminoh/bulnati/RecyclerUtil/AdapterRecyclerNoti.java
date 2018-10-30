package com.uminoh.bulnati.RecyclerUtil;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.TextView;

import com.uminoh.bulnati.R;

import java.util.List;

public class AdapterRecyclerNoti extends RecyclerView.Adapter<AdapterRecyclerNoti.ViewHolder> {

    //받아올 데이터리스트
    private List<DataNoti> mItemList;
    private Context context;

    //----------------------------------------------------------------------------------------------

    //생성자
    public AdapterRecyclerNoti(Context context, List<DataNoti> mDataList) { //어레이리스트 셋온 (생성자)
        mItemList = mDataList;
        this.context = context;
    }

    //----------------------------------------------------------------------------------------------

    //인터페이스(메쏘드를 공유하는 간이 클래스 = 도구)
    public interface AdapterRecyclerNotiClickListener { //메쏘드 선언
        void ItemBoxClicked(int i);
        void SwitchClicked(int i, boolean b);
    }

    private AdapterRecyclerNoti.AdapterRecyclerNotiClickListener mListener; //인터페이스 객체 선언

    public void setOnClickListener(AdapterRecyclerNoti.AdapterRecyclerNotiClickListener listener) { //객체 셋온
        mListener = listener;
    }

    //----------------------------------------------------------------------------------------------

    //뷰홀더(=틀, 리사이클러뷰 틀에 쓰일 아이템 선언)
    public static class ViewHolder extends RecyclerView.ViewHolder {

        TextView notiTitle;
        TextView notiBroad;
        TextView notiWeek;
        Switch notiSwitch;

        //뷰홀더와 뷰 연결
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            notiTitle = itemView.findViewById(R.id.noti_title);
            notiBroad = itemView.findViewById(R.id.noti_broad);
            notiWeek = itemView.findViewById(R.id.noti_week);
            notiSwitch = itemView.findViewById(R.id.noti_switch);
        }
    }

    //----------------------------------------------------------------------------------------------

    //뷰홀더 생성
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View view;
        view = LayoutInflater.from(viewGroup.getContext())
                .inflate(R.layout.item_noti, viewGroup, false);
        return new ViewHolder(view);
    }

    //----------------------------------------------------------------------------------------------
    //뷰홀더 연결

    @Override
    public void onBindViewHolder(@NonNull ViewHolder viewHolder, final int i) {

        viewHolder.notiTitle.setText(mItemList.get(i).getTitle());
        viewHolder.notiBroad.setText(mItemList.get(i).getBroad());
        viewHolder.notiWeek.setText(mItemList.get(i).getWeek());
        viewHolder.notiSwitch.setChecked(mItemList.get(i).getCheck());

        if (mListener != null) {
            viewHolder.notiTitle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mListener.ItemBoxClicked(i);
                }
            });
            viewHolder.notiSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                    mListener.SwitchClicked(i, b);
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
