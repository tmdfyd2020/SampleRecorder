package org.techtown.samplerecorder;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {

    private ArrayList<File> fileList;
    Context listContext;

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        ViewHolder(View itemView) {
            super(itemView);

            textView = itemView.findViewById(R.id.item_text);

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
                            .setIcon(listContext.getDrawable(R.drawable.delete))
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

    Adapter(ArrayList<File> list, Context context) {
        fileList = list;
        listContext = context;
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
    }

    // getItemCount() - 전체 데이터 갯수 리턴.
    @Override
    public int getItemCount() {
        return fileList.size();
    }

}
