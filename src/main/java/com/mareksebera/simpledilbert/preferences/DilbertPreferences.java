package com.mareksebera.simpledilbert.preferences;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import com.mareksebera.simpledilbert.R;
import com.mareksebera.simpledilbert.favorites.FavoritedItem;

import org.intellij.lang.annotations.MagicConstant;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;

public final class DilbertPreferences {

    private final SharedPreferences preferences;
    private final SharedPreferences.Editor editor;

    private static final String PREF_CURRENT_DATE = "dilbert_current_date";
    private static final String PREF_CURRENT_URL = "dilbert_current_url";
    private static final String PREF_HIGH_QUALITY_ENABLED = "dilbert_use_high_quality";
    private static final String PREF_DARK_LAYOUT = "dilbert_dark_layout";
    private static final String PREF_DARK_WIDGET_LAYOUT = "dilbert_dark_layout_widget";
    private static final String PREF_FORCE_LANDSCAPE = "dilbert_force_landscape";
    private static final String PREF_HIDE_TOOLBARS = "dilbert_hide_toolbars";
    private static final String PREF_DOWNLOAD_TARGET = "dilbert_download_target_folder";
    private static final String PREF_SHARE_IMAGE = "dilbert_share_with_image";
    private static final String PREF_MOBILE_NETWORK = "dilbert_using_slow_network";
    private static final String PREF_REVERSE_LANDSCAPE = "dilbert_reverse_landscape";
    private static final String PREF_OPEN_AT_LATEST = "dilbert_open_at_latest_strip";
    private static final String PREF_WIDGET_ALWAYS_SHOW_LATEST = "dilbert_widget_always_show_latest";
    private static final String TAG = "DilbertPreferences";
    public static final DateTimeZone TIME_ZONE = DateTimeZone
            .forID("America/Chicago");
    public static final DateTimeFormatter DATE_FORMATTER = DateTimeFormat
            .forPattern("yyyy-MM-dd");

    @SuppressLint("CommitPrefEdits")
    public DilbertPreferences(Context context) {
        preferences = PreferenceManager.getDefaultSharedPreferences(context);
        editor = preferences.edit();
    }

    /**
     * Retrieves last viewed strip date
     *
     * @return LocalDate of last viewed strip
     */
    public LocalDate getCurrentDate() {
        String savedDate = preferences.getString(PREF_CURRENT_DATE, null);
        if (savedDate == null || isShouldOpenAtLatestStrip()) {
            return LocalDate.now(DilbertPreferences.TIME_ZONE);
        } else {
            return LocalDate.parse(savedDate, DATE_FORMATTER);
        }
    }

    /**
     * Saves last viewed date in preferences
     *
     * @param currentDate date of last viewed strip
     * @return if saving was successfull
     */
    public boolean saveCurrentDate(LocalDate currentDate) {
        editor.putString(PREF_CURRENT_DATE,
                currentDate.toString(DilbertPreferences.DATE_FORMATTER));
        return editor.commit();
    }

    /**
     * Returns state of high-quality user preference
     *
     * @return whether downloading high-quality images is enabled
     */
    public boolean isHighQualityOn() {
        return preferences.getBoolean(PREF_HIGH_QUALITY_ENABLED, true);
    }

    /**
     * Saves retrieved URL for date
     *
     * @param date string identifier of comics strip
     * @param s    url of strip
     * @return if saving was successfull
     */
    public boolean saveCurrentUrl(String date, String s) {
        editor.putString(PREF_CURRENT_URL, s);
        editor.putString(date, s);
        return editor.commit();
    }

    /**
     * Returns cached URL for provided date
     *
     * @param dateKey LocalDate of item
     * @return cached URL or null
     */
    public String getCachedUrl(LocalDate dateKey) {
        return getCachedUrl(dateKey.toString(DATE_FORMATTER));
    }

    /**
     * Returns cached URL for provided date
     *
     * @param dateKey string key of date
     * @return cached URL or null
     */
    String getCachedUrl(String dateKey) {
        return preferences.getString(dateKey, null);
    }

    /**
     * Removes cached URL for provided date
     *
     * @param currentDate date for which the cache should be deleted
     * @return if the removal was successfull
     */
    public boolean removeCache(LocalDate currentDate) {
        return editor.remove(
                currentDate.toString(DilbertPreferences.DATE_FORMATTER))
                .commit();
    }

    /**
     * Verifies if the date is associated with favorited flag
     *
     * @param currentDay date of favorited item
     * @return whether the date is favorited or not
     */
    public boolean isFavorited(LocalDate currentDay) {
        return preferences.getBoolean(toFavoritedKey(currentDay), false);
    }

    /**
     * Toggles favorite state for provided date
     *
     * @param currentDay date of favorited item
     * @return final state of toggling (last value inverted)
     */
    public boolean toggleIsFavorited(LocalDate currentDay) {
        boolean newState = !isFavorited(currentDay);
        editor.putBoolean(toFavoritedKey(currentDay), newState).commit();
        return newState;
    }

    /**
     * Gets preferences key for favorited item
     *
     * @param currentDay date of favorited item
     * @return preferences key for favorited item
     */
    private String toFavoritedKey(LocalDate currentDay) {
        return "favorite_"
                + currentDay.toString(DilbertPreferences.DATE_FORMATTER);
    }

    /**
     * Retrieves list of favorited items, which is stored in as plain in local preferences
     *
     * @return List of favorited items (not null but may be empty)
     */
    public List<FavoritedItem> getFavoritedItems() {
        List<FavoritedItem> favorites = new ArrayList<>();
        Map<String, ?> allPreferences = preferences.getAll();
        if (allPreferences != null) {
            for (String key : allPreferences.keySet()) {
                if (key.startsWith("favorite_")
                        && (Boolean) allPreferences.get(key)) {
                    String date = key.replace("favorite_", "");
                    favorites.add(new FavoritedItem(LocalDate.parse(date,
                            DATE_FORMATTER)));
                }
            }
        }
        Collections.sort(favorites, new Comparator<FavoritedItem>() {

            @Override
            public int compare(FavoritedItem lhs, FavoritedItem rhs) {
                return lhs.getDate().compareTo(rhs.getDate());
            }
        });
        return favorites;
    }


    /**
     * Tries to launch DownloadManager with visible notification to download file from URL to
     * downloadble path or user selected
     *
     * @param activity    Activity context to access system services and Toast notifications
     * @param downloadUrl url from which the image is downloaded
     * @param stripDate   date of strip being downloaded
     */
    public void downloadImageViaManager(final Activity activity,
                                        final String downloadUrl, LocalDate stripDate) {
        downloadImageViaManager(activity, downloadUrl, stripDate, false);
    }

    /**
     * Tries to launch DownloadManager with visible notification to download file from URL to
     * downloadble path or user selected
     *
     * @param activity       Activity context to access system services and Toast notifications
     * @param downloadToTemp boolean whether the file should be first downloaded to temp location
     * @param downloadUrl    url from which the image is downloaded
     * @param stripDate      date of strip being downloaded
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    @SuppressWarnings("deprecation")
    public void downloadImageViaManager(final Activity activity,
                                        final String downloadUrl, LocalDate stripDate, boolean downloadToTemp) {
        try {
            DownloadManager dm = (DownloadManager) activity
                    .getSystemService(Context.DOWNLOAD_SERVICE);
            String url = toHighQuality(downloadUrl);
            DownloadManager.Request request = new DownloadManager.Request(
                    Uri.parse(url));
            String downloadDate = DATE_FORMATTER.print(stripDate);
            Uri userPath = Uri.withAppendedPath(
                    Uri.parse("file://" + getDownloadTarget()),
                    downloadDate + ".gif");
            if (downloadToTemp) {
                request.setDestinationUri(
                        Uri.withAppendedPath(
                                Uri.fromFile(
                                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)),
                                downloadDate + ".gif"));
                scheduleFileToMove(downloadDate, userPath);
            } else {
                request.setDestinationUri(userPath);
            }
            request.setVisibleInDownloadsUi(true);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
                request.allowScanningByMediaScanner();
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            } else {
                request.setShowRunningNotification(true);
            }
            dm.enqueue(request);
        } catch (SecurityException se) {
            if (!downloadToTemp) {
                downloadImageViaManager(activity, downloadUrl, stripDate, true);
            } else {
                Toast.makeText(activity, "Cannot download to selected folder", Toast.LENGTH_LONG).show();
                Log.e(TAG, "Folder not supported", se);
            }
        } catch (Throwable t) {
            Log.e(TAG, "Should not happen", t);
            Toast.makeText(activity, R.string.download_manager_unsupported,
                    Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Schedules moving downloaded file to targetPath
     *
     * @param downloadDate date of strip in format YYYY-MM-DD
     * @param targetPath   path to which the file should be moved
     */
    private void scheduleFileToMove(String downloadDate, Uri targetPath) {
        editor.putString("move_" + downloadDate.replace("-", "_"), targetPath.toString()).commit();
    }

    /**
     * Retrieves requested target path, to which the downloaded file should be moved
     *
     * @param downloadDate date in format YYYY-MM-DD
     * @return local target path (from {@link android.net.Uri#toString()}
     */
    public String getScheduledTargetPath(String downloadDate) {
        return preferences.getString("move_" + downloadDate.replace("-", "_"), null);
    }

    /**
     * Converts URL to target high quality image
     *
     * @param url original url to be modified
     * @return url after modifications
     */
    public String toHighQuality(String url) {
        if (url == null)
            return null;
        return url.replace(".gif", ".zoom.gif").replace("zoom.zoom", "zoom");
    }

    /**
     * Converts URL to target low iamge quality
     *
     * @param date date associated with url
     * @param url  url which should be modified
     * @return url after changes
     */
    public String toLowQuality(LocalDate date, String url) {
        if (url == null)
            return null;
        if (date.getDayOfWeek() == DateTimeConstants.SUNDAY) {
            return url.replace(".zoom.gif", ".sunday.gif").replace("zoom.zoom",
                    "zoom");
        }
        return url.replace(".zoom.gif", ".gif").replace("zoom.zoom", "zoom");
    }

    /**
     * Saves date for widget, after user made change to it
     *
     * @param appWidgetId id of widget
     * @param date        date which is currently selected
     * @return boolean if save was successfull
     */
    public boolean saveDateForWidgetId(int appWidgetId, LocalDate date) {
        date = validateDate(date);
        return editor.putString("widget_" + appWidgetId,
                date.toString(DATE_FORMATTER)).commit();
    }

    /**
     * Returns saved date for specific widget id
     *
     * @param appWidgetId id of widget
     * @return Date which is saved or todays day if there is no such
     */
    public LocalDate getDateForWidgetId(int appWidgetId) {
        String savedDate = preferences.getString("widget_" + appWidgetId, null);
        if (savedDate == null || isWidgetAlwaysShowLatest())
            return LocalDate.now();
        else
            return LocalDate.parse(savedDate, DATE_FORMATTER);
    }

    /**
     * Generated random date within range of Dilbert existence
     *
     * @return validated random date
     */
    public static LocalDate getRandomDate() {
        Random random = new Random();
        LocalDate now = LocalDate.now();
        int year = 1989 + random.nextInt(now.getYear() - 1989);
        int month = 1 + random.nextInt(12);
        int day = random.nextInt(31);
        return validateDate(LocalDate.parse(
                String.format(new Locale("en"), "%d-%d-1", year, month))
                .plusDays(day));
    }

    /**
     * First strip was published on 16.4.1989
     *
     * @return LocalDate date of first Dilbert comics strip
     * @see <a href="http://en.wikipedia.org/wiki/Dilbert">Wikipedia</a>
     */
    public static LocalDate getFirstStripDate() {
        return LocalDate.parse("1989-04-16",
                DilbertPreferences.DATE_FORMATTER);
    }

    /**
     * Removing widget from launcher will cause deleting it's save date from preferences
     *
     * @param widgetId id of widget
     * @return boolean whether delete was successfull or not
     */
    public boolean deleteDateForWidgetId(int widgetId) {
        return editor.remove("widget_" + widgetId).commit();
    }

    /**
     * Validates selected date and filters ot future dates and dates before Dilbert started
     *
     * @param selDate Date user selected
     * @return LocalDate date which is correct
     */
    private static LocalDate validateDate(LocalDate selDate) {
        if (selDate.isAfter(LocalDate.now())) {
            selDate = LocalDate.now();
        }
        if (selDate.isBefore(DilbertPreferences.getFirstStripDate())) {
            selDate = DilbertPreferences.getFirstStripDate();
        }
        return selDate;
    }

    /**
     * Checks, if user preference is to force landscape orientation
     *
     * @return boolean whether user requires landscape orientation
     */
    public boolean isForceLandscape() {
        return preferences.getBoolean(PREF_FORCE_LANDSCAPE, false);
    }

    /**
     * Sets user preference of display orientation
     *
     * @param force whether to force ladndscape or not
     * @return boolean if saving preference was successfull
     */
    public boolean setIsForceLandscape(boolean force) {
        return editor.putBoolean(PREF_FORCE_LANDSCAPE, force).commit();
    }

    public boolean isDarkLayoutEnabled() {
        return preferences.getBoolean(PREF_DARK_LAYOUT, false);
    }

    public boolean setIsDarkLayoutEnabled(boolean dark) {
        return editor.putBoolean(PREF_DARK_LAYOUT, dark).commit();
    }

    public boolean isToolbarsHidden() {
        return preferences.getBoolean(PREF_HIDE_TOOLBARS, false);
    }

    public boolean setIsToolbarsHidden(boolean hidden) {
        return editor.putBoolean(PREF_HIDE_TOOLBARS, hidden).commit();
    }

    public boolean setIsHighQualityOn(boolean enabled) {
        return editor.putBoolean(PREF_HIGH_QUALITY_ENABLED, enabled).commit();
    }

    public boolean isDarkWidgetLayoutEnabled() {
        return preferences.getBoolean(PREF_DARK_WIDGET_LAYOUT, false);
    }

    public boolean setIsDarkWidgetLayoutEnabled(boolean dark) {
        return editor.putBoolean(PREF_DARK_WIDGET_LAYOUT, dark).commit();
    }

    public String getDownloadTarget() {
        return preferences.getString(
                PREF_DOWNLOAD_TARGET,
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath());
    }

    public boolean setDownloadTarget(String absolutePath) {
        return absolutePath != null && editor.putString(PREF_DOWNLOAD_TARGET, absolutePath).commit();
    }

    public boolean isSharingImage() {
        return preferences.getBoolean(PREF_SHARE_IMAGE, true);
    }

    public boolean setIsSharingImage(boolean shouldShareImage) {
        return editor.putBoolean(PREF_SHARE_IMAGE, shouldShareImage).commit();
    }

    /**
     * Setter for slow network flag
     *
     * @param isSlowNetwork whether application is on slow network
     * @return boolean if the save was successfull
     */
    public boolean setIsSlowNetwork(boolean isSlowNetwork) {
        return editor.putBoolean(PREF_MOBILE_NETWORK, isSlowNetwork).commit();
    }

    /**
     * Getter for slow network flag
     *
     * @return whether application assumes user is connected over slow network
     */
    public boolean isSlowNetwork() {
        return preferences.getBoolean(PREF_MOBILE_NETWORK, true);
    }

    public boolean setIsReversedLandscape(boolean isReversed) {
        return preferences.edit().putBoolean(PREF_REVERSE_LANDSCAPE, isReversed).commit();
    }

    public boolean isReversedLandscape() {
        return preferences.getBoolean(PREF_REVERSE_LANDSCAPE, false);
    }

    public boolean setShouldOpenAtLatestStrip(boolean should) {
        return preferences.edit().putBoolean(PREF_OPEN_AT_LATEST, should).commit();
    }

    public boolean isShouldOpenAtLatestStrip() {
        return preferences.getBoolean(PREF_OPEN_AT_LATEST, false);
    }

    public boolean setWidgetAlwaysShowLatest(boolean alwaysShowLatest) {
        return preferences.edit().putBoolean(PREF_WIDGET_ALWAYS_SHOW_LATEST, alwaysShowLatest).commit();
    }

    public boolean isWidgetAlwaysShowLatest() {
        return preferences.getBoolean(PREF_WIDGET_ALWAYS_SHOW_LATEST, false);
    }

    @TargetApi(Build.VERSION_CODES.GINGERBREAD)
    @MagicConstant(intValues = {ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE, ActivityInfo.SCREEN_ORIENTATION_SENSOR})
    public int getLandscapeOrientation() {
        return isForceLandscape() ?
                (Build.VERSION.SDK_INT >= 9 && isReversedLandscape()) ?
                        ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_SENSOR;
    }
}
