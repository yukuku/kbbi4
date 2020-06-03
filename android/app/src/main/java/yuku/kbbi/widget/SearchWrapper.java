package yuku.kbbi.widget;

import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import android.view.inputmethod.EditorInfo;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import yuku.kbbi.BaseActivity;
import yuku.kbbi.R;
import static yuku.kbbi.util.Views.gonify;

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

	OnQueryTextListener mOnQueryChangeListener;
	CharSequence mOldQueryText;

	public SearchWrapper(final View view) {
		root = view;
		tSearchText = view.findViewById(R.id.tSearchText);
		bSearchClear = view.findViewById(R.id.bSearchClear);

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
