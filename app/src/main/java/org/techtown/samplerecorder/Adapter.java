package org.techtown.samplerecorder;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import org.techtown.samplerecorder.Main.myLog;

import java.io.File;
import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private ArrayList<File> fileList;

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.item_text);

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        myLog.d(String.valueOf(pos) + "번째 아이템 클릭!");
                    }
                    // 내가 구현해야 할 것은 "꾹 누르면 해당 파일을 삭제하는 다이얼로그 띄우기"
                }


            });
        }
    }

    Adapter(ArrayList<File> list) {
        fileList = list;
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
//        String text = arrayList.get(position);
        File file = fileList.get(position);
        holder.textView.setText(file.getName());
    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return fileList.size();
    }
}
