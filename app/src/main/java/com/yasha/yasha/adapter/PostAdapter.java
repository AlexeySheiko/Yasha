package com.yasha.yasha.adapter;

import android.content.Context;
import android.content.Intent;
import android.support.v7.widget.PopupMenu;
import android.view.LayoutInflater;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.parse.CountCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseImageView;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.yasha.yasha.CommentActivity;
import com.yasha.yasha.R;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class PostAdapter extends ArrayAdapter<ParseObject> {

    public PostAdapter(Context context) {
        super(context, 0);
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext())
                    .inflate(R.layout.message_list_item, parent, false);
        }

        final TextView authorView = (TextView) convertView.findViewById(R.id.author_textview);
        final ParseImageView avatarView = (ParseImageView) convertView.findViewById(R.id.avatar_imageview);
        final TextView messageView = (TextView) convertView.findViewById(R.id.message_textview);
        final TextView dateView = (TextView) convertView.findViewById(R.id.date_textview);
        final TextView categoryView = (TextView) convertView.findViewById(R.id.category_textview);
        final TextView counterView = (TextView) convertView.findViewById(R.id.messages_counter);
        final View buttonMore = convertView.findViewById(R.id.button_more);

        final ParseObject post = getItem(position);
        messageView.setText(post.getString("message"));
        dateView.setText(formatDate(post.getCreatedAt()));
        categoryView.setText(post.getString("category"));

        final ParseUser author = post.getParseUser("author");
        authorView.setText(author.getUsername());

        ParseFile avatarFile = author.getParseFile("avatar");
        avatarView.setPlaceholder(getContext().getResources().getDrawable(R.drawable.avatar_placeholder));
        avatarView.setParseFile(avatarFile);
        avatarView.loadInBackground();

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Comment");
        query.whereEqualTo("post", post);
        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        query.countInBackground(new CountCallback() {
            @Override
            public void done(int count, ParseException e) {
                counterView.setText(String.valueOf(count));
            }
        });

        View.OnClickListener commentsClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getContext(), CommentActivity.class);
                intent.putExtra("postId", post.getObjectId());
                getContext().startActivity(intent);
            }
        };
        counterView.setOnClickListener(commentsClickListener);
        convertView.setOnClickListener(commentsClickListener);

        buttonMore.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopup(v, author);
            }
        });

        return convertView;
    }

    private void showPopup(View v, ParseUser author) {
        PopupMenu popup = new PopupMenu(getContext(), v);
        MenuInflater inflater = popup.getMenuInflater();

        int menuRes;

        if (ParseUser.getCurrentUser().getUsername().equals(author.getUsername())) {
            menuRes = R.menu.menu_post_me;
        } else {
            menuRes = R.menu.menu_post_others;
        }


        inflater.inflate(menuRes, popup.getMenu());
        popup.show();
    }

    private String formatDate(Date date) {
        if (isToday(date)) {
            DateFormat dateFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            return dateFormat.format(date);
        } else if (isYesterday(date)) {
            return "Yesterday";
        } else {
            DateFormat dateFormat = new SimpleDateFormat("MMMM d", Locale.getDefault());
            return dateFormat.format(date);
        }
    }

    public static boolean isToday(Date date1) {
        return isSameDay(date1, Calendar.getInstance(TimeZone.getTimeZone("UTC")).getTime());
    }

    public static boolean isYesterday(Date date1) {
        Date date2 = new Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L);
        return isSameDay(date1, date2);
    }

    public static boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal1.setTime(date1);
        Calendar cal2 = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal2.setTime(date2);
        return isSameDay(cal1, cal2);
    }

    public static boolean isSameDay(Calendar cal1, Calendar cal2) {
        return (cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR));
    }
}
