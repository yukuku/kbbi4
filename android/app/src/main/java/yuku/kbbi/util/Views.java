package yuku.kbbi.util;

import android.view.View;
import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;
import androidx.annotation.NonNull;

public class Views {

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
