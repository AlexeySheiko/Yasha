package com.yasha.yasha;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.FindCallback;
import com.parse.GetCallback;
import com.parse.GetDataCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.squareup.picasso.Picasso;
import com.yasha.yasha.adapters.CommentAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class CommentActivity extends AppCompatActivity {

    private ParseObject mPost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comment);

        findViewById(R.id.date_textview).setVisibility(View.GONE);
        findViewById(R.id.category_textview).setVisibility(View.GONE);
        findViewById(R.id.messages_counter).setVisibility(View.GONE);
        findViewById(R.id.button_more).setVisibility(View.GONE);

        String postId = getIntent().getStringExtra("postId");

        ParseQuery<ParseObject> query = ParseQuery.getQuery("Post");
        query.include("author");
        query.include("comments");
        query.setCachePolicy(ParseQuery.CachePolicy.NETWORK_ELSE_CACHE);
        query.getInBackground(postId, new GetCallback<ParseObject>() {
            @Override
            public void done(ParseObject post, ParseException e) {
                if (e == null) {
                    mPost = post;
                    showPostContents();
                    showCommentList();
                }
            }
        });
    }

    private void showPostContents() {
        ParseUser author = mPost.getParseUser("author");

        TextView authorView = (TextView) findViewById(R.id.author_textview);
        TextView messageView = (TextView) findViewById(R.id.message_textview);
        final ImageView avatarView = (ImageView) findViewById(R.id.avatar_imageview);

        messageView.setText(mPost.getString("message"));
        authorView.setText(author.getUsername());

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

                    Picasso.with(CommentActivity.this)
                            .load(tempFile)
                            .placeholder(R.drawable.avatar_placeholder)
                            .fit()
                            .transform(new CircleTransform())
                            .noFade()
                            .into(avatarView);
                }
            });
        }
    }

    private void showCommentList() {
        final CommentAdapter adapter = new CommentAdapter(this);

        ListView commentList = (ListView) findViewById(R.id.comment_list);
        commentList.setAdapter(adapter);

        ParseQuery<ParseObject> query = new ParseQuery<>("Comment");
        query.whereEqualTo("post", mPost);
        query.include("author");
        query.setCachePolicy(ParseQuery.CachePolicy.CACHE_THEN_NETWORK);
        query.findInBackground(new FindCallback<ParseObject>() {
            @Override
            public void done(List<ParseObject> comments, ParseException e) {
                if (e == null) {
                    adapter.clear();
                    adapter.addAll(comments);

                    markAsRead(comments);
                }
            }
        });
    }

    private void markAsRead(List<ParseObject> comments) {
        for (final ParseObject comment : comments) {
            if (mPost.getParseUser("author") == ParseUser.getCurrentUser()
                    && !comment.getBoolean("readByAuthor")) {
                comment.put("readByAuthor", true);
                comment.saveEventually();
            }
        }
    }

    public void onClickSend(View view) {
        EditText messageField = (EditText) findViewById(R.id.message_field);
        String message = messageField.getText().toString().trim();

        if (message.isEmpty()) {
            messageField.setError("Enter your message");
            return;
        }

        ParseUser user = ParseUser.getCurrentUser();
        ParseObject comment = new ParseObject("Comment");

        comment.put("author", user);
        comment.put("message", message);
        comment.put("post", mPost);
        comment.put("readByAuthor", false);

        comment.saveInBackground(new SaveCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    // refresh page
                    Intent intent = getIntent();
                    finish();
                    startActivity(intent);
                } else {
                    Toast.makeText(CommentActivity.this, e.getMessage(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            super.onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
