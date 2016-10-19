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
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
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
				case "definition":
					final int acu_id = intent.getIntExtra("acu_id", 0);
					pd = new SimplePageDescriptor("definition:" + acu_id, "Definition");
					break;
				case "jenis":
					final String jenis = intent.getStringExtra("jenis");
					pd = new SimplePageDescriptor("jenis:" + jenis, "Jenis " + jenis);
					break;
				default:
					pd = new SimplePageDescriptor("unknown", "unknown");
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

	public static void requestJenisPage(final String jenis) {
		App.lbm().sendBroadcast(new Intent(ACTION_REQUEST_NEW_PAGE)
			.putExtra("kind", "jenis")
			.putExtra("jenis", jenis)
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

		if (itemId == R.id.menuBahasa) {
			requestJenisPage("bahasa");
		} else if (itemId == R.id.menuBidang) {
			requestJenisPage("bidang");
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
			final String kind;
			final String addl;
			if (tag.contains(":")) {
				final int p = tag.indexOf(':');
				kind = tag.substring(0, p);
				addl = tag.substring(p + 1);
			} else {
				kind = tag;
				addl = null;
			}
			switch (kind) {
				case "dashboard": {
					return new DashboardPage();
				}
				case "definition": {
					final Intent intent = Henson.with(MainActivity.this).gotoDefinitionPage()
						.acu_id(Integer.parseInt(addl))
						.build();
					final DefinitionPage page = new DefinitionPage();
					page.setArguments(intent.getExtras());
					return page;
				}
				case "jenis": {
					final Intent intent = Henson.with(MainActivity.this).gotoJenisPage()
						.jenis(addl)
						.build();
					final JenisPage page = new JenisPage();
					page.setArguments(intent.getExtras());
					return page;
				}
			}
			return new UnknownPage();
		}
	}
}
