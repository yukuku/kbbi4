package yuku.kbbi.util;

import android.app.Activity;
import android.view.View;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;

public class Views {
	/**
	 * Automatic-casting version of {@link View#findViewById(int)}.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends View> T Find(@NonNull View parent, @IdRes int id) {
		return (T) parent.findViewById(id);
	}

	/**
	 * Automatic-casting version of {@link Activity#findViewById(int)}.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends View> T Find(@NonNull Activity activity, @IdRes int id) {
		return (T) activity.findViewById(id);
	}

	public static void gonify(View... views) {
		changeVisibility(GONE, views);
	}

	public static void invisiblify(View... views) {
		changeVisibility(INVISIBLE, views);
	}

	public static void visiblify(View... views) {
		changeVisibility(VISIBLE, views);
	}

	public static void changeVisibility(int visibility, View... views) {
		for (final View view : views) {
			if (view != null) {
				view.setVisibility(visibility);
			}
		}
	}

	public static void makeVisible(boolean visible, View... views) {
		changeVisibility(visible ? VISIBLE : GONE, views);
	}

	public static boolean isVisible(@NonNull View view) {
		return view.getVisibility() == VISIBLE;
	}
}
