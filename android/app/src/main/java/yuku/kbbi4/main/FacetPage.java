package yuku.kbbi4.main;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.f2prateek.dart.Dart;
import com.f2prateek.dart.InjectExtra;
import yuku.kbbi4.R;
import yuku.kbbi4.dictdata.Kategori;
import yuku.kbbi4.dictdata.KategoriRepo;

import java.util.ArrayList;
import java.util.List;

import static yuku.kbbi4.util.Views.Find;

public class FacetPage extends ContentPage {
	@InjectExtra
	String facet;

	TextView tFacet;
	RecyclerView lsKategoris;

	KategorisAdapter kategorisAdapter;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Dart.inject(this, getArguments());
	}

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_jenis, container, false);
		tFacet = Find(res, R.id.tFacet);
		lsKategoris = Find(res, R.id.lsKategoris);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		tFacet.setText(facet);
		lsKategoris.setLayoutManager(new LinearLayoutManager(getActivity()));
		lsKategoris.setAdapter(kategorisAdapter = new KategorisAdapter());
		kategorisAdapter.setData(KategoriRepo.INSTANCE.listKategoris(facet));
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final TextView text1;

		public ViewHolder(final View itemView) {
			super(itemView);
			text1 = Find(itemView, android.R.id.text1);
		}
	}

	class KategorisAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		final List<Kategori> kategoris = new ArrayList<>();

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
			return new ViewHolder(getActivity().getLayoutInflater().inflate(R.layout.item_acu, parent, false));
		}

		@Override
		public void onBindViewHolder(final RecyclerView.ViewHolder _holder_, final int position) {
			final ViewHolder holder = (ViewHolder) _holder_;
			{
				final Kategori kategori = kategoris.get(position);
				holder.text1.setText(kategori.desc);
			}

			holder.itemView.setOnClickListener(v -> {
				final Kategori kategori = kategoris.get(holder.getAdapterPosition());
				MainActivity.requestKategoriPage(facet, kategori.nilai);
			});
		}

		@Override
		public int getItemCount() {
			return kategoris.size();
		}

		public void setData(final List<Kategori> result) {
			this.kategoris.clear();
			this.kategoris.addAll(result);
			notifyDataSetChanged();
		}
	}

}
