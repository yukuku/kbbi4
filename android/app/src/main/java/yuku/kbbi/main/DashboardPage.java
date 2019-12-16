package yuku.kbbi.main;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import yuku.kbbi.R;
import static yuku.kbbi.util.Views.Find;

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
		panelFacets = Find(res, R.id.panelFacets);
		bSearch = Find(res, R.id.bSearch);
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
			final View cell = Find(row, idx % 2 == 0 ? R.id.cell0 : R.id.cell1);

			final TextView tFacetTitle = Find(cell, R.id.tFacetTitle);
			final TextView tFacetDesc = Find(cell, R.id.tFacetDesc);
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
