package com.example.id.delovnapraksa;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.facebook.AccessToken;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.HttpMethod;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * This {@code IntentService} does the app's actual work.
 * {@code SampleAlarmReceiver} (a {@code WakefulBroadcastReceiver}) holds a
 * partial wake lock for this service while the service does its work. When the
 * service is finished, it calls {@code completeWakefulIntent()} to release the
 * wake lock.
 */
public class SampleSchedulingService extends IntentService {
    public SampleSchedulingService() {
        super("SchedulingService");
    }
    
    // An ID used to post the notification.
    public static final int NOTIFICATION_ID = 1;

    private NotificationManager mNotificationManager;

    // Cache
    SharedPreferences settings;
    SharedPreferences.Editor editor;

    int numNewTags = 0;
    Intent currentIntent;

    final String username = "MAIL HERE";
    final String password = "PASSWORD HERE";

    @Override
    protected void onHandleIntent(Intent intent) {
        // BEGIN_INCLUDE(service_onhandle)
        // The URL from which to fetch content.
        settings = getSharedPreferences("MySettings", Context.MODE_PRIVATE);
        editor = settings.edit();

        currentIntent = intent;
        
        // Try to connect to the Google homepage and download content.
        loadFromNetwork();

        // END_INCLUDE(service_onhandle)
    }
    
    // Post a notification indicating whether a doodle was found.
    private void sendNotification(String msg) {
        mNotificationManager = (NotificationManager)
               this.getSystemService(Context.NOTIFICATION_SERVICE);
    
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
            new Intent(this, MainActivity.class), 0);

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle("You have " + numNewTags + " new tags")
        .setStyle(new NotificationCompat.BigTextStyle()
        .bigText(msg))
        .setContentText(msg);

        mBuilder.setContentIntent(contentIntent);
        mNotificationManager.notify(NOTIFICATION_ID, mBuilder.build());
    }
 
//
// The methods below this line fetch content from the specified URL and return the
// content as a string.
//
    /** Given a URL string, initiate a fetch operation. */
    private void loadFromNetwork() {
        Log.d("SERVICE", "loadFromNetwork");
        Handler mHandler = new Handler(getMainLooper());

        mHandler.post(new Runnable() {
                          @Override
                          public void run() {
        new GraphRequest(
                AccessToken.getCurrentAccessToken(),
                "/me/tagged",
                null,
                HttpMethod.GET,
                new GraphRequest.Callback() {
                    public void onCompleted(GraphResponse response) {
                            /* handle the result */
                        Log.d("graph response SERVICE", response.toString());
                        JSONObject json = response.getJSONObject();
                        try {
                            JSONArray data = json.getJSONArray("data");

                            numNewTags = 0;
                            for (int i = 0; i < data.length(); i++) {
                                Log.d("graph response SERVICE", String.valueOf(i));
                                String message = data.getJSONObject(i).getString("message");
                                String tagged_time = data.getJSONObject(i).getString("tagged_time");
                                String id = data.getJSONObject(i).getString("id");
                                Log.d("graph response id", String.valueOf(id));

                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
                                Date convertedDate = new Date();
                                try {
                                    convertedDate = dateFormat.parse(tagged_time);
                                    Date lastTagDate = new Date();
                                    lastTagDate.setTime(settings.getLong("last_tag_time", 0));

                                    if (convertedDate.after(lastTagDate)) {
                                        numNewTags++;
                                        Log.d("ID OF LAST TAGGED POST", String.valueOf(id));
                                        editor.putLong("last_tag_time", convertedDate.getTime());
                                        editor.apply();
                                        Log.d("SERVICE", "there is a new tag!");
                                    }
                                } catch (ParseException e) {
                                    // TODO Auto-generated catch block
                                    e.printStackTrace();
                                }
                            }

                            if (numNewTags > 0) {
                                sendNotification("Check your e-mail!");
                                SendMail sendMail = new SendMail();
                                sendMail.execute();
                            } else {
                                MailAlarmReceiver.completeWakefulIntent(currentIntent);
                            }

                            // Release the wake lock provided by the BroadcastReceiver.
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }
        ).executeAsync();
                          }
        });
    }

    private class SendMail extends AsyncTask<Void, String, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {

            Properties props = new Properties();
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.starttls.enable", "true");
            props.put("mail.smtp.host", "smtp.gmail.com");
            props.put("mail.smtp.port", "587");

            Session session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(username, password);
                        }
                    });
            try {
                // message to send to the email
                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress("SENDER MAIL HERE"));
                message.setRecipients(Message.RecipientType.TO,
                        InternetAddress.parse("RECIPIENT MAIL HERE"));
                message.setSubject("You have new tags!");
                message.setText("You have " + numNewTags + " new tags on your Facebook profile!");
                Transport.send(message);

                return true;
            } catch (MessagingException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
