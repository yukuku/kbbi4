package yuku.kbbi.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import yuku.kbbi.R;
import yuku.kbbi.dictdata.Kategori;
import yuku.kbbi.dictdata.KategoriRepo;

public class FacetPage extends ContentPage {
	String facet;
	String judul;

	TextView tFacet;
	RecyclerView lsKategoris;

	KategorisAdapter kategorisAdapter;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Bundle bundle = getArguments();
		facet = bundle.getString("facet");
		judul = bundle.getString("judul");
	}

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_facet, container, false);
		tFacet = res.findViewById(R.id.tFacet);
		lsKategoris = res.findViewById(R.id.lsKategoris);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		tFacet.setText(judul);
		lsKategoris.setLayoutManager(new LinearLayoutManager(getActivity()));
		lsKategoris.setAdapter(kategorisAdapter = new KategorisAdapter());
		kategorisAdapter.setData(KategoriRepo.INSTANCE.listKategoris(facet));
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final TextView text1;

		ViewHolder(final View itemView) {
			super(itemView);
			text1 = itemView.findViewById(android.R.id.text1);
		}
	}

	class KategorisAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		final List<Kategori> kategoris = new ArrayList<>();

		@NonNull
		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(@NonNull final ViewGroup parent, final int viewType) {
			return new ViewHolder(getActivity().getLayoutInflater().inflate(R.layout.item_acu, parent, false));
		}

		@Override
		public void onBindViewHolder(@NonNull final RecyclerView.ViewHolder _holder_, final int position) {
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
