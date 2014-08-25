/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.db;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.miz.functions.ColumnIndexCache;
import com.miz.functions.EpisodeCounter;
import com.miz.functions.GridEpisode;
import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class DbAdapterTvShowEpisodes extends AbstractDbAdapter {

	// Database fields
	public static final String KEY_SEASON = "season";
	public static final String KEY_EPISODE = "episode";
	public static final String KEY_DATE_ADDED = "date_added";
	public static final String KEY_TO_WATCH = "to_watch";
	public static final String KEY_HAS_WATCHED = "has_watched";
	public static final String KEY_FAVOURITE = "favourite"; // Favorite
	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_EPISODE_TITLE = "episode_title";
	public static final String KEY_EPISODE_PLOT = "episode_description";
	public static final String KEY_EPISODE_AIRDATE = "episode_airdate";
	public static final String KEY_EPISODE_RATING = "episode_rating";
	public static final String KEY_EPISODE_DIRECTOR = "episode_director";
	public static final String KEY_EPISODE_WRITER = "episode_writer";
	public static final String KEY_EPISODE_GUESTSTARS = "episode_gueststars";

	public static final String DATABASE_TABLE = "tvshow_episodes";

	private final String[] ALL_COLUMNS = new String[]{KEY_SEASON, KEY_EPISODE, KEY_DATE_ADDED,
			KEY_TO_WATCH, KEY_HAS_WATCHED, KEY_FAVOURITE, KEY_SHOW_ID, KEY_EPISODE_TITLE, KEY_EPISODE_PLOT, KEY_EPISODE_AIRDATE,
			KEY_EPISODE_RATING, KEY_EPISODE_DIRECTOR, KEY_EPISODE_WRITER, KEY_EPISODE_GUESTSTARS};

	// Only use this when dealing with all rows
	private ColumnIndexCache mCache = new ColumnIndexCache();

	public DbAdapterTvShowEpisodes(Context context) {
		super(context);
	}

	public long createEpisode(String filepath, String season, String episode, String showId, String episodeTitle, String episodePlot,
			String episodeAirdate, String episodeRating, String episodeDirector, String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues initialValues = createContentValues(season, episode, showId, episodeTitle,
				episodePlot, episodeAirdate, episodeRating, episodeDirector, episodeWriter, episodeGuestStars, hasWatched, favorite);
		
		MizuuApplication.getTvShowEpisodeMappingsDbAdapter().createFilepathMapping(filepath, showId, season, episode);
		return mDatabase.insert(DATABASE_TABLE, null, initialValues);
	}

	public boolean updateSingleItem(String showId, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "'", null) > 0;
	}

	public boolean updateEpisode(String filepath, String season, String episode, String showId, String episodeTitle, String episodePlot,
			String episodeAirdate, String episodeRating, String episodeDirector, String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues initialValues = createContentValues(season,episode, showId, episodeTitle,
				episodePlot, episodeAirdate, episodeRating, episodeDirector, episodeWriter, episodeGuestStars, hasWatched, favorite);
		return mDatabase.update(DATABASE_TABLE, initialValues, KEY_SHOW_ID + "='" + showId + "'", null) > 0;
	}

	public boolean updateEpisode(String showId, String season, String episode, String table, String value) {
		ContentValues values = new ContentValues();
		values.put(table, value);
		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + season + "' AND " + KEY_EPISODE + "='" + episode + "'", null) > 0;
	}

	public Cursor getEpisode(String showId, int season, int episode) {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + MizLib.addIndexZero(season) + "' AND " + KEY_EPISODE + "='" + MizLib.addIndexZero(episode) + "'", null, null, null, null);
	}

	public static int OLDEST_FIRST = 0, NEWEST_FIRST = 1;
	public Cursor getAllEpisodes(String showId, int type) {
		if (type == OLDEST_FIRST)
			return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " asc, " + KEY_EPISODE + " asc");
		else
			return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " desc, " + KEY_EPISODE + " desc");
	}

	public Cursor getAllEpisodesInDatabase(boolean includeRemoved) {
		if (includeRemoved)
			return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, null, null, null, null, null);
		else
			return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, "NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, null, null, null);
	}

	public Cursor getAllIgnoredEpisodesInDatabase() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE'", null, null, null, null);
	}

	public Cursor getAllUnidentifiedEpisodesInDatabase() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = 'invalid'", null, null, null, null);
	}

	public boolean ignoreEpisode(String showId) {
		return updateSingleItem(showId, KEY_EPISODE_TITLE, "MIZ_REMOVED_EPISODE");
	}

	public boolean deleteEpisode(String showId, int season, int episode) {
		boolean result = true;
		if (getEpisodeCount(showId) == 1)
			result = MizuuApplication.getTvDbAdapter().deleteShow(showId);

		return result && mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" +
				MizLib.addIndexZero(season) + "' AND " + KEY_EPISODE + "='" + MizLib.addIndexZero(episode) + "'", null) > 0;
	}

	public boolean deleteAllEpisodes(String showId) {
		boolean result = MizuuApplication.getTvDbAdapter().deleteShow(showId);
		return result && mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "= '" + showId + "'", null) > 0;
	}

	public boolean deleteSeaon(String showId, String season) {	
		boolean result = mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + " = '" + showId + "' and " + KEY_SEASON + " = '" + season + "'", null) > 0;
		if (getEpisodeCount(showId) == 0)
			result = result && MizuuApplication.getTvDbAdapter().deleteShow(showId);

		return result;
	}

	public boolean deleteAllEpisodesInDatabase() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0 &&
				MizuuApplication.getTvDbAdapter().deleteAllShowsInDatabase();
	}

	public boolean deleteAllUnidentifiedEpisodes() {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + " = 'invalid'", null) > 0 &&
				MizuuApplication.getTvDbAdapter().deleteAllUnidentifiedShows();
	}

	public int getEpisodeCount(String showId) {
		Cursor c = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " asc, " + KEY_EPISODE + " asc");
		int count = c.getCount();
		c.close();
		return count;
	}

	public HashMap<String, EpisodeCounter> getSeasons(String showId) {
		HashMap<String, EpisodeCounter> results = new HashMap<String, EpisodeCounter>();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor c = null;
		try {
			c = mDatabase.query(DATABASE_TABLE, new String[]{KEY_SHOW_ID, KEY_EPISODE_TITLE, KEY_SEASON, KEY_HAS_WATCHED}, KEY_SHOW_ID + "='" + showId + "' AND NOT(" + KEY_EPISODE_TITLE + " = 'MIZ_REMOVED_EPISODE')", null, KEY_SEASON + "," + KEY_EPISODE, null, KEY_SEASON + " asc");
			while (c.moveToNext()) {
				String season = c.getString(cache.getColumnIndex(c, KEY_SEASON));

				EpisodeCounter counter = results.get(season);
				if (counter == null)
					counter = new EpisodeCounter();

				// Increment the episode count
				counter.incrementEpisodeCount();

				// Increment watched counter if the episode has been watched
				if (c.getString(cache.getColumnIndex(c, KEY_HAS_WATCHED)).equals("1"))
					counter.incrementWatchedCount();

				results.put(season, counter);
			}
		} catch (Exception e) {
		} finally {
			if (c != null)
				c.close();
		}

		return results;
	}

	public List<GridEpisode> getEpisodesInSeason(Context context, String showId, int season) {
		List<GridEpisode> episodes = new ArrayList<GridEpisode>();

		ColumnIndexCache cache = new ColumnIndexCache();
		Cursor cursor = null;
		
		try {
			String seasonWithIndex = MizLib.addIndexZero(season);
			cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' and " + KEY_SEASON + "='" + seasonWithIndex + "'", null, KEY_EPISODE, null, KEY_EPISODE + " asc");

			File temp;
			while (cursor.moveToNext()) {
				temp = MizLib.getTvShowEpisode(context, showId, cursor.getString(cache.getColumnIndex(cursor, KEY_SEASON)), cursor.getString(cache.getColumnIndex(cursor, KEY_EPISODE)));

				if (!temp.exists())
					temp = MizLib.getTvShowBackdrop(context, showId);

				episodes.add(new GridEpisode(context,
						cursor.getString(cache.getColumnIndex(cursor, KEY_EPISODE_TITLE)),
						MizuuApplication.getTvShowEpisodeMappingsDbAdapter().getFirstFilepath(showId, seasonWithIndex, cursor.getString(cache.getColumnIndex(cursor, KEY_EPISODE))),
						season,
						Integer.valueOf(cursor.getString(cache.getColumnIndex(cursor, KEY_EPISODE))),
						cursor.getString(cache.getColumnIndex(cursor, KEY_HAS_WATCHED)).equals("1"),
						temp));
			}
		} catch (Exception e) {} finally {
			cache.clear();
			if (cursor != null)
				cursor.close();
		}

		return episodes;
	}

	public String getLatestEpisodeAirdate(String showId) {
		String s = "";
		Cursor c = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_EPISODE_AIRDATE + " LIKE '%-%'", null, null, null, KEY_EPISODE_AIRDATE + " desc"); // %-% hack to make sure that the airdate includes a hyphen and is an actual date
		if (c.moveToFirst())
			s = c.getString(mCache.getColumnIndex(c, DbAdapterTvShowEpisodes.KEY_EPISODE_AIRDATE));
		c.close();
		return s;
	}

	public boolean hasUnwatchedEpisodes(String showId) {
		Cursor c = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = '" + showId + "' AND " + KEY_HAS_WATCHED + " = '0'", null, null, null, null);
		return c.getCount() > 0;
	}

	public boolean setSeasonWatchStatus(String showId, String season, boolean watched) {
		ContentValues values = new ContentValues();
		values.put(KEY_HAS_WATCHED, watched ? "1" : "0");
		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + season + "'", null) > 0;
	}
	
	public boolean setEpisodeWatchStatus(String showId, String season, String episode, boolean watched) {
		ContentValues values = new ContentValues();
		values.put(KEY_HAS_WATCHED, watched ? "1" : "0");
		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + season + "' AND " + KEY_EPISODE + "='" + episode + "'", null) > 0;
	}

	private ContentValues createContentValues(String season, String episode, String showId, String episodeTitle,
			String episodePlot, String episodeAirdate, String episodeRating, String episodeDirector,
			String episodeWriter, String episodeGuestStars, String hasWatched, String favorite) {
		ContentValues values = new ContentValues();
		values.put(KEY_SEASON, season);
		values.put(KEY_EPISODE, episode);
		values.put(KEY_SHOW_ID, showId);
		values.put(KEY_EPISODE_TITLE, episodeTitle);
		values.put(KEY_EPISODE_PLOT, episodePlot);
		values.put(KEY_EPISODE_AIRDATE, episodeAirdate);
		values.put(KEY_EPISODE_RATING, episodeRating);
		values.put(KEY_EPISODE_DIRECTOR, episodeDirector);
		values.put(KEY_EPISODE_WRITER, episodeWriter);
		values.put(KEY_EPISODE_GUESTSTARS, episodeGuestStars);
		values.put(KEY_HAS_WATCHED, hasWatched);
		values.put(KEY_FAVOURITE, favorite);
		return values;
	}
}