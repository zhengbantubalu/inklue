package com.bupt.inklue.activity;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bupt.inklue.R;
import com.bupt.inklue.adapter.CharCardAdapter;
import com.bupt.inklue.adapter.CharCardDecoration;
import com.bupt.inklue.data.CardData;
import com.bupt.inklue.data.CardsData;
import com.bupt.inklue.data.DatabaseHelper;

//作业详情页面
public class PracticeActivity extends AppCompatActivity implements View.OnClickListener {

    private CharCardAdapter adapter;//卡片适配器
    private CardData practiceCardData;//练习数据
    private String charIDs;//汉字ID列表
    private CardsData charCardsData;//汉字卡片数据

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_practice);

        //取得练习数据
        getPracticeData();

        //设置练习标题
        TextView textView = findViewById(R.id.textview_title);
        textView.setText(practiceCardData.getName());

        //取得汉字卡片数据
        getCardsData();

        //初始化RecyclerView
        initRecyclerView();

        //RecyclerView中项目的点击监听器
        adapter.setOnItemClickListener(this::startImageActivity);

        //设置按钮的点击监听器
        findViewById(R.id.button_back).setOnClickListener(this);
        findViewById(R.id.button_start).setOnClickListener(this);
    }

    //点击事件回调
    public void onClick(View view) {
        if (view.getId() == R.id.button_back) {
            finish();
        } else if (view.getId() == R.id.button_start) {
            startWritingActivity();
        }
    }

    //取得练习数据
    private void getPracticeData() {
        practiceCardData = new CardData();
        try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            long id = getIntent().getLongExtra("practiceCardID", 0);
            Cursor cursor = db.rawQuery("SELECT * FROM Practice WHERE id = " + id, null);
            int nameIndex = cursor.getColumnIndex("name");
            int coverImgPathIndex = cursor.getColumnIndex("coverImgPath");
            int charIDsIndex = cursor.getColumnIndex("charIDs");
            if (cursor.moveToFirst()) {
                practiceCardData.setName(cursor.getString(nameIndex));
                practiceCardData.setStdImgPath(cursor.getString(coverImgPathIndex));
                charIDs = cursor.getString(charIDsIndex);
            }
            cursor.close();
        }
    }

    //取得汉字卡片数据
    private void getCardsData() {
        charCardsData = new CardsData();
        String[] idArray = charIDs.split(",");
        try (DatabaseHelper dbHelper = new DatabaseHelper(this)) {
            SQLiteDatabase db = dbHelper.getWritableDatabase();
            for (String id : idArray) {
                Cursor cursor = db.rawQuery("SELECT * FROM StdChar WHERE id = " +
                        id, null);
                int name = cursor.getColumnIndex("name");
                int stdImgPath = cursor.getColumnIndex("stdImgPath");
                if (cursor.moveToFirst()) {
                    do {
                        CardData cardData = new CardData();
                        cardData.setID(Long.parseLong(id));
                        cardData.setName(cursor.getString(name));
                        cardData.setStdImgPath(cursor.getString(stdImgPath));
                        charCardsData.add(cardData);
                    } while (cursor.moveToNext());
                }
                cursor.close();
            }
            db.close();
        }
    }

    //启动图片查看页面
    private void startImageActivity(int position) {
        Intent intent = new Intent();
        intent.setClass(this, ImageActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("charCardsData", charCardsData);
        bundle.putInt("position", position);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    //启动书写页面
    private void startWritingActivity() {
        Intent intent = new Intent();
        intent.setClass(this, WritingActivity.class);
        Bundle bundle = new Bundle();
        bundle.putSerializable("charCardsData", charCardsData);
        bundle.putSerializable("practiceCardData", practiceCardData);
        intent.putExtras(bundle);
        startActivity(intent);
    }

    //初始化RecyclerView
    private void initRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.recyclerview_practice);
        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        recyclerView.setLayoutManager(layoutManager);//设置布局管理器
        int spacing = getResources().getDimensionPixelSize(R.dimen.spacing);
        CharCardDecoration decoration = new CharCardDecoration(spacing);
        recyclerView.addItemDecoration(decoration);//设置间距装饰类
        adapter = new CharCardAdapter(this, charCardsData);
        recyclerView.setAdapter(adapter);//设置卡片适配器
    }
}
