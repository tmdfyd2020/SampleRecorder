package org.techtown.samplerecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import org.techtown.samplerecorder.Main.myLog;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private ArrayList<File> fileList;
    Context listContext;
    int sampleRate, bufferSize;
    AudioTrack audioTrack;
    boolean state_playing = false;
    SeekBar seekBar;
    int len = 0;
    int seek = 0;

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.item_text);
            imageView = itemView.findViewById(R.id.btn_item_play);
            seekBar = itemView.findViewById(R.id.seekbar_playState);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                }
            });

            // if click itemView on LongClick, show delete dialog
            itemView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    File file = fileList.get(getAdapterPosition());

                    AlertDialog.Builder builder = new AlertDialog.Builder(v.getContext(), android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar);
                    builder.setTitle("Delete")
                            .setMessage("Are you sure you want to delete?")
                            .setIcon(listContext.getDrawable(R.drawable.png_delete))
                            .setPositiveButton(Html.fromHtml("<font color='#3399FF'>Yes</font>"), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    file.delete();
                                    fileList.remove(getAdapterPosition());
                                    notifyItemRemoved(getAdapterPosition());
                                    notifyItemRangeChanged(getAdapterPosition(), fileList.size());
                                    Toast.makeText(listContext, "파일이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                                }
                            })
                            .setNegativeButton(Html.fromHtml("<font color='#F06292'>No</font>"), new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.cancel();
                                }
                            });

                    AlertDialog dialog = builder.create();
                    dialog.getWindow().setGravity(Gravity.CENTER);
                    dialog.show();

                    return false;
                }
            });
        }
    }

    Adapter(ArrayList<File> list, Context context, int sampleRate, int bufferSize) {
        fileList = list;
        listContext = context;
        this.sampleRate = sampleRate;
        this.bufferSize = bufferSize;
    }

    // onCreateViewHolder() : 아이템 뷰를 위한 뷰홀더 객체 생성하여 리턴.
    @Override
    public Adapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

        View view = inflater.inflate(R.layout.item, parent, false);
        Adapter.ViewHolder vh = new Adapter.ViewHolder(view);

        return vh;
    }

    // onBindViewHolder() - position에 해당하는 데이터를 뷰홀더의 아이템뷰에 표시.
    @Override
    public void onBindViewHolder(Adapter.ViewHolder holder, int position) {
        File file = fileList.get(position);
        holder.textView.setText(file.getName());
        holder.imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (state_playing == false) {  // if click play image button,

                    state_playing = true;
                    holder.imageView.setImageResource(R.drawable.png_pause);

                    Thread playThread = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (audioTrack == null) {
                                audioTrack = new android.media.AudioTrack.Builder()
                                        .setAudioAttributes(new AudioAttributes.Builder()
                                                .setUsage(AudioAttributes.USAGE_MEDIA)
                                                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                                                .build())
                                        .setAudioFormat(new AudioFormat.Builder()
                                                .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                                                .setSampleRate(sampleRate)
                                                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                                                .build())
                                        .setBufferSizeInBytes(bufferSize * 2)
                                        .build();
                            }

                            byte[] data = new byte[bufferSize]; // small buffer size to not overflow AudioTrack's internal buffer

                            RandomAccessFile randomFile = null;

                            try {
                                randomFile = new RandomAccessFile(file, "rw");
                            } catch (FileNotFoundException e) {
                                e.printStackTrace();
                            }

                            audioTrack.play();

                            /*
                             ○ randomFile.getFilePointer() :: 진행 중인 파일 포인터의 위치
                             ○ randomFile.length() :: 실행할 파일의 총 길이 (getFilePointer()의 최댓값)
                             ○ randomFile.seek(long position) :: position으로 파일의 포인터를 이동
                             */
                            int i = 0;
                            while (i != -1) { // run until file ends
                                try {
                                    i = randomFile.read(data);
                                    len = audioTrack.write(data, 0, i);

                                    if(state_playing == false) {
                                        break;
                                    }
                                }
                                catch (IOException e) {
                                    e.printStackTrace();
                                }
                            }

                            try {
                                randomFile.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }

                            if(state_playing == true) {
                                audioTrack.stop();
                                audioTrack.release();
                                audioTrack = null;
                            }
                            state_playing = false;
                            holder.imageView.setImageResource(R.drawable.png_play);
                        }
                    });
                    playThread.start();
                }

                else {  // if click pause image button,
                    state_playing = false;
                    audioTrack.stop();
                    audioTrack.release();
                    audioTrack = null;
                    holder.imageView.setImageResource(R.drawable.png_play);

                }


            }
        });
    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return fileList.size();
    }

}
