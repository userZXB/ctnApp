package com.example.zxb.ctnclassifier;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.PagerSnapHelper;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class MainActivity extends AppCompatActivity {

    private Button infoBtn;
    private Button picBtn;
    private Button camBtn;
    /*声明一个含有四个图片的列表*/
    private List<Integer> list = new ArrayList<>(4);
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        /*存入图片,设置轮播图*/
        list.add(R.drawable.b1);
        list.add(R.drawable.b2);
        list.add(R.drawable.b3);
        list.add(R.drawable.b4);
        BannerAdapter adapter = new BannerAdapter(this, list);
        final RecyclerView recyclerView = findViewById(R.id.recycler);
        final SmoothLinearLayoutManager layoutManager = new SmoothLinearLayoutManager(this, 0,false);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setHasFixedSize(true);
        recyclerView.setAdapter(adapter);
        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);
        recyclerView.scrollToPosition(list.size()*10);
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    int i = layoutManager.findFirstVisibleItemPosition() % list.size();
                    //得到指示器红点的位置
                }
            }
        });
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        scheduledExecutorService.scheduleAtFixedRate(new Runnable(){
            @Override
            public void run() {
                recyclerView.smoothScrollToPosition(layoutManager.findFirstVisibleItemPosition() + 1);
            }
        },2000,2000, TimeUnit.MILLISECONDS);
        // 根据id名称找到按钮
        infoBtn = findViewById(R.id.info);
        infoBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent=new Intent();
                intent.setClass(MainActivity.this,infoActivity.class);
                startActivity(intent);
            }

        });
        picBtn = findViewById(R.id.picture);
        picBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent=new Intent();
                intent.setClass(MainActivity.this,pictureActivity.class);
                startActivity(intent);
            }

        });
        camBtn = findViewById(R.id.camera);

        camBtn.setOnClickListener(new View.OnClickListener(){
            public void onClick(View view){
                Intent intent=new Intent();
                intent.setClass(MainActivity.this,cameraActivity.class);
                startActivity(intent);
            }

        });

    }
}
