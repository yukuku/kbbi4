package yuku.kbbi.main;

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
import yuku.kbbi.R;
import yuku.kbbi.dictdata.Acu;
import yuku.kbbi.dictdata.Kategori;
import yuku.kbbi.dictdata.KategoriRepo;

import java.util.ArrayList;
import java.util.List;

import static yuku.kbbi.main.MainActivity.requestDefinitionPage;
import static yuku.kbbi.util.Views.Find;

public class KategoriPage extends ContentPage {
	@InjectExtra
	String facet;

	@InjectExtra
	String nilai;

	TextView tKategori;
	RecyclerView lsAcus;

	Kategori kategori;
	AcusAdapter acusAdapter;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Dart.inject(this, getArguments());
	}

	@Nullable
	@Override
	public View onCreateView(final LayoutInflater inflater, @Nullable final ViewGroup container, @Nullable final Bundle savedInstanceState) {
		final View res = inflater.inflate(R.layout.page_main_kategori, container, false);
		tKategori = Find(res, R.id.tKategori);
		lsAcus = Find(res, R.id.lsAcus);
		return res;
	}

	@Override
	public void onViewCreated(final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		kategori = KategoriRepo.INSTANCE.getKategori(facet, nilai);

		tKategori.setText(kategori.desc + (kategori.nilai.equals(kategori.desc) ? "" : (" (" + kategori.nilai + ")")));
		lsAcus.setLayoutManager(new LinearLayoutManager(getActivity()));
		lsAcus.setAdapter(acusAdapter = new AcusAdapter());

		final List<String> acus = new ArrayList<>();
		for (final int acuId : KategoriRepo.INSTANCE.listAcuIds(facet, nilai)) {
			acus.add(Acu.INSTANCE.getAcu(acuId));
		}
		acusAdapter.setData(acus);
	}

	static class ViewHolder extends RecyclerView.ViewHolder {
		final TextView text1;

		public ViewHolder(final View itemView) {
			super(itemView);
			text1 = Find(itemView, android.R.id.text1);
		}
	}

	class AcusAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
		final List<String> acus = new ArrayList<>();

		AcusAdapter() {
			setHasStableIds(true);
		}

		@Override
		public RecyclerView.ViewHolder onCreateViewHolder(final ViewGroup parent, final int viewType) {
			return new ViewHolder(getActivity().getLayoutInflater().inflate(R.layout.item_acu, parent, false));
		}

		@Override
		public void onBindViewHolder(final RecyclerView.ViewHolder _holder_, final int position) {
			final ViewHolder holder = (ViewHolder) _holder_;
			{
				final String acu = acus.get(position);
				holder.text1.setText(acu);
			}

			holder.itemView.setOnClickListener(v -> {
				final String acu = acus.get(holder.getAdapterPosition());
				requestDefinitionPage(Acu.INSTANCE.getId(acu));
			});
		}

		@Override
		public int getItemCount() {
			return acus.size();
		}

		public void setData(final List<String> result) {
			this.acus.clear();
			this.acus.addAll(result);
			notifyDataSetChanged();
		}

		@Override
		public long getItemId(final int position) {
			return Acu.INSTANCE.getId(acus.get(position));
		}
	}
}
