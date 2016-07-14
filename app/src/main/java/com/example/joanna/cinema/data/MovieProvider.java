package com.example.joanna.cinema.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import net.simonvt.schematic.annotation.ContentProvider;
import net.simonvt.schematic.annotation.ContentUri;
import net.simonvt.schematic.annotation.InexactContentUri;
import net.simonvt.schematic.annotation.NotifyBulkInsert;
import net.simonvt.schematic.annotation.NotifyDelete;
import net.simonvt.schematic.annotation.NotifyInsert;
import net.simonvt.schematic.annotation.NotifyUpdate;
import net.simonvt.schematic.annotation.TableEndpoint;

/**
 * Created by joanna on 07/07/16.
 */
@ContentProvider(
        authority = MovieProvider.AUTHORITY,
        database = MovieDatabase.class,
        packageName = "com.example.joanna.cinema.provider")
public class MovieProvider {

    private MovieProvider() {

    }

    public static final String AUTHORITY = "com.example.joanna.cinema.MovieProvider";

    static final Uri BASE_CONTENT_URI = Uri.parse("content://" + AUTHORITY);

    interface Path {
        String MOVIES = "movies";
        String MOVIES_BY_POPULARITY = "movies/sort/most_popular";
        String MOVIES_BY_RATING = "movies/sort/top_rated";
//        String FROM_LIST = "fromList";
    }

    private static Uri buildUri(String... paths) {
        Uri.Builder builder = BASE_CONTENT_URI.buildUpon();
        for (String path : paths) {
            builder.appendPath(path);
        }
        return builder.build();
    }

    @TableEndpoint(table = MovieDatabase.MOVIES) public static class Movies {

        @ContentUri(
                path = Path.MOVIES,
                type = "vnd.android.cursor.dir/movies")
        public static final Uri LIST_CONTENT_URI = buildUri(Path.MOVIES);

        @ContentUri(
                path = Path.MOVIES_BY_POPULARITY,
                type = "vnd.android.cursor.dir/movies",
                defaultSort = MovieContract.MovieColumns.COLUMN_POPULARITY + " DESC")
        public static final Uri BY_POPULARITY_CONTENT_URI = buildUri(Path.MOVIES_BY_POPULARITY);

        @ContentUri(
                path = Path.MOVIES_BY_RATING,
                type = "vnd.android.cursor.dir/movies",
                defaultSort = MovieContract.MovieColumns.COLUMN_VOTE_AVERAGE + " DESC")
        public static final Uri BY_RATING_CONTENT_URI = buildUri(Path.MOVIES_BY_RATING);

        @InexactContentUri(
                name = "MOVIE_ID",
                path = Path.MOVIES + "/#",
                type = "vnd.android.cursor.item/movie",
                whereColumn = MovieContract.MovieColumns.COLUMN_MOVIE_ID,
                pathSegment = 1)

        public static Uri withId(long id) {
            return buildUri(Path.MOVIES, String.valueOf(id));
        }

        @NotifyInsert(paths = Path.MOVIES)
        public static Uri[] onInsert(ContentValues values) {
            final int movieId = values.getAsInteger(MovieContract.MovieColumns.COLUMN_MOVIE_ID);
            return new Uri[] {
                    Movies.withId(movieId),
            };
        }

        @NotifyBulkInsert(paths = Path.MOVIES)
        public static Uri[] onBulkInsert(Context context, Uri uri, ContentValues[] values, long[] ids) {
            return new Uri[] {
                    uri,
            };
        }

        @NotifyUpdate(paths = Path.MOVIES + "/#")
        public static Uri[] onUpdate(Context context, Uri uri, String where, String[] whereArgs) {
            final long noteId = Long.valueOf(uri.getPathSegments().get(1));
            Cursor c = context.getContentResolver().query(
                    uri,
                    null,
                    null,
                    null,
                    null);
            c.moveToFirst();
            c.close();

            return new Uri[] {
                    withId(noteId)
            };
        }

        @NotifyDelete(paths = Path.MOVIES + "/#")
        public static Uri[] onDelete(Context context, Uri uri) {

            final long noteId = Long.valueOf(uri.getPathSegments().get(1));
            Cursor c = context.getContentResolver().query(uri, null, null, null, null);
            c.moveToFirst();
            c.close();

            return new Uri[] {
                    withId(noteId),
            };
        }
    }
}
