package com.uminoh.bulnati.RecyclerUtil;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.view.PagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.uminoh.bulnati.R;

public class MyGuideViewPagerAdapter extends PagerAdapter {

    private int[] images = {R.drawable.intro1,
            R.drawable.intro2,
            R.drawable.intro3,
            R.drawable.intro4};
    private LayoutInflater inflater;
    private Context context;

    //전달 받은 LayoutInflater 멤버변수로 전달
    public MyGuideViewPagerAdapter(Context context){
        this.context = context;
    }

    //PagerAdapter 가지고 잇는 View 개수를 리턴
    @Override
    public int getCount() { return images.length; }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.viewpager_childview, container,false);
        ImageView imageView = view.findViewById(R.id.img_viewpager_childImage);
        imageView.setImageResource(images[position]);
        container.addView(view);
        return view;
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.invalidate();
    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object o) {
        return view == o;
    }

}
