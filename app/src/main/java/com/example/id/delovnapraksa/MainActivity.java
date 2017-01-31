package com.example.id.delovnapraksa;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.support.v4.app.FragmentActivity;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.login.widget.LoginButton;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

public class MainActivity extends FragmentActivity {

    LoginButton loginButton;
    CallbackManager callbackManager;
    ArrayList<String> values = new ArrayList<>();
    ArrayAdapter<String> adapter;
    private ListView listView;

    @Override
    protected void onCreate(Bundle savedInstaces) {
        super.onCreate(savedInstaces);
        FacebookSdk.sdkInitialize(getApplicationContext());
        setContentView(R.layout.activity_main);
        loginButton = (LoginButton) findViewById(R.id.login_button);
        callbackManager = CallbackManager.Factory.create();

        loginButton.setReadPermissions("public_profile, user_posts");
        final TextView txtMentions = (TextView) findViewById(R.id.txtMentions);

        loginButton.registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d("login token", loginResult.getAccessToken().toString());
                getInfoFromFacebookLogin();

                loginButton.setVisibility(View.GONE);
                txtMentions.setVisibility(View.VISIBLE);
                listView.setVisibility(View.VISIBLE);

                MailAlarmReceiver alarm = new MailAlarmReceiver();
                alarm.setAlarm(getApplicationContext());
            }

            @Override
            public void onCancel() {

            }

            @Override
            public void onError(FacebookException error) {

            }
        });

        listView = (ListView) findViewById(R.id.listView);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, values);
        listView.setAdapter(adapter);

        if (AccessToken.getCurrentAccessToken() != null && !AccessToken.getCurrentAccessToken().isExpired()) {
            MailAlarmReceiver alarm = new MailAlarmReceiver();
            alarm.setAlarm(this);

            // user is logged in, hide login button
            loginButton.setVisibility(View.GONE);
            txtMentions.setVisibility(View.VISIBLE);
            listView.setVisibility(View.VISIBLE);

            // get info from facebook
            getInfoFromFacebookLogin();
        } else {
            loginButton.setVisibility(View.VISIBLE);
            listView.setVisibility(View.GONE);
            txtMentions.setVisibility(View.GONE);
        }


    }

    private void getInfoFromFacebookLogin() {
        // request pull of user mentions, likes, etc...
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/tagged",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                            /* handle the result */
                        Log.d("graph response", response.toString());
                        JSONObject json = response.getJSONObject();
                        values.clear();
                        try {
                            JSONArray data = json.getJSONArray("data");

                            for (int i = 0; i < data.length(); i++) {
                                String message = data.getJSONObject(i).getString("message");
                                String tagged_time = data.getJSONObject(i).getString("tagged_time");
                                String id = data.getJSONObject(i).getString("id");

                                values.add(String.valueOf(i + 1) + ") Mention message: " + message);
                                values.add("Mention time: " + tagged_time);
                            }

                            adapter.notifyDataSetChanged();
                            listView.setAdapter(adapter);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode,resultCode,data);
    }
}