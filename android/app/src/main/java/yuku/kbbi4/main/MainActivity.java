package yuku.kbbi4.main;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import com.commonsware.cwac.pager.PageDescriptor;
import com.commonsware.cwac.pager.SimplePageDescriptor;
import com.commonsware.cwac.pager.v4.ArrayPagerAdapter;
import yuku.kbbi4.BaseActivity;
import yuku.kbbi4.R;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends BaseActivity implements NavigationView.OnNavigationItemSelectedListener {

	DrawerLayout drawer;
	ViewPager vp;

	ContentAdapter adapter;
	ArrayList<PageDescriptor> descriptors;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		final Toolbar toolbar = find(R.id.toolbar);
		setSupportActionBar(toolbar);

		drawer = find(R.id.drawer_layout);

		final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
		drawer.addDrawerListener(toggle);
		toggle.syncState();

		final NavigationView nav = find(R.id.nav);
		nav.setNavigationItemSelectedListener(this);

		descriptors = new ArrayList<>();
		descriptors.add(new SimplePageDescriptor("dashboard", "Dashboard"));
		descriptors.add(new SimplePageDescriptor("unknown", "Unknown"));

		vp = find(R.id.vp);
		vp.setAdapter(adapter = new ContentAdapter(getSupportFragmentManager(), descriptors));
	}

	@Override
	public void onBackPressed() {
		if (drawer.isDrawerOpen(GravityCompat.START)) {
			drawer.closeDrawer(GravityCompat.START);
		} else {
			super.onBackPressed();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.menu_main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();

		//noinspection SimplifiableIfStatement
		if (id == R.id.menuAbout) {
			return true;
		}

		return super.onOptionsItemSelected(item);
	}

	@SuppressWarnings("StatementWithEmptyBody")
	@Override
	public boolean onNavigationItemSelected(MenuItem item) {
		// Handle navigation view item clicks here.
		int id = item.getItemId();

		drawer.closeDrawer(GravityCompat.START);
		return true;
	}

	static class ContentAdapter extends ArrayPagerAdapter<ContentPage> {
		public ContentAdapter(final FragmentManager fragmentManager, final List<PageDescriptor> descriptors) {
			super(fragmentManager, descriptors);
		}

		@Override
		@NonNull
		protected ContentPage createFragment(final PageDescriptor pd) {
			final String tag = pd.getFragmentTag();
			final String kind;
			if (tag.contains(":")) {
				kind = tag.substring(0, tag.indexOf(':'));
			} else {
				kind = tag;
			}
			switch (kind) {
				case "dashboard":
					return new DashboardPage();
			}
			return new UnknownPage();
		}
	}
}
