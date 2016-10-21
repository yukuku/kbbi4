package yuku.kbbi4.main;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.commonsware.cwac.pager.BuildConfig;
import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.SimplePageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;
import yuku.kbbi4.App;
import yuku.kbbi4.BaseActivity;
import yuku.kbbi4.R;
import yuku.kbbi4.dictdata.Acu;
import yuku.kbbi4.util.Background;
import yuku.kbbi4.util.Debouncer;
import yuku.kbbi4.widget.SearchWrapper;

import java.util.ArrayList;
import java.util.List;

import static yuku.kbbi4.util.Views.*;

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
		toolbar.setNavigationIcon(R.drawable.ic_menu_white_24dp);
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
			@Override
			public void onQueryTextSubmit(@NonNull final String query) {
				debouncer.submit(query.trim(), 0);
			}

			@Override
			public void onQueryTextChange(@NonNull final String query) {
				debouncer.submit(query.trim());
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
					pd = new SimplePageDescriptor("facet:" + facet + ":" + Math.random(), "Facet " + facet);
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

	public static void requestFacetPage(final String facet) {
		App.lbm().sendBroadcast(new Intent(ACTION_REQUEST_NEW_PAGE)
			.putExtra("kind", "facet")
			.putExtra("facet", facet)
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
				requestFacetPage("bahasa");
				break;
			case R.id.menuBidang:
				requestFacetPage("bidang");
				break;
			case R.id.menuKelas:
				requestFacetPage("kelas");
				break;
			case R.id.menuRagam:
				requestFacetPage("ragam");
				break;
			case R.id.menuJenis:
				requestFacetPage("jenis");
				break;
			case R.id.menuAbout:
				new AlertDialog.Builder(this)
					.setMessage(getString(R.string.app_name) + " " + BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")")
					.setPositiveButton("OK", null)
					.show();
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
					final Intent intent = Henson.with(MainActivity.this).gotoDefinitionPage()
						.acu_id(Integer.parseInt(args[1]))
						.build();
					final DefinitionPage page = new DefinitionPage();
					page.setArguments(intent.getExtras());
					return page;
				}
				case "facet": {
					final Intent intent = Henson.with(MainActivity.this).gotoFacetPage()
						.facet(args[1])
						.build();
					final FacetPage page = new FacetPage();
					page.setArguments(intent.getExtras());
					return page;
				}
				case "kategori": {
					final Intent intent = Henson.with(MainActivity.this).gotoKategoriPage()
						.facet(args[1])
						.nilai(args[2])
						.build();
					final KategoriPage page = new KategoriPage();
					page.setArguments(intent.getExtras());
					return page;
				}
			}
			return new UnknownPage();
		}
	}
}
