package yuku.kbbi4;

import android.app.Application;
import android.content.Context;
import android.support.v4.content.LocalBroadcastManager;

public class App extends Application {
	public static Context context;

	public static LocalBroadcastManager lbm() {
		return LocalBroadcastManager.getInstance(context);
	}
}
