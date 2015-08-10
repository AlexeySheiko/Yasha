package com.yasha.yasha.adapter;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseUser;
import com.yasha.yasha.HistoryActivity;
import com.yasha.yasha.R;

public class UserAdapter extends ArrayAdapter<ParseUser> {

    public UserAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.user_list_item, parent, false);
        }

        final TextView nameView = (TextView) convertView.findViewById(R.id.name_textview);
        final TextView cityView = (TextView) convertView.findViewById(R.id.city_textview);
        final ParseImageView avatarView = (ParseImageView) convertView.findViewById(R.id.avatar_imageview);

        ParseUser user = getItem(position);
        nameView.setText(user.getUsername());
        cityView.setText(user.getString("city"));

        ParseFile avatarFile = user.getParseFile("avatar");
        avatarView.setPlaceholder(getContext().getResources().getDrawable(R.drawable.avatar_placeholder));
        avatarView.setParseFile(avatarFile);
        avatarView.loadInBackground();

        View.OnClickListener userClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), HistoryActivity.class);
                getContext().startActivity(intent);
            }
        };
        convertView.setOnClickListener(userClickListener);

        return convertView;
    }
}