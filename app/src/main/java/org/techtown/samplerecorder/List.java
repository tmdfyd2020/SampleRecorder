package org.techtown.samplerecorder;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

public class List extends AppCompatActivity {

    Context context;
    Activity activity;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        context = getApplicationContext();

        Toolbar toolbar_main = findViewById(R.id.toolbar_list);
        setSupportActionBar(toolbar_main);
        getSupportActionBar().setTitle("목록");

        // 리사이클러뷰에 표시할 데이터 리스트 생성.
        File filePath = new File("/mnt/sdcard/audioDrop/");
        if (filePath.exists() == false) {
            return;
        }

        File[] files = filePath.listFiles();
        ArrayList<File> arrayList = new ArrayList<>();
        Collections.sort(arrayList);

        for (int i = 0; i < files.length; i++) {
            if(!files[i].isHidden() && files[i].isFile()) {
                arrayList.add(files[i]);
            }
        }

        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 리사이클러뷰에 SimpleTextAdapter 객체 지정.
        Adapter adapter = new Adapter(arrayList, context);
        recyclerView.setAdapter(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_list_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.list_back:
                finish();
                break;
        }

        return true;
    }
}