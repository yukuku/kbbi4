package yuku.kbbi.main;

import android.app.Activity;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.Nullable;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import yuku.kbbi.R;

public class DashboardPage extends ContentPage {
	static String[] facets = {
		"kelas", "Kelas kata", "nomina, verba, …",
		"ragam", "Ragam", "hormat, cakapan, …",
		"bahasa", "Bahasa", "Jawa, Inggris, …",
		"bidang", "Bidang", "Komputer, Olahraga, …",
	};

	ViewGroup panelFacets;
	FloatingActionButton bSearch;

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_dashboard, container, false);
		panelFacets = res.findViewById(R.id.panelFacets);
		bSearch = res.findViewById(R.id.bSearch);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		View row = null;
		for (int i = 0; i < facets.length; i += 3) {
			final String name = facets[i];
			final String title = facets[i + 1];
			final String desc = facets[i + 2];

			final int idx = i / 3;

			if (idx % 2 == 0) {
				row = getActivity().getLayoutInflater().inflate(R.layout.dashboard_facet_row, panelFacets, false);
				panelFacets.addView(row);
			}

			assert row != null;
			int id = idx % 2 == 0 ? R.id.cell0 : R.id.cell1;
			final View cell = row.findViewById(id);

			final TextView tFacetTitle = cell.findViewById(R.id.tFacetTitle);
			final TextView tFacetDesc = cell.findViewById(R.id.tFacetDesc);
			tFacetTitle.setText(title);
			tFacetDesc.setText(desc);

			cell.setOnClickListener(v -> MainActivity.requestFacetPage(name, title));
		}

		bSearch.setOnClickListener(v -> {
			final Activity activity = getActivity();
			if (activity != null) {
				final View menuSearch = activity.findViewById(R.id.menuSearch);
				if (menuSearch != null) {
					menuSearch.performClick();
				}
			}
		});
	}
}
