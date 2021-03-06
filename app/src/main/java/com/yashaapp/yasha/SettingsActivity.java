package com.yashaapp.yasha;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.parse.DeleteCallback;
import com.parse.FindCallback;
import com.parse.GetDataCallback;
import com.parse.LogInCallback;
import com.parse.LogOutCallback;
import com.parse.ParseException;
import com.parse.ParseFile;
import com.parse.ParseObject;
import com.parse.ParseQuery;
import com.parse.ParseUser;
import com.parse.SaveCallback;
import com.soundcloud.android.crop.Crop;
import com.squareup.picasso.Picasso;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_CAPTURE = 101;
    private static final int REQUEST_FROM_GALLERY = 102;
    String mCurrentPhotoPath;
    private Uri destination;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        getSupportActionBar().setElevation(0);

        TextView nameView = (TextView) findViewById(R.id.name_textview);
        TextView cityView = (TextView) findViewById(R.id.city_textview);
        final ImageView avatarView = (ImageView) findViewById(R.id.avatar_picker);

        ParseUser user = ParseUser.getCurrentUser();
        nameView.setText(user.getUsername());
        cityView.setText(user.getString("city").replace("No city", "Unable to retrieve city via GPS"));
        cityView.setTypeface(null, Typeface.ITALIC);

        ParseFile avatarFile = user.getParseFile("avatar");
        if (avatarFile != null) {
            avatarFile.getDataInBackground(new GetDataCallback() {
                @Override
                public void done(byte[] bytes, ParseException e) {
                    if (e == null) {
                        File tempFile = null;
                        try {
                            tempFile = File.createTempFile("abc", "cba", null);
                            FileOutputStream fos = new FileOutputStream(tempFile);
                            fos.write(bytes);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }

                        Picasso.with(SettingsActivity.this)
                                .load(tempFile)
                                .transform(new CircleTransform())
                                .into(avatarView);
                    }
                }
            });
        } else {
            Picasso.with(this)
                    .load(R.drawable.avatar_placeholder)
                    .transform(new CircleTransform())
                    .into(avatarView);
        }
    }

    public void onAvatarClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose image from...");
        builder.setSingleChoiceItems(
                new String[]{"Gallery", "Take photo"},
                -1,
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int position) {
                        switch (position) {
                            case 0:
                                dispatchGalleryPickerIntent();
                                dialog.cancel();
                                break;
                            case 1:
                                dispatchTakePictureIntent();
                                dialog.cancel();
                                break;
                        }
                    }
                });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void dispatchTakePictureIntent() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (intent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ignored) {
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                intent.putExtra(MediaStore.EXTRA_OUTPUT,
                        Uri.fromFile(photoFile));
                startActivityForResult(intent, REQUEST_IMAGE_CAPTURE);
            }
        } else {
            Toast.makeText(this, "Install camera app to take a picture", Toast.LENGTH_LONG).show();
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = "file:" + image.getAbsolutePath();
        return image;
    }

    private void dispatchGalleryPickerIntent() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, REQUEST_FROM_GALLERY);
        } else {
            Toast.makeText(this, "Install gallery app to choose an image", Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        final ImageView avatarView = (ImageView) findViewById(R.id.avatar_picker);

        if (resultCode == RESULT_OK) {

            if (requestCode == REQUEST_IMAGE_CAPTURE) {
                beginCrop(Uri.parse(mCurrentPhotoPath));
            } else if (requestCode == REQUEST_FROM_GALLERY) {
                beginCrop(data.getData());
            } else if (requestCode == Crop.REQUEST_CROP) {

                Picasso.with(this).invalidate(destination);
                Picasso.with(this)
                        .load(destination)
                        .fit().centerCrop()
                        .transform(new CircleTransform())
                        .into(avatarView);


                File imageFile = new File(destination.getPath());
                int size = (int) imageFile.length();
                byte[] bytes = new byte[size];
                try {
                    BufferedInputStream buf = new BufferedInputStream(new FileInputStream(imageFile));
                    buf.read(bytes, 0, bytes.length);
                    buf.close();

                    final ParseFile avatarFile = new ParseFile("avatar.jpg", bytes, "image/jpg");
                    avatarFile.saveInBackground(new SaveCallback() {
                        @Override
                        public void done(ParseException e) {
                            if (e == null) {
                                ParseUser user = ParseUser.getCurrentUser();
                                user.put("avatar", avatarFile);
                                user.saveInBackground();
                            }
                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void beginCrop(Uri uri) {
        destination = Uri.fromFile(new File(getCacheDir(), "cropped"));
        Crop.of(uri, destination).asSquare().start(this);
    }

    public void onInviteClick(View view) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT,
                "Hey check out the app 'Yasha' in the Google Play Store!\n"
                        + "https://play.google.com/store/apps/details?id="
                        + getApplicationContext().getPackageName());
        sendIntent.setType("text/plain");
        startActivity(Intent.createChooser(sendIntent, "Invite using..."));
    }

    public void onDeleteAccountPressed(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete your account? There's no undo.");

        final EditText passwordField = new EditText(this);
        passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordField.setHint("Current password");
        builder.setView(passwordField, convertToPixels(20), convertToPixels(12), convertToPixels(20), convertToPixels(4));

        builder.setPositiveButton("Delete", null);
        builder.setNegativeButton("Return", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = passwordField.getText().toString().trim();

                if (password.isEmpty()) {
                    passwordField.setError("Password is required");
                    passwordField.requestFocus();
                    return;
                }

                ParseUser.logInInBackground(ParseUser.getCurrentUser().getUsername(), password, new LogInCallback() {
                    @Override
                    public void done(final ParseUser user, ParseException e) {
                        if (e == null) {

                            ParseQuery<ParseObject> postsQuery = ParseQuery.getQuery("Post");
                            postsQuery.whereEqualTo("author", user);
                            postsQuery.findInBackground(new FindCallback<ParseObject>() {
                                @Override
                                public void done(List<ParseObject> posts, ParseException e) {
                                    if (e == null) {
                                        for (ParseObject post : posts) {
                                            post.deleteEventually();
                                        }

                                        ParseQuery<ParseObject> commentQuery = ParseQuery.getQuery("Comment");
                                        commentQuery.whereEqualTo("author", user);
                                        commentQuery.findInBackground(new FindCallback<ParseObject>() {
                                            @Override
                                            public void done(List<ParseObject> comments, ParseException e) {
                                                if (e == null) {
                                                    for (ParseObject comment : comments) {
                                                        comment.deleteEventually();
                                                    }

                                                    user.deleteInBackground(new DeleteCallback() {
                                                        @Override
                                                        public void done(ParseException e) {
                                                            if (e == null) {
                                                                ParseUser.logOutInBackground();

                                                                Toast.makeText(SettingsActivity.this, "Account was removed. Perhaps you'd like to create a new one?", Toast.LENGTH_SHORT).show();
                                                                Intent intent = new Intent(SettingsActivity.this, RegisterActivity.class);
                                                                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                                                                startActivity(intent);
                                                                finish();
                                                            } else {
                                                                Toast.makeText(SettingsActivity.this,
                                                                        "Failed to delete account: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                                            }
                                                        }
                                                    });
                                                }
                                            }
                                        });
                                    }
                                }
                            });

                        } else {
                            passwordField.setError("Password entered incorrectly");
                            passwordField.selectAll();
                            passwordField.requestFocus();
                        }
                    }
                });
            }
        });
    }

    public void onChangePasswordClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Change password:");

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText oldPasswordField = new EditText(this);
        oldPasswordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        oldPasswordField.setHint("Current password");

        final EditText newPasswordField = new EditText(this);
        newPasswordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        newPasswordField.setHint("New password");

        layout.addView(oldPasswordField);
        layout.addView(newPasswordField);

        builder.setView(layout, convertToPixels(20), convertToPixels(12), convertToPixels(20), convertToPixels(4));

        builder.setPositiveButton("Save", null);

        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPassword = oldPasswordField.getText().toString().trim();
                final String newPassword = newPasswordField.getText().toString().trim();

                boolean hasEmptyFields = false;

                if (newPassword.isEmpty()) {
                    newPasswordField.setError("Cannot be empty");
                    newPasswordField.requestFocus();
                    hasEmptyFields = true;
                }
                if (oldPassword.isEmpty()) {
                    oldPasswordField.setError("Cannot be empty");
                    oldPasswordField.requestFocus();
                    hasEmptyFields = true;
                }
                if (hasEmptyFields) return;


                ParseUser.logInInBackground(ParseUser.getCurrentUser().getUsername(), oldPassword, new LogInCallback() {
                    @Override
                    public void done(ParseUser user, ParseException e) {
                        if (e == null) {

                            user.setPassword(newPassword);
                            user.saveInBackground(new SaveCallback() {
                                @Override
                                public void done(ParseException e) {
                                    if (e == null) {
                                        ParseUser.logOutInBackground(new LogOutCallback() {
                                            @Override
                                            public void done(ParseException e) {
                                                if (e == null) {
                                                    dialog.cancel();
                                                    startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                                                    Toast.makeText(SettingsActivity.this, "Now you can login using your new password", Toast.LENGTH_LONG).show();
                                                }
                                            }
                                        });
                                    } else {
                                        Toast.makeText(SettingsActivity.this,
                                                e.getMessage(), Toast.LENGTH_LONG).show();
                                    }
                                }
                            });

                        } else {
                            oldPasswordField.setError("Old password entered incorrectly");
                            oldPasswordField.selectAll();
                            oldPasswordField.requestFocus();
                        }
                    }
                });
            }
        });
    }

    private int convertToPixels(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return (int) (dp * density);
    }

    public void onChangeUsernameClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Change “" + ParseUser.getCurrentUser().getUsername() + "” to:");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText usernameField = new EditText(this);
        usernameField.setInputType(InputType.TYPE_TEXT_VARIATION_PERSON_NAME | InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        usernameField.setHint("New username");

        final EditText passwordField = new EditText(this);
        passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordField.setHint("Current password");

        layout.addView(usernameField);
        layout.addView(passwordField);

        builder.setView(layout, convertToPixels(20), convertToPixels(12), convertToPixels(20), convertToPixels(4));

        builder.setPositiveButton("Save", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String username = usernameField.getText().toString().trim();
                String password = passwordField.getText().toString().trim();

                boolean hasEmptyFields = false;

                if (password.isEmpty()) {
                    passwordField.setError("Cannot be empty");
                    passwordField.requestFocus();
                    hasEmptyFields = true;
                }
                if (username.isEmpty()) {
                    usernameField.setError("Cannot be empty");
                    usernameField.requestFocus();
                    hasEmptyFields = true;
                }
                if (hasEmptyFields) return;


                ParseUser.logInInBackground(ParseUser.getCurrentUser().getUsername(),
                        password, new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if (e == null) {
                                    user.setUsername(username);
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                ParseUser.logOutInBackground(new LogOutCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        if (e == null) {
                                                            dialog.cancel();
                                                            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                                                            Toast.makeText(SettingsActivity.this, "Now you can login using your new username", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                usernameField.setError("Username already taken");
                                                usernameField.selectAll();
                                                usernameField.requestFocus();
                                            }
                                        }
                                    });
                                } else {
                                    passwordField.setError("Password entered incorrectly");
                                    passwordField.selectAll();
                                    passwordField.requestFocus();
                                }
                            }
                        });
            }
        });
    }

    public void onChangeEmailClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Change “" + ParseUser.getCurrentUser().getEmail() + "” to:");


        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        final EditText emailField = new EditText(this);
        emailField.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailField.setHint("New email");

        final EditText passwordField = new EditText(this);
        passwordField.setTransformationMethod(PasswordTransformationMethod.getInstance());
        passwordField.setHint("Current password");

        layout.addView(emailField);
        layout.addView(passwordField);

        builder.setView(layout, convertToPixels(20), convertToPixels(12), convertToPixels(20), convertToPixels(4));

        builder.setPositiveButton("Save", null);

        final AlertDialog dialog = builder.create();
        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final String email = emailField.getText().toString();
                String password = passwordField.getText().toString();

                boolean hasEmptyFields = false;

                if (password.isEmpty()) {
                    passwordField.setError("Cannot be empty");
                    passwordField.requestFocus();
                    hasEmptyFields = true;
                }
                if (email.isEmpty()) {
                    emailField.setError("Cannot be empty");
                    emailField.requestFocus();
                    hasEmptyFields = true;
                }
                if (hasEmptyFields) return;


                ParseUser.logInInBackground(ParseUser.getCurrentUser().getUsername(),
                        password, new LogInCallback() {
                            @Override
                            public void done(ParseUser user, ParseException e) {
                                if (e == null) {
                                    user.setEmail(email);
                                    user.saveInBackground(new SaveCallback() {
                                        @Override
                                        public void done(ParseException e) {
                                            if (e == null) {
                                                ParseUser.logOutInBackground(new LogOutCallback() {
                                                    @Override
                                                    public void done(ParseException e) {
                                                        if (e == null) {
                                                            dialog.cancel();
                                                            startActivity(new Intent(SettingsActivity.this, LoginActivity.class));
                                                            Toast.makeText(SettingsActivity.this, "Now you can login using your new email", Toast.LENGTH_LONG).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                emailField.setError("Invalid email address");
                                                emailField.selectAll();
                                                emailField.requestFocus();
                                            }
                                        }
                                    });
                                } else {
                                    passwordField.setError("Password entered incorrectly");
                                    passwordField.selectAll();
                                    passwordField.requestFocus();
                                }
                            }
                        });

            }
        });
    }

    public void onLogoutClick(View view) {
        ParseUser.logOutInBackground(new LogOutCallback() {
            @Override
            public void done(ParseException e) {
                if (e == null) {
                    finish();

                    Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return true;
        }
        return false;
    }
}
