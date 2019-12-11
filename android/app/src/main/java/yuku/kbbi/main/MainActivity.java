package yuku.kbbi.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.text.util.LinkifyCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.SimplePageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;
import kotlin.io.TextStreamsKt;
import yuku.kbbi.App;
import yuku.kbbi.BaseActivity;
import yuku.kbbi.BuildConfig;
import yuku.kbbi.R;
import yuku.kbbi.dictdata.Acu;
import yuku.kbbi.util.Background;
import yuku.kbbi.util.Debouncer;
import yuku.kbbi.util.Exceptions;
import yuku.kbbi.widget.SearchWrapper;

import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static yuku.kbbi.util.Views.*;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

	private static final String ACTION_REQUEST_NEW_PAGE = MainActivity.class.getName() + ".ACTION_REQUEST_NEW_PAGE";

	DrawerLayout drawer;
	ActionBar ab;
	SearchWrapper searchWrapper;
	ViewPager vp;
	RecyclerView lsAcus;

	AcusAdapter acusAdapter;
	ContentAdapter pagerAdapter;
	ArrayList<PageDescriptor> descriptors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		final Toolbar toolbar = find(R.id.toolbar);
		setSupportActionBar(toolbar);
		toolbar.setNavigationIcon(R.drawable.ic_menu_gold_24dp);
		ab = getSupportActionBar();
		assert ab != null;
		ab.setHomeButtonEnabled(true);
		ab.setDisplayShowTitleEnabled(true);

		drawer = find(R.id.drawer_layout);

		final NavigationView nav = find(R.id.nav);
		nav.setNavigationItemSelectedListener(this);

		lsAcus = find(R.id.lsAcus);
		lsAcus.setLayoutManager(new LinearLayoutManager(this));
		lsAcus.setAdapter(acusAdapter = new AcusAdapter());

		searchWrapper = new SearchWrapper(find(R.id.searchView));
		searchWrapper.setOnQueryTextListener(new SearchWrapper.OnQueryTextListener() {
			@NonNull
			private String proc(final @NonNull String query) {
				return query.trim().toLowerCase(Locale.US);
			}

			@Override
			public void onQueryTextSubmit(@NonNull final String query) {
				debouncer.submit(proc(query), 0);
			}

			@Override
			public void onQueryTextChange(@NonNull final String query) {
				debouncer.submit(proc(query));
			}
		});

		descriptors = new ArrayList<>();
		descriptors.add(new SimplePageDescriptor("dashboard", "Dashboard"));

		vp = find(R.id.vp);
		vp.setAdapter(pagerAdapter = new ContentAdapter(getSupportFragmentManager(), descriptors));

		Background.run(() -> Acu.INSTANCE.warmup());

		App.lbm().registerReceiver(requestNewPageReceiver, new IntentFilter(ACTION_REQUEST_NEW_PAGE));
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();

		App.lbm().unregisterReceiver(requestNewPageReceiver);
	}

	final BroadcastReceiver requestNewPageReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(final Context context, final Intent intent) {
			final String kind = intent.getStringExtra("kind");

			final PageDescriptor pd;
			switch (kind) {
				case "definition": {
					final int acu_id = intent.getIntExtra("acu_id", 0);
					pd = new SimplePageDescriptor("definition:" + acu_id + ":" + Math.random(), "Definition");
					break;
				}
				case "facet": {
					final String facet = intent.getStringExtra("facet");
					final String judul = intent.getStringExtra("judul");
					pd = new SimplePageDescriptor("facet:" + facet + ":" + judul + ":" + Math.random(), "Facet " + judul);
					break;
				}
				case "kategori": {
					final String facet = intent.getStringExtra("facet");
					final String nilai = intent.getStringExtra("nilai");
					pd = new SimplePageDescriptor("kategori:" + facet + ":" + nilai + ":" + Math.random(), "Kategori " + nilai);
					break;
				}
				default:
					pd = new SimplePageDescriptor("unknown" + ":" + Math.random(), "unknown");
			}

			// remove anything after current pos
			final int pos = vp.getCurrentItem();
			for (int i = pagerAdapter.getCount() - 1; i > pos; i--) {
				pagerAdapter.remove(i);
			}

			pagerAdapter.add(pd);
			vp.setCurrentItem(pagerAdapter.getCount() - 1, true);
		}
	};

	public static void requestDefinitionPage(final int acu_id) {
		App.lbm().sendBroadcast(new Intent(ACTION_REQUEST_NEW_PAGE)
			.putExtra("kind", "definition")
			.putExtra("acu_id", acu_id)
		);
	}

	public static void requestFacetPage(final String facet, final String judul) {
		App.lbm().sendBroadcast(new Intent(ACTION_REQUEST_NEW_PAGE)
			.putExtra("kind", "facet")
			.putExtra("facet", facet)
			.putExtra("judul", judul)
		);
	}

	public static void requestKategoriPage(final String facet, final String nilai) {
		App.lbm().sendBroadcast(new Intent(ACTION_REQUEST_NEW_PAGE)
			.putExtra("kind", "kategori")
			.putExtra("facet", facet)
			.putExtra("nilai", nilai)
		);
	}

	@Override
	protected void onStart() {
		super.onStart();

		stopSearchMode();
	}

	@Override
	public void onBackPressed() {
		if (isInSearchMode()) {
			stopSearchMode();
			return;
		}

		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
			return;
		}

		if (vp.getCurrentItem() != 0) {
			vp.setCurrentItem(vp.getCurrentItem() - 1, true);
			return;
		}

		super.onBackPressed();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onPrepareOptionsMenu(final Menu menu) {
		final MenuItem menuSearch = menu.findItem(R.id.menuSearch);
		menuSearch.setVisible(!isInSearchMode());

		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
			case android.R.id.home:
				if (isInSearchMode()) {
					stopSearchMode();
				} else {
					if (!drawer.isDrawerOpen(GravityCompat.START)) {
						drawer.openDrawer(GravityCompat.START);
					}
				}
				return true;
			case R.id.menuSearch:
				startSearchMode();
				return true;
		}

		return super.onOptionsItemSelected(item);
	}

	void startSearchMode() {
		visiblify(searchWrapper.root, lsAcus);
		searchWrapper.tSearchText.setText("");
		searchWrapper.tSearchText.requestFocus();
		showKeyboard(searchWrapper.tSearchText);

		supportInvalidateOptionsMenu();
	}

	void stopSearchMode() {
		gonify(searchWrapper.root, lsAcus);
		hideKeyboard(searchWrapper.tSearchText);

		supportInvalidateOptionsMenu();
	}

	boolean isInSearchMode() {
		return isVisible(searchWrapper.root);
	}

	final Debouncer<String, List<String>> debouncer = new Debouncer<String, List<String>>(200) {
		@Override
		public List<String> process(final String payload) {
			return Acu.INSTANCE.listAcus(payload);
		}

		@Override
		public void onResult(final List<String> result) {
			acusAdapter.setData(result);
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
			return new ViewHolder(getLayoutInflater().inflate(R.layout.item_acu, parent, false));
		}

		@Override
		public void onBindViewHolder(final RecyclerView.ViewHolder _holder_, final int position) {
			final ViewHolder holder = (ViewHolder) _holder_;
			{
				final String acu = acus.get(position);
				holder.text1.setText(acu);
			}

			holder.itemView.setOnClickListener(v -> {
				stopSearchMode();

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

	@Override
	public boolean onNavigationItemSelected(@NonNull MenuItem item) {
		final int itemId = item.getItemId();

		switch (itemId) {
			case R.id.menuBahasa:
				requestFacetPage("bahasa", "Bahasa");
				break;
			case R.id.menuBidang:
				requestFacetPage("bidang", "Bidang");
				break;
			case R.id.menuKelas:
				requestFacetPage("kelas", "Kelas kata");
				break;
			case R.id.menuRagam:
				requestFacetPage("ragam", "Ragam");
				break;
			case R.id.menuJenis:
				requestFacetPage("jenis", "Jenis");
				break;
			case R.id.menuAbout:
				final SpannableStringBuilder content = new SpannableStringBuilder(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")\n\n" +
					"© 2016-2018 Badan Pengembangan dan Pembinaan Bahasa, Kementerian Pendidikan dan Kebudayaan Republik Indonesia\n\n" +
					"Untuk informasi lebih lanjut, silakan mengunjungi KBBI V Daring kbbi.kemdikbud.go.id atau menghubungi kami melalui posel badan.bahasa@kemdikbud.go.id\n\n" +
					"Pengembang aplikasi:\n– David Moeljadi\n– Randy Sugianto (Yuku)\n– Jaya Satrio Hendrick\n– Kenny Hartono");

				LinkifyCompat.addLinks(content, Linkify.WEB_URLS | Linkify.EMAIL_ADDRESSES);

				final AlertDialog dialog = new AlertDialog.Builder(this)
					.setMessage(content)
					.setPositiveButton("OK", null)
					.setNeutralButton("Tim Penyusun", (dialog1, which) -> Background.run(() -> {
						//noinspection deprecation
						final Spanned text = Html.fromHtml(Exceptions.mustNotFail(() -> TextStreamsKt.readText(new InputStreamReader(getAssets().open("about/penyusun.html"), Charset.forName("utf-8")))));
						runOnUiThread(() -> new AlertDialog.Builder(this)
							.setMessage(text)
							.setPositiveButton("OK", null)
							.show()
						);
					}))
					.show();

				final TextView tv = (TextView) dialog.findViewById(android.R.id.message);
				if (tv != null) {
					tv.setMovementMethod(LinkMovementMethod.getInstance());
				}

				break;
		}

		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	class ContentAdapter extends ArrayPagerAdapter<ContentPage> {
		public ContentAdapter(final FragmentManager fragmentManager, final List<PageDescriptor> descriptors) {
			super(fragmentManager, descriptors);
		}

		@Override
		@NonNull
		protected ContentPage createFragment(final PageDescriptor pd) {
			final String tag = pd.getFragmentTag();
			final String[] args = tag.split(":");
			final String kind = args[0];

			switch (kind) {
				case "dashboard": {
					return new DashboardPage();
				}
				case "definition": {
					final DefinitionPage page = new DefinitionPage();
					final Bundle bundle = new Bundle();
					bundle.putInt("acu_id", Integer.parseInt(args[1]));
					page.setArguments(bundle);
					return page;
				}
				case "facet": {
					final FacetPage page = new FacetPage();
					final Bundle bundle = new Bundle();
					bundle.putString("facet", args[1]);
					bundle.putString("judul", args[2]);
					page.setArguments(bundle);
					return page;
				}
				case "kategori": {
					final KategoriPage page = new KategoriPage();
					final Bundle bundle = new Bundle();
					bundle.putString("facet", args[1]);
					bundle.putString("nilai", args[2]);
					page.setArguments(bundle);
					return page;
				}
			}
			return new UnknownPage();
		}
	}
}
