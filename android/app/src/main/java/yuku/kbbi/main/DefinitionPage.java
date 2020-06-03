package yuku.kbbi.main;

import android.os.Bundle;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import kotlin.Unit;
import yuku.kbbi.R;
import yuku.kbbi.dictdata.Acu;
import yuku.kbbi.dictdata.Renderer;

public class DefinitionPage extends ContentPage {
	int acu_id;

	TextView tDesc;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Bundle bundle = getArguments();
		acu_id = bundle.getInt("acu_id");
	}

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_definition, container, false);
		tDesc = res.findViewById(R.id.tDesc);
		return res;
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		final Renderer renderer = Acu.INSTANCE.getRenderer(acu_id, link_acu_id -> {
			MainActivity.requestDefinitionPage(link_acu_id);
			return Unit.INSTANCE;
		}, (facet, nilai) -> {
			MainActivity.requestKategoriPage(facet, nilai);
			return Unit.INSTANCE;
		});

		tDesc.setText(renderer.render());
		tDesc.setMovementMethod(LinkMovementMethod.getInstance());
	}
}
