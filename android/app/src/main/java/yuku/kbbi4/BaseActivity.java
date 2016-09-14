package yuku.kbbi4;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import yuku.kbbi4.util.Views;

public abstract class BaseActivity extends AppCompatActivity {
	/**
	 * Automatic-casting version of {@link Activity#findViewById(int)}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends View> T find(@IdRes int id) {
		return Views.Find(this, id);
	}
}
