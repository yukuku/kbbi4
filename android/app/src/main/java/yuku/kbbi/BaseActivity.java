package yuku.kbbi;

import android.app.Activity;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import yuku.kbbi.util.Views;

public abstract class BaseActivity extends AppCompatActivity {
	/**
	 * Automatic-casting version of {@link Activity#findViewById(int)}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends View> T find(@IdRes int id) {
		return Views.Find(this, id);
	}

	public static void hideKeyboard(View view) {
		final InputMethodManager imm = (InputMethodManager) App.context.getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
		imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	public static void showKeyboard(View view) {
		final InputMethodManager imm = (InputMethodManager) App.context.getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, 0);
	}
}
