package yuku.kbbi4.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import yuku.kbbi4.R;
import yuku.kbbi4.dictdata.Acu;
import yuku.kbbi4.dictdata.Renderer;

import static yuku.kbbi4.util.Views.Find;

public class DefinitionPage extends ContentPage {
	@InjectExtra
	int acu_id;

	TextView tDesc;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Dart.inject(this, getArguments());
	}

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_definition, container, false);
		tDesc = Find(res, R.id.tDesc);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		final Renderer renderer = Acu.INSTANCE.getRenderer(acu_id);
		tDesc.setText(renderer.render());
	}
}
