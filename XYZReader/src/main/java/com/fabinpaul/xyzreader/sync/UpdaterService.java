package com.fabinpaul.xyzreader.sync;

import android.app.IntentService;
import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.RemoteException;
import android.text.Html;
import android.text.format.DateUtils;
import android.util.Log;

import com.fabinpaul.xyzreader.R;
import com.fabinpaul.xyzreader.data.ItemsContract;
import com.fabinpaul.xyzreader.remote.RemoteEndpointUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;

public class UpdaterService extends IntentService {
    private static final String TAG = "UpdaterService";

    public static final String BROADCAST_ACTION_STATE_CHANGE
            = "com.fabinpaul.xyzreader.intent.action.STATE_CHANGE";
    public static final String EXTRA_REFRESHING
            = "com.fabinpaul.xyzreader.intent.extra.REFRESHING";

    public static final String EXTRA_SYNC_IMMEDIATELY
            = "com.fabinpaul.xyzreader.intent.extra.SYNC_IMMEDIATELY";

    public static final String SYNC_TIME = "com.fabinpaul.xyzreader.shared_pref.SYNC_TIME";

    private SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.sss");
    // Use default locale format
    private SimpleDateFormat outputFormat = new SimpleDateFormat();
    // Most time functions can only handle 1902 - 2037
    private GregorianCalendar START_OF_EPOCH = new GregorianCalendar(2, 1, 1);

    private static final long STALE_PERIOD = 3 * 60 * 60 * 1000; // 3 hours

    public UpdaterService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        boolean syncImmediately = intent.getBooleanExtra(EXTRA_SYNC_IMMEDIATELY, false);

        if (!isSyncNeeded(syncImmediately))
            return;

        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo ni = cm.getActiveNetworkInfo();
        if (ni == null || !ni.isConnected()) {
            Log.w(TAG, "Not online, not refreshing.");
            return;
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, true));

        // Don't even inspect the intent, we only do one thing, and that's fetch content.
        ArrayList<ContentProviderOperation> cpo = new ArrayList<ContentProviderOperation>();

        Uri dirUri = ItemsContract.Items.buildDirUri();

        // Delete all items
        cpo.add(ContentProviderOperation.newDelete(dirUri).build());

        try {
            JSONArray array = RemoteEndpointUtil.fetchJsonArray();
            if (array == null) {
                throw new JSONException("Invalid parsed item array");
            }

            for (int i = 0; i < array.length(); i++) {
                ContentValues values = new ContentValues();
                JSONObject object = array.getJSONObject(i);
                values.put(ItemsContract.Items.SERVER_ID, object.getString("id"));
                values.put(ItemsContract.Items.AUTHOR, object.getString("author"));
                values.put(ItemsContract.Items.TITLE, object.getString("title"));
                String articleBody = Html.fromHtml(object.getString("body").replaceAll("(\r\n|\n)", "<br />")).toString();
                values.put(ItemsContract.Items.BODY, articleBody);
                values.put(ItemsContract.Items.THUMB_URL, object.getString("thumb"));
                values.put(ItemsContract.Items.PHOTO_URL, object.getString("photo"));
                values.put(ItemsContract.Items.ASPECT_RATIO, object.getString("aspect_ratio"));
                Date publishedDate = parsePublishedDate(object.getString("published_date"));
                String publishedDateString = "";
                if (!publishedDate.before(START_OF_EPOCH.getTime())) {
                    publishedDateString = DateUtils.getRelativeTimeSpanString(
                            publishedDate.getTime(),
                            System.currentTimeMillis(), DateUtils.HOUR_IN_MILLIS,
                            DateUtils.FORMAT_ABBREV_ALL).toString();

                } else {
                    // If date is before 1902, just show the string
                    publishedDateString = outputFormat.format(publishedDate);
                }
                values.put(ItemsContract.Items.PUBLISHED_DATE, publishedDateString);
                cpo.add(ContentProviderOperation.newInsert(dirUri).withValues(values).build());
            }

            getContentResolver().applyBatch(ItemsContract.CONTENT_AUTHORITY, cpo);
            setSyncTime();

        } catch (JSONException | RemoteException | OperationApplicationException e) {
            Log.e(TAG, "Error updating content.", e);
        }

        sendStickyBroadcast(
                new Intent(BROADCAST_ACTION_STATE_CHANGE).putExtra(EXTRA_REFRESHING, false));
    }

    private Date parsePublishedDate(String date) {
        try {
            return dateFormat.parse(date);
        } catch (ParseException ex) {
            Log.e(TAG, ex.getMessage());
            Log.i(TAG, "passing today's date");
            return new Date();
        }
    }

    private void setSyncTime() {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.sync_preference), Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putLong(SYNC_TIME, System.currentTimeMillis());
        editor.apply();
    }

    private boolean isSyncNeeded(boolean syncImmediately) {
        SharedPreferences preferences = getSharedPreferences(getString(R.string.sync_preference), Context.MODE_PRIVATE);
        long lastSyncTime = preferences.getLong(SYNC_TIME, System.currentTimeMillis());
        if (syncImmediately)
            return true;
        else
            return lastSyncTime == System.currentTimeMillis() || lastSyncTime + STALE_PERIOD <= System.currentTimeMillis();
    }
}
