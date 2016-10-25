package yuku.kbbi4.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import yuku.kbbi4.R;

import static yuku.kbbi4.util.Views.Find;

public class DashboardPage extends ContentPage {
	static String[] facets = {
		"bahasa", "Bahasa", "Dari mana suatu kata berasal",
		"bidang", "Bidang", "Ranah kata bla bla bla",
		"kelas", "Kelas", "Kata sifat, benda, ganti, dan lain-lain",
		"ragam", "Ragam", "Arkais, cakapan, hormat, kasar, dan klasik",
		"jenis", "Jenis", "Apakah kata dasar atau tidak",
	};

	ViewGroup panelFacets;

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_dashboard, container, false);
		panelFacets = Find(res, R.id.panelFacets);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		for (int i = 0; i < facets.length; i+=3) {
			final String name = facets[i];
			final String title = facets[i+1];
			final String desc = facets[i+2];

			final View row = getActivity().getLayoutInflater().inflate(R.layout.dashboard_facet_row, panelFacets, false);
			final TextView tFacetTitle = Find(row, R.id.tFacetTitle);
			final TextView tFacetDesc = Find(row, R.id.tFacetDesc);
			tFacetTitle.setText(title);
			tFacetDesc.setText(desc);
			row.setOnClickListener(v -> {
				MainActivity.requestFacetPage(name);
			});

			panelFacets.addView(row);
		}
	}
}
