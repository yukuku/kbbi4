package yuku.kbbi;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import androidx.annotation.IdRes;
import androidx.appcompat.app.AppCompatActivity;
import yuku.kbbi.util.Views;

public abstract class BaseActivity extends AppCompatActivity {
	public static void hideKeyboard(View view) {
		final InputMethodManager imm = (InputMethodManager) App.context.getSystemService(INPUT_METHOD_SERVICE);
		imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_IMPLICIT_ONLY);
		imm.hideSoftInputFromWindow(view.getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);
	}

	public static void showKeyboard(View view) {
		final InputMethodManager imm = (InputMethodManager) App.context.getSystemService(INPUT_METHOD_SERVICE);
		imm.showSoftInput(view, 0);
	}

	/**
	 * Automatic-casting version of {@link Activity#findViewById(int)}.
	 */
	@SuppressWarnings("unchecked")
	public <T extends View> T find(@IdRes int id) {
		return Views.Find(this, id);
	}
}
