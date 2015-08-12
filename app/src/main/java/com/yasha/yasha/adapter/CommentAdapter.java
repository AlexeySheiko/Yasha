package com.yasha.yasha.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseUser;
import com.squareup.picasso.Picasso;
import com.yasha.yasha.CircleTransform;
import com.yasha.yasha.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class CommentAdapter extends ArrayAdapter<ParseObject> {

    public CommentAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.comment_list_item, parent, false);

        final TextView nameView = (TextView) convertView.findViewById(R.id.name_textview);
        final TextView messageView = (TextView) convertView.findViewById(R.id.message_textview);
        final ParseImageView avatarView = (ParseImageView) convertView.findViewById(R.id.avatar_imageview);

        final ParseObject post = getItem(position);
        messageView.setText(post.getString("message"));

        final ParseUser author = post.getParseUser("author");
            author.fetchInBackground(new GetCallback<ParseUser>() {
                @Override
                public void done(ParseUser author, ParseException e) {
                    if (e == null) {
                        nameView.setText(author.getUsername());
                    }
                }
            });

        ParseFile avatarFile = author.getParseFile("avatar");
        if (avatarFile != null) {
            avatarFile.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    File tempFile = null;
                    try {
                        tempFile = File.createTempFile("abc", "cba", null);
                        FileOutputStream fos = new FileOutputStream(tempFile);
                        fos.write(bytes);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }

                    Picasso.with(getContext())
                            .load(tempFile)
                            .placeholder(R.drawable.avatar_placeholder)
                            .fit()
                            .transform(new CircleTransform())
                            .noFade()
                            .into(avatarView);
                }
            });
        }


//        View.OnClickListener userClickListener = new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                Intent intent = new Intent(getContext(), HistoryActivity.class);
//                getContext().startActivity(intent);
//            }
//        };
//        convertView.setOnClickListener(userClickListener);

        return convertView;
    }
}
