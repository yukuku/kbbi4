package yuku.kbbi4;

import android.os.Bundle;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import yuku.kbbi4.dictdata.Acu;
import yuku.kbbi4.dictdata.Renderer;
import yuku.kbbi4.util.Debouncer;
import yuku.kbbi4.widget.DefaultTextWatcher;

import java.util.ArrayList;
import java.util.List;

import static yuku.kbbi4.util.Views.Find;

public class MainActivity extends BaseActivity {

	TextView tCarian;
	RecyclerView lsAcus;

	AcusAdapter adapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		lsAcus = find(R.id.lsAcus);
		lsAcus.setLayoutManager(new LinearLayoutManager(this));
		lsAcus.setAdapter(adapter = new AcusAdapter());

		tCarian = find(R.id.tCarian);
		tCarian.addTextChangedListener(new DefaultTextWatcher(s -> debouncer.submit(s.toString().trim())));

		Acu.INSTANCE.noop();
	}

	final Debouncer<String, List<String>> debouncer = new Debouncer<String, List<String>>(200) {
		@Override
		public List<String> process(final String payload) {
			return Acu.INSTANCE.listAcus(payload);
		}

		@Override
		public void onResult(final List<String> result) {
			adapter.setData(result);
			lsAcus.scrollToPosition(0);
		}
	};

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
			return new ViewHolder(getLayoutInflater().inflate(android.R.layout.simple_list_item_1, parent, false));
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
				final Renderer renderer = Acu.INSTANCE.getRenderer(acu);

				new AlertDialog.Builder(MainActivity.this)
					.setMessage(renderer.render())
					.setPositiveButton("OK", null)
					.show();
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
