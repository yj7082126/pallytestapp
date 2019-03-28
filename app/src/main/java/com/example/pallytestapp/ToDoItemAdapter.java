package com.example.pallytestapp;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter to bind a ToDoItem List to a view
 */
public class ToDoItemAdapter extends RecyclerView.Adapter<ToDoItemAdapter.ViewHolder> {

    /**
     * Adapter context
     */
    Context mContext;

    /**
     * Adapter View layout
     */
    int mLayoutResourceId;

    ToDoActivity mFragment;

    List<ToDoItem> mResults;

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView nameTextView;
        public CheckBox checkButton;

        public ViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.TextToDoItem);
            checkButton = itemView.findViewById(R.id.checkToDoItem);
        }
    }

    public ToDoItemAdapter(Context context, int layoutResourceId, ToDoActivity fragment) {
        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mFragment = fragment;
        mResults = new ArrayList<ToDoItem>();
    }

    public ToDoItemAdapter(Context context, int layoutResourceId, ArrayList<ToDoItem> resultList) {
        mContext = context;
        mLayoutResourceId = layoutResourceId;
        mResults = resultList;
    }

    public void setList(ArrayList<ToDoItem> newList) {
        mResults = newList;
    }

    public void clearList() {
        mResults = new ArrayList<ToDoItem>();
    }

    public void addList(ToDoItem todo) {
        mResults.add(todo);
    }

    @NonNull
    @Override
    public ToDoItemAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View row = inflater.inflate(mLayoutResourceId, viewGroup, false);
        ViewHolder viewHolder = new ViewHolder(row);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ToDoItemAdapter.ViewHolder viewHolder, int i) {
        final ToDoItem currentItem = mResults.get(i);

        final TextView textView = viewHolder.nameTextView;
        final CheckBox checkBox = viewHolder.checkButton;

        textView.setText(currentItem.getText());
        if (currentItem.isComplete()) {
            checkBox.setChecked(true);
        }
        else {
            checkBox.setChecked(false);
        }
        checkBox.setEnabled(true);
        checkBox.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View arg0) {
                if (checkBox.isChecked()) {
                    checkBox.setEnabled(false);
                        mFragment.checkItem(currentItem);
                }
            }
        });
    }


    @Override
    public int getItemCount() {
        return mResults.size();
    }
}