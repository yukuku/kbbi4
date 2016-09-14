package yuku.kbbi4.widget;

import android.text.Editable;
import android.text.TextWatcher;

public class DefaultTextWatcher implements TextWatcher {
	public interface AfterTextChangedListener {
		void afterTextChanged(final Editable s);
	}

	private final AfterTextChangedListener afterTextChangedListener;

	public DefaultTextWatcher() {
		afterTextChangedListener = null;
	}

	public DefaultTextWatcher(final AfterTextChangedListener afterTextChangedListener) {
		this.afterTextChangedListener = afterTextChangedListener;
	}

	@Override
	public void beforeTextChanged(final CharSequence s, final int start, final int count, final int after) {

	}

	@Override
	public void onTextChanged(final CharSequence s, final int start, final int before, final int count) {

	}

	@Override
	public void afterTextChanged(final Editable s) {
		if (afterTextChangedListener != null) {
			afterTextChangedListener.afterTextChanged(s);
		}
	}
}
