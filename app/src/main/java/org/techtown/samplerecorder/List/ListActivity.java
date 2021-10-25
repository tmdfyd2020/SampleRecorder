package org.techtown.samplerecorder.List;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.techtown.samplerecorder.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;

import static org.techtown.samplerecorder.Main.MainActivity.filePath;

public class ListActivity extends AppCompatActivity {

    private final String TAG = this.getClass().getSimpleName();

    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_list);

        context = getApplicationContext();

        Toolbar toolbar_list = findViewById(R.id.toolbar_list);
        setSupportActionBar(toolbar_list);
        getSupportActionBar().setTitle("목록");

        Intent myIntent = getIntent();
        int sampleRate = myIntent.getIntExtra(getString(R.string.rate), 16000);
        int bufferSize = myIntent.getIntExtra(getString(R.string.buffer_size), 1024);

        // 리사이클러뷰에 표시할 데이터 리스트 생성.
//        File filePath = new File("/mnt/sdcard/audioDrop/");
        File file = new File(filePath);
        if (file.exists() == false) {
            return;
        }

        File[] files = file.listFiles();
        ArrayList<File> arrayList = new ArrayList<>();

        for (int i = 0; i < files.length; i++) {
            if(!files[i].isHidden() && files[i].isFile()) {
                arrayList.add(files[i]);
            }
        }
        Collections.sort(arrayList);  // 우선 이름 순서대로 출력

        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
        RecyclerView recyclerView = findViewById(R.id.list_recyclerview);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 리사이클러뷰에 SimpleTextAdapter 객체 지정.
        Adapter adapter = new Adapter(arrayList, context, sampleRate, bufferSize);
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