package com.lzx.demo.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.github.jdsjlzx.interfaces.OnItemClickListener;
import com.github.jdsjlzx.interfaces.OnItemLongClickListener;
import com.github.jdsjlzx.recyclerview.LRecyclerView;
import com.github.jdsjlzx.recyclerview.LRecyclerViewAdapter;
import com.lzx.demo.R;
import com.lzx.demo.base.ListBaseAdapter;
import com.lzx.demo.bean.ItemModel;
import com.lzx.demo.imageloader.ImageLoader;
import com.lzx.demo.imageloader.ImageLoaderUtil;
import com.lzx.demo.util.AppToast;
import com.lzx.demo.util.TLog;
import com.lzx.demo.view.SampleFooter;
import com.lzx.demo.view.SampleHeader;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * 局部刷新
 */
public class PartialRefreshActivity extends AppCompatActivity {

    private LRecyclerView mRecyclerView = null;

    private DataAdapter mDataAdapter = null;

    private LRecyclerViewAdapter mLRecyclerViewAdapter = null;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.sample_ll_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mRecyclerView = (LRecyclerView) findViewById(R.id.list);

        //init data
        ArrayList<ItemModel> dataList = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            ItemModel itemModel = new ItemModel();
            itemModel.title = "item" + i;
            itemModel.imgUrl = "http://img.blog.csdn.net/20160603160856217";
            dataList.add(itemModel);
        }

        mDataAdapter = new DataAdapter(this);
        mDataAdapter.setDataList(dataList);

        mLRecyclerViewAdapter = new LRecyclerViewAdapter(mDataAdapter);
        mRecyclerView.setAdapter(mLRecyclerViewAdapter);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        //add a HeaderView
        View header = LayoutInflater.from(this).inflate(R.layout.sample_header,(ViewGroup)findViewById(android.R.id.content), false);

        mLRecyclerViewAdapter.addHeaderView(header);
        mLRecyclerViewAdapter.addHeaderView(new SampleHeader(this));

        //add a FooterView
        mLRecyclerViewAdapter.addFooterView(new SampleFooter(this));

        //禁止下拉刷新功能
        mRecyclerView.setPullRefreshEnabled(false);

        mLRecyclerViewAdapter.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position) {
                ItemModel item = mDataAdapter.getDataList().get(position);
                AppToast.showShortText(PartialRefreshActivity.this, item.title);
            }

        });

        mLRecyclerViewAdapter.setOnItemLongClickListener(new OnItemLongClickListener() {
            @Override
            public void onItemLongClick(View view, int position) {
                ItemModel item = mDataAdapter.getDataList().get(position);
                AppToast.showShortText(PartialRefreshActivity.this, "onItemLongClick - " + item.title);
            }
        });

    }

    private class DataAdapter extends ListBaseAdapter<ItemModel> {

        private LayoutInflater mLayoutInflater;

        public DataAdapter(Context context) {
            mLayoutInflater = LayoutInflater.from(context);
        }


        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            mContext = parent.getContext();
            return new ViewHolder(mLayoutInflater.inflate(R.layout.list_item_pic, parent, false));
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            bind(holder,position);
        }

        //局部刷新关键：带payload的这个onBindViewHolder方法必须实现
        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position, List payloads) {

            if (payloads.isEmpty()) {
                onBindViewHolder(holder,position);
            } else {

                //注意：payloads的size总是1
                String payload = (String)payloads.get(0);
                TLog.error("payload = " + payload);

                //需要更新的控件
                ItemModel itemModel = mDataList.get(position);
                ViewHolder viewHolder = (ViewHolder) holder;
                viewHolder.textView.setText(itemModel.title);

            }
        }

        private void bind(RecyclerView.ViewHolder holder, int position) {
            ItemModel itemModel = mDataList.get(position);

            ViewHolder viewHolder = (ViewHolder) holder;

            viewHolder.textView.setText(itemModel.title);

            ImageLoaderUtil imageLoaderUtil = new ImageLoaderUtil();
            ImageLoader imageLoader = new ImageLoader.Builder()
                    .imgView(viewHolder.avatarImage)
                    .url(itemModel.imgUrl)
                    //.strategy(ImageLoaderUtil.LOAD_STRATEGY_ONLY_WIFI) 可以不写
                    .build();
            //imageLoaderUtil.setLoadImgStrategy(new GlideImageLoaderStrategy()); //这里可以更改图片加载框架
            imageLoaderUtil.loadImage(mContext, imageLoader);
        }

        @Override
        public int getItemCount() {
            return mDataList.size();
        }

        private class ViewHolder extends RecyclerView.ViewHolder {

            private TextView textView;
            private ImageView avatarImage;

            public ViewHolder(View itemView) {
                super(itemView);
                textView = (TextView) itemView.findViewById(R.id.info_text);
                avatarImage = (ImageView) itemView.findViewById(R.id.avatar_image);
            }
        }
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_partial_refresh, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
        } else if (item.getItemId() == R.id.menu_partial_refresh) {

            int position = 1;//指定列表中的第2个item

            ItemModel itemModel = mDataAdapter.getDataList().get(position);
            itemModel.id = 100;
            itemModel.title = "refresh item " + itemModel.id;
            mDataAdapter.getDataList().set(position,itemModel);

            //RecyclerView局部刷新
            // notifyItemChanged(int position, Object payload) 其中的payload相当于一个标记，类型不限
            mLRecyclerViewAdapter.notifyItemChanged(mLRecyclerViewAdapter.getAdapterPosition(false,position) , "jdsjlzx");

        }
        return true;
    }

}