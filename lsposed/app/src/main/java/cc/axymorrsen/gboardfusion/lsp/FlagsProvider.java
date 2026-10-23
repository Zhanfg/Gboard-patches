package cc.axymorrsen.gboardfusion.lsp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import java.util.Set;

public final class FlagsProvider extends ContentProvider {
    static final String AUTHORITY = "cc.axymorrsen.gboardfusion.lsp.flags";
    static final String PREFS = "gboard_lsp_flags";
    static final String EMOJI = "emoji";
    static final String ACCESSIBILITY = "accessibility";
    static final String CLIPBOARD = "clipboard";
    static final String ROUNDED = "rounded";
    static final String LANGUAGE = "language";
    static final Set<String> SUPPORTED = Set.of(EMOJI, ACCESSIBILITY, CLIPBOARD, ROUNDED, LANGUAGE);

    static boolean defaultFor(String key) {
        return EMOJI.equals(key) || ACCESSIBILITY.equals(key);
    }

    @Override public boolean onCreate() { return true; }

    @Override public Cursor query(Uri uri, String[] projection, String selection,
                                  String[] selectionArgs, String sortOrder) {
        if (uri == null || uri.getPathSegments().size() != 2
                || !"flag".equals(uri.getPathSegments().get(0))) return null;
        String key = uri.getLastPathSegment();
        if (!SUPPORTED.contains(key) || getContext() == null) return null;
        boolean enabled = getContext().getSharedPreferences(PREFS, 0)
                .getBoolean(key, defaultFor(key));
        MatrixCursor out = new MatrixCursor(new String[] {"enabled"});
        out.addRow(new Object[] {enabled ? 1 : 0});
        return out;
    }
    @Override public String getType(Uri uri) { return "vnd.android.cursor.item/vnd.gboardfusion.flag"; }
    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
