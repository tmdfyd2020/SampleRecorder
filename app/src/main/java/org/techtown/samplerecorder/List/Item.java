package org.techtown.samplerecorder.List;

public class Item {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}

// ListActivity
//public class ListActivity extends AppCompatActivity {
//
//    private final String TAG = this.getClass().getSimpleName();
//
//    Context context;
//
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_list);
//
//        context = getApplicationContext();
//
//        Toolbar toolbar_list = findViewById(R.id.toolbar_list);
//        setSupportActionBar(toolbar_list);
//        getSupportActionBar().setTitle("목록");
//
//        Intent myIntent = getIntent();
//        int sampleRate = myIntent.getIntExtra(getString(R.string.rate), 16000);
//        int bufferSize = myIntent.getIntExtra(getString(R.string.buffer_size), 1024);
//
//        // 리사이클러뷰에 표시할 데이터 리스트 생성.
////        File filePath = new File("/mnt/sdcard/audioDrop/");
//        File file = new File(MainActivity.Companion.getFilePath());
//        if (file.exists() == false) {
//            return;
//        }
//
//        File[] files = file.listFiles();
//        ArrayList<File> arrayList = new ArrayList<>();
//
//        for (int i = 0; i < files.length; i++) {
//            if(!files[i].isHidden() && files[i].isFile()) {
//                arrayList.add(files[i]);
//            }
//        }
//        Collections.sort(arrayList);  // 우선 이름 순서대로 출력
//
//        // 리사이클러뷰에 LinearLayoutManager 객체 지정.
//        RecyclerView recyclerView = findViewById(R.id.list_recyclerview);
//        recyclerView.setLayoutManager(new LinearLayoutManager(this));
//
//        // 리사이클러뷰에 SimpleTextAdapter 객체 지정.
//        Adapter adapter = new Adapter(arrayList, context, sampleRate, bufferSize);
//        recyclerView.setAdapter(adapter);
//    }
//
//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        MenuInflater menuInflater = getMenuInflater();
//        menuInflater.inflate(R.menu.menu_list_toolbar, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        switch (item.getItemId()) {
//
//            case R.id.list_back:
//                finish();
//                break;
//        }
//
//        return true;
//    }
//}

// Adapter

//public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
//
//    private final String TAG = this.getClass().getSimpleName();
//
//    private final ArrayList<File> fileList;
//    Context listContext;
//    int sampleRate, bufferSize;
//    AudioTrack audioTrack;
//    boolean seekbar_touch = false;
//    int previous_position = -1, move_pointer;
//    String btn_type = "play_button";
//    Thread playThread;
//    boolean press_pause = false, resume = false, complete_play;
//    long pause_point;
//
//    public class ViewHolder extends RecyclerView.ViewHolder {
//        ImageView imageView;
//        TextView textView;
//        SeekBar seekBar;
//
//        ViewHolder(View itemView) {
//            super(itemView);
//
//            textView = itemView.findViewById(R.id.item_text);
//            imageView = itemView.findViewById(R.id.btn_item_play);
//            seekBar = itemView.findViewById(R.id.seekbar_playState);
//
//            itemView.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//
//                }
//            });
//
//            // if click itemView on LongClick, show delete dialog
//            itemView.setOnLongClickListener(new View.OnLongClickListener() {
//                @Override
//                public boolean onLongClick(View v) {
//                    File file = fileList.get(getAdapterPosition());
//
//                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
//                    builder.setTitle("Delete")
//                            .setMessage("Are you sure you want to delete?")
//                            .setIcon(listContext.getDrawable(R.drawable.png_delete))
//                            .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Yes</font>"), new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    file.delete();
//                                    fileList.remove(getAdapterPosition());
//                                    notifyItemRemoved(getAdapterPosition());
//                                    notifyItemRangeChanged(getAdapterPosition(), fileList.size());
//                                    Toast.makeText(listContext, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
//                                }
//                            })
//                            .setNegativeButton(Html.fromHtml("<font color='#F06292'>No</font>"), new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.cancel();
//                                }
//                            });
//
//                    AlertDialog dialog = builder.create();
//                    dialog.getWindow().setGravity(Gravity.CENTER);
//                    dialog.show();
//
//                    return false;
//                }
//            });
//        }
//    }
//
//    Adapter(ArrayList<File> list, Context context, int sampleRate, int bufferSize) {
//        fileList = list;
//        listContext = context;
//        this.sampleRate = sampleRate;
//        this.bufferSize = bufferSize;
//    }
//
//    @Override
//    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        Context context = parent.getContext();
//        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
//
//        View view = inflater.inflate(R.layout.item, parent, false);
//        Adapter.ViewHolder vh = new Adapter.ViewHolder(view);
//
//        return vh;
//    }
//
//    @Override
//    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
//        File file = fileList.get(position);
//        holder.textView.setText(file.getName());
//        holder.seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
//            @Override  // if drag seekbar
//            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
//                move_pointer = progress;
//            }
//
//            @Override  // if start touch seekbar thumb
//            public void onStartTrackingTouch(SeekBar seekBar) {
//                seekbar_touch = true;
//            }
//
//            @Override  // if stop touch seekbar thumb
//            public void onStopTrackingTouch(SeekBar seekBar) {
//                // Thread 실행 시키면 될 것 같다.
//            }
//        });
//        holder.imageView.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                switch(btn_type) {
//                    case "play_button":
//                        btn_type = "pause_button";
//                        complete_play = true;
//
//                        if (previous_position != -1 && previous_position != position) {
//                            notifyItemChanged(previous_position, "click");  // hide previous position seekbar
//                            pause_point = 0;
//                            resume = false;
//                        }
//                        previous_position = position;
//
//                        holder.imageView.setImageResource(R.drawable.png_pause);
//                        holder.seekBar.setVisibility(View.VISIBLE);
//
//                        playThread = new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                if (audioTrack == null) {
//                                    audioTrack = new android.media.AudioTrack.Builder()
//                                            .setAudioAttributes(new AudioAttributes.Builder()
//                                                    .setUsage(AudioAttributes.USAGE_MEDIA)
//                                                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
//                                                    .build())
//                                            .setAudioFormat(new AudioFormat.Builder()
//                                                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
//                                                    .setSampleRate(sampleRate)
//                                                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
//                                                    .build())
//                                            .setBufferSizeInBytes(bufferSize * 2)
//                                            .build();
//                                }
//
//                                byte[] data = new byte[bufferSize]; // small buffer size to not overflow AudioTrack's internal buffer
//
//                                RandomAccessFile randomFile = null;
//                                try {
//                                    randomFile = new RandomAccessFile(file, "rw");
//                                } catch (FileNotFoundException e) {
//                                    e.printStackTrace();
//                                }
//
//                                holder.seekBar.setMin(0);
//                                try {
//                                    holder.seekBar.setMax((int) randomFile.length());
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//
//                                audioTrack.play();
//
//                                if (!resume) {
//                                    int i = 0;
//                                    while (i != -1) { // run until file ends
//                                        try {
//                                            i = randomFile.read(data);
//                                            audioTrack.write(data, 0, i);
//                                            holder.seekBar.setProgress((int) randomFile.getFilePointer());
//
//                                            if (press_pause) {
//                                                pause_point = randomFile.getFilePointer();
//                                                complete_play = false;
//                                                press_pause = false;
//                                                break;
//                                            }
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                } else {  // if click pause button and then resume play,
//                                    resume = false;
//                                    try {
//                                        randomFile.seek((int) pause_point);
//                                    } catch (IOException e) {
//                                        e.printStackTrace();
//                                    }
//                                    int j = 0;
//                                    while (j != -1) { // run until file ends
//                                        try {
//                                            j = randomFile.read(data);
//                                            audioTrack.write(data, 0, j);
//                                            holder.seekBar.setProgress((int) randomFile.getFilePointer());
//
//                                            if (press_pause) {
//                                                pause_point = randomFile.getFilePointer();
//                                                complete_play = false;
//                                                press_pause = false;
//                                                break;
//                                            }
//                                        } catch (IOException e) {
//                                            e.printStackTrace();
//                                        }
//                                    }
//                                }
//
//                                audioTrack.stop();
//                                audioTrack.release();
//                                audioTrack = null;
//
//                                try {
//                                    randomFile.close();
//                                } catch (IOException e) {
//                                    e.printStackTrace();
//                                }
//
//                                if (complete_play) {  // if complete to play audio file until end point,
//                                    holder.imageView.setImageResource(R.drawable.png_play);
//                                    btn_type = "play_button";
//                                }
//                            }
//                        });
//
//                        playThread.start();
//                        break;
//
//                    case "pause_button":
//                        btn_type = "play_button";
//                        press_pause = true;
//                        resume = true;
//
//                        holder.imageView.setImageResource(R.drawable.png_play);
//                        break;
//                }
//            }
//        });
//    }
//
//    @Override
//    public void onBindViewHolder(@NonNull Adapter.ViewHolder holder, int position, @NonNull List<Object> payloads) {
//        super.onBindViewHolder(holder, position, payloads);
//        // by performing upper condition, hide previous position holder seekbar
//        holder.seekBar.setVisibility(View.GONE);
//    }
//
//    @Override
//    public int getItemCount() {
//        return fileList.size();
//    }
//
//}