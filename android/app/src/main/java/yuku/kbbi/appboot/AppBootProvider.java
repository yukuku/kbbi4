package yuku.kbbi.appboot;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import androidx.annotation.Nullable;
import yuku.kbbi.App;

public class AppBootProvider extends ContentProvider {
	@Override
	public boolean onCreate() {
		App.context = getContext().getApplicationContext();
		return true;
	}

	@Nullable
	@Override
	public Cursor query(final Uri uri, final String[] projection, final String selection, final String[] selectionArgs, final String sortOrder) {
		return null;
	}

	@Nullable
	@Override
	public String getType(final Uri uri) {
		return null;
	}

	@Nullable
	@Override
	public Uri insert(final Uri uri, final ContentValues values) {
		return null;
	}

	@Override
	public int delete(final Uri uri, final String selection, final String[] selectionArgs) {
		return 0;
	}

	@Override
	public int update(final Uri uri, final ContentValues values, final String selection, final String[] selectionArgs) {
		return 0;
	}
}
