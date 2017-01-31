import android.app.Application;
import com.facebook.FacebookSdk;
import com.facebook.appevents.AppEventsLogger;


/**
 * Created by ID on 1/24/2017.
 */

public class MyApplication extends Application {

    public void onCreate() {
        super.onCreate();
        FacebookSdk.sdkInitialize(getApplicationContext());
        AppEventsLogger.activateApp(this);

    }

}
