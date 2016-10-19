package yuku.kbbi4.widget;

import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import yuku.kbbi4.BaseActivity;
import yuku.kbbi4.R;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static yuku.kbbi4.util.Views.Find;
import static yuku.kbbi4.util.Views.gonify;

public class SearchWrapper {
	public abstract static class OnQueryTextListener {
		public void onQueryTextSubmit(@NonNull String query) {
		}

		public void onQueryTextChange(@NonNull String query) {
		}
	}

	public final View root;
	public final TextView tSearchText;
	public final ImageButton bSearchClear;

	private OnQueryTextListener mOnQueryChangeListener;
	private CharSequence mOldQueryText;

	public SearchWrapper(final View view) {
		root = view;
		tSearchText = Find(view, R.id.tSearchText);
		bSearchClear = Find(view, R.id.bSearchClear);

		gonify(bSearchClear);

		tSearchText.setOnEditorActionListener((v, actionId, event) -> {
			if (actionId == EditorInfo.IME_ACTION_SEARCH || event.getKeyCode() == KeyEvent.KEYCODE_ENTER) {
				if (mOnQueryChangeListener != null) {
					mOnQueryChangeListener.onQueryTextSubmit(tSearchText.getText().toString());
				}
				return true;
			}
			return false;
		});

		tSearchText.addTextChangedListener(new DefaultTextWatcher() {
			public void onTextChanged(CharSequence newText, int start, int before, int after) {
				bSearchClear.setVisibility(!TextUtils.isEmpty(tSearchText.getText()) ? VISIBLE : GONE);
				if (mOnQueryChangeListener != null && !(TextUtils.isEmpty(newText) && TextUtils.isEmpty(mOldQueryText)) && !TextUtils.equals(newText, mOldQueryText)) {
					mOnQueryChangeListener.onQueryTextChange(newText.toString());
				}
				mOldQueryText = newText.toString();
			}
		});

		bSearchClear.setOnClickListener(v -> {
			tSearchText.setText("");
			if (!tSearchText.isFocused()) {
				tSearchText.requestFocus();
				BaseActivity.showKeyboard(tSearchText);
			}
		});
	}

	public void setOnQueryTextListener(OnQueryTextListener listener) {
		mOnQueryChangeListener = listener;
	}
}
