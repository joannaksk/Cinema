package com.example.joanna.cinema.data;

import net.simonvt.schematic.annotation.AutoIncrement;
import net.simonvt.schematic.annotation.ConflictResolutionType;
import net.simonvt.schematic.annotation.DataType;
import net.simonvt.schematic.annotation.PrimaryKey;
import net.simonvt.schematic.annotation.Unique;

import static net.simonvt.schematic.annotation.DataType.Type.INTEGER;
import static net.simonvt.schematic.annotation.DataType.Type.TEXT;

/**
 * Created by joanna on 07/07/16.
 */
public class MovieContract{

    public interface MovieColumns {
        @DataType(INTEGER) @PrimaryKey
        @AutoIncrement
        public static final String _ID = "_id";

        @DataType(INTEGER)
        public static final String _COUNT = "_count";

        @DataType(TEXT)
        public static final String COLUMN_MOVIE_ID = "movie_id";

        @DataType(TEXT) @Unique (onConflict = ConflictResolutionType.IGNORE)
        public static final String COLUMN_TITLE = "title";

        @DataType(TEXT)
        public static final String COLUMN_RELEASE_DATE = "release_date";

        @DataType(TEXT)
        public static final String COLUMN_DURATION = "duration";

        @DataType(TEXT) @Unique (onConflict = ConflictResolutionType.IGNORE)
        public static final String COLUMN_POSTER = "movie_poster";

        @DataType(INTEGER)
        public static final String COLUMN_POPULARITY = "popularity";

        @DataType(INTEGER)
        public final String COLUMN_VOTE_AVERAGE = "vote_average";

        @DataType(TEXT)
        public final String COLUMN_OVERVIEW = "overview";



    }
}
