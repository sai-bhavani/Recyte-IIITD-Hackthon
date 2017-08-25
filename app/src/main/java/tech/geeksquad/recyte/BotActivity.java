package tech.geeksquad.recyte;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import ai.api.AIListener;
import ai.api.AIServiceException;
import ai.api.android.AIConfiguration;
import ai.api.android.AIDataService;
import ai.api.android.AIService;
import ai.api.model.AIError;
import ai.api.model.AIRequest;
import ai.api.model.AIResponse;

public class BotActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_IMAGE = 1;
    private AIConfiguration config;
    private AIListener aiListener;
    private AIService aiService;
    private FirebaseDatabase database;
    private DatabaseReference reference;
    private ArrayList<Message> messageArrayList;
    private ChatAdapter adapter;
    private String TAG = "bot_activity";
    private AIDataService aiDataService;
    private ListView messageListView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_bot);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

        database = FirebaseDatabase.getInstance();
        reference = database.getReference("messages").child(user.getUid());

        messageListView = (ListView) findViewById(R.id.message_list);

        init();

        messageArrayList = new ArrayList<>();
        adapter = new ChatAdapter(this, messageArrayList);
        messageListView.setAdapter(adapter);

        reference.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                Message message = dataSnapshot.getValue(Message.class);
                messageArrayList.add(message);
                adapter.notifyDataSetChanged();
                messageListView.smoothScrollToPosition(messageArrayList.size());
            }

            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {

            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {

            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }

    private void init() {

        config = new AIConfiguration("20436a41d63641b685aa22dafc43cdcb",
                AIConfiguration.SupportedLanguages.English,
                AIConfiguration.RecognitionEngine.System);

        aiDataService = new AIDataService(this, config);

        aiService = AIService.getService(this, config);

        aiListener = new AIListener() {
            @Override
            public void onResult(AIResponse result) {
                Log.d(TAG, "onResult: " + result);
            }

            @Override
            public void onError(AIError error) {
                Log.d(TAG, "onResult: " + error);

            }

            @Override
            public void onAudioLevel(float level) {

            }

            @Override
            public void onListeningStarted() {

            }

            @Override
            public void onListeningCanceled() {

            }

            @Override
            public void onListeningFinished() {

            }
        };

        aiService.setListener(aiListener);
    }

    public void sendMessage(View view) {
        EditText messageEditText = (EditText) findViewById(R.id.message);
        String messageString = messageEditText.getText().toString();
        Message message = new Message("user", messageString);
        reference.child(String.valueOf(System.currentTimeMillis())).setValue(message);
        new messageSendAsyncTask().execute(messageString);
        messageEditText.setText("");
    }

    public void sendPicture(View view) {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, REQUEST_CODE_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == RESULT_OK &&
                data != null && data.getData() != null) {
            Uri uri = data.getData();
            try {
                ImageView imageView = new ImageView(this);
                final Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), uri);
                imageView.setImageBitmap(bitmap);
                LinearLayout linearLayout = new LinearLayout(this);
                linearLayout.setGravity(Gravity.CENTER);
                LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(400, 400);
                imageView.setLayoutParams(layoutParams);
                linearLayout.addView(imageView);

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(getString(R.string.upload_image))
                        .setMessage(getString(R.string.upload_confirm_prompt))
                        .setView(linearLayout)
                        .setPositiveButton(getString(R.string.upload), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                uploadImage(bitmap);
                            }
                        }).setNegativeButton(getString(R.string.cancel), null)
                        .setCancelable(false);
                builder.show();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadImage(Bitmap bitmap) {
        int byteCount = bitmap.getByteCount() / 1024;

        int quality = 100;
        if (byteCount > 500) {
            quality = 100 * 500 / byteCount;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos);
        byte[] data = baos.toByteArray();

        Message message = new Message("user", data);
    }

    private class messageSendAsyncTask extends AsyncTask<String, Object, AIResponse> {
        @Override
        protected AIResponse doInBackground(String... params) {
            try {
                Log.d(TAG, "doInBackground: before call");
                AIResponse hello = aiDataService.request(new AIRequest(params[0]));
                Log.d(TAG, "doInBackground: after call");

                Log.d(TAG, "doInBackground: " + hello.getResult().getFulfillment().getSpeech());
                Message message = new Message("bot", hello.getResult().getFulfillment().getSpeech().replaceAll("/n", "\n"));
                reference.child(String.valueOf(System.currentTimeMillis())).setValue(message);
                return hello;
            } catch (AIServiceException e) {
                Log.e(TAG, "doInBackground: ", e);
                e.printStackTrace();
            }
            return null;
        }
    }

}
