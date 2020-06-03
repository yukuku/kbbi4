package yuku.kbbi;

import android.app.Application;
import android.content.Context;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

public class App extends Application {
	public static Context context;

	public static LocalBroadcastManager lbm() {
		return LocalBroadcastManager.getInstance(context);
	}
}
