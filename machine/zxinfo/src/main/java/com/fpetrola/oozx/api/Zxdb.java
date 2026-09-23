/*
 * Copyright (c) 2023-2026 Fernando Damian Petrola
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.fpetrola.oozx.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fpetrola.oozx.config.Configuration;
import com.fpetrola.oozx.speccy.media.DownloadAndUnzip;
import org.sqlite.SQLiteConnection;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipInputStream;

/**
 * ZXDB read on this machine: the database ZXInfo is built from, asked here with SQL and answered
 * in the shape the web service answers in, so that whoever reads an entry cannot tell which of the
 * two gave it - the files it is downloaded from, its releases, maps and recordings, and the TOSEC
 * dumps of what ZXDB may not hand out.
 * <p>
 * The whole of it is in ~/.oozx once {@link #main} has brought it down. Until then the part that
 * ships with the build answers, which is the five thousand most voted games: fewer games, the same
 * answers.
 */
public class Zxdb implements KnowsTheGames {

  /** Where the whole database goes once it has been brought down, and is asked from then on. */
  public static final File WHOLE = new File(Configuration.home(), "zxdb.sqlite");
  private static final String SHIPPED = "/zxdb.sqlite";
  private static final String DUMP = "https://github.com/zxdb/ZXDB/raw/master/ZXDB_mysql.sql.zip";
  /** Which TOSEC dumps are which entry. ZXDB does not say; ZXInfo keeps the list it answers with. */
  private static final String TOSEC =
      "https://raw.githubusercontent.com/thomasheckmann/zxinfo-db/master/ZXInfoExt/TOSEC/tosec_2023.txt";
  private static final int MOST_AT_ONCE = 150, SHIPPED_GAMES = 5000;

  /** The tables kept per entry, which are what the part that ships is cut down by. */
  private static final List<String> BY_ENTRY =
      List.of("releases", "downloads", "publishers", "authors", "aliases", "scores", "tosec");
  /** The tables that give an id its name, kept whole. */
  private static final List<String> NAMES = List.of("machinetypes", "genretypes", "availabletypes",
      "filetypes", "extensions", "sourcetypes", "schemetypes", "currencies", "labeltypes", "countries", "languages");
  /** Every table an answer is read from; nothing else of the dump is kept. */
  private static final List<String> READ =
      Stream.of(List.of("entries", "labels"), BY_ENTRY, NAMES).flatMap(List::stream).toList();

  private static final Pattern TABLE = Pattern.compile("(?m)^(?:CREATE TABLE IF NOT EXISTS|INSERT INTO) (\\w+)");
  private static final Pattern KEY = Pattern.compile("(?i)\\s*KEY.*");
  private static final Pattern UNSIGNED = Pattern.compile("(?i)int\\s*\\(\\d+\\)\\s+unsigned");
  private static final Pattern UNIQUE = Pattern.compile("\\s*UNIQUE KEY \\w*\\s*\\(");

  /** A genre with the kind of thing it is set apart, "Arcade Game" out of "Arcade Game: Platform". */
  private static final String GENRES =
      "(SELECT id, text, substr(text, 1, instr(text || ':', ':') - 1) AS type FROM genretypes)";

  /**
   * One hit as the web service writes it, built by the database itself so that there is no second
   * shape to keep in step with the first. Two rules are ZXInfo's and nothing in ZXDB says them: the
   * files of a release are the file types 8 to 22 - tape, snapshot, disk, cartridge, what a machine
   * loads - and everything else is an additional download. And a screen is a picture: WoS keeps a
   * GIF beside every .scr under /pub/, where the web service converts them on a host of its own,
   * which is down when it is.
   */
  private static final String ENTRY = """
      WITH downloaded AS NOT MATERIALIZED (
          SELECT d.*, f.text AS type,
            (SELECT x.text FROM extensions x WHERE d.file_link LIKE '%' || x.ext ORDER BY length(x.ext) DESC LIMIT 1) AS format
          FROM downloads d JOIN filetypes f ON f.id = d.filetype_id WHERE d.filetype_id > 0),
        published AS NOT MATERIALIZED (
          SELECT p.entry_id, p.release_seq, p.publisher_seq, json_object('publisherSeq', p.publisher_seq, 'name', b.name,
              'country', c.text, 'labelType', t.text) AS publisher
          FROM publishers p JOIN labels b ON b.id = p.label_id LEFT JOIN countries c ON c.id = b.country_id
            LEFT JOIN labeltypes t ON t.id = b.labeltype_id)
      SELECT json_object('_id', printf('%07d', e.id), '_source', json_object(
          '_id', printf('%07d', e.id),
          'id', printf('%07d', e.id),
          'contentType', CASE g.type WHEN 'Book' THEN 'BOOK' WHEN 'Hardware' THEN 'HARDWARE' ELSE 'SOFTWARE' END,
          'title', e.title,
          'originalYearOfRelease', r.release_year,
          'originalMonthOfRelease', r.release_month,
          'originalDayOfRelease', r.release_day,
          'machineType', m.text,
          'numberOfPlayers', e.max_players,
          'xrated', e.is_xrated,
          'genre', g.text,
          'genreType', g.type,
          'genreSubType', nullif(trim(substr(g.text, length(g.type) + 2)), ''),
          'language', l.text,
          'availability', a.text,
          'score', CASE WHEN s.votes IS NOT NULL THEN json_object('score', s.score, 'votes', s.votes) END,
          'publishers', json((SELECT json_group_array(json(publisher) ORDER BY publisher_seq) FROM published
              WHERE entry_id = e.id AND release_seq = 0)),
          'authors', json((SELECT json_group_array(json_object('authorSeq', w.author_seq, 'name', b.name, 'country', c.text,
                'labelType', t.text, 'groupName', team.name) ORDER BY w.author_seq)
              FROM authors w JOIN labels b ON b.id = w.label_id LEFT JOIN labels team ON team.id = w.team_id
                LEFT JOIN countries c ON c.id = b.country_id LEFT JOIN labeltypes t ON t.id = b.labeltype_id
              WHERE w.entry_id = e.id)),
          'releases', json((SELECT json_group_array(json_object('releaseSeq', v.release_seq, 'yearOfRelease', v.release_year,
                'releasePrice', CASE WHEN v.release_price IS NOT NULL THEN json_object('amount', CAST(v.release_price AS TEXT),
                  'currency', coalesce(k.symbol, v.currency_id), 'prefix', k.prefix) END,
                'code', (SELECT max(file_code) FROM downloads WHERE entry_id = v.entry_id AND release_seq = v.release_seq),
                'publishers', json((SELECT json_group_array(json(publisher) ORDER BY publisher_seq) FROM published
                    WHERE entry_id = v.entry_id AND release_seq = v.release_seq)),
                'files', json((SELECT json_group_array(json_object('path', d.file_link, 'size', d.file_size, 'type', d.type,
                      'format', d.format, 'origin', o.text, 'comments', d.comments, 'encodingScheme', h.text) ORDER BY d.file_link)
                    FROM downloaded d LEFT JOIN sourcetypes o ON o.id = d.sourcetype_id
                      LEFT JOIN schemetypes h ON h.id = d.schemetype_id
                    WHERE d.entry_id = v.entry_id AND d.release_seq = v.release_seq AND d.filetype_id BETWEEN 8 AND 22)))
                ORDER BY v.release_seq)
              FROM releases v LEFT JOIN currencies k ON k.id = v.currency_id WHERE v.entry_id = e.id)),
          'additionalDownloads', json((SELECT json_group_array(json_object('path', d.file_link, 'size', d.file_size,
                'type', d.type, 'format', d.format, 'language', n.text) ORDER BY d.release_seq, d.file_link)
              FROM downloaded d LEFT JOIN languages n ON n.id = d.language_id
              WHERE d.entry_id = e.id AND d.filetype_id NOT BETWEEN 8 AND 22)),
          'screens', json((SELECT json_group_array(json_object('type', d.type, 'url', CASE WHEN d.file_link LIKE '%.scr'
                THEN replace(substr(d.file_link, 1, length(d.file_link) - 4), '/scr/', '/gif/') || '.gif' ELSE d.file_link END)
                ORDER BY d.release_seq, d.filetype_id)
              FROM downloaded d WHERE d.entry_id = e.id AND d.filetype_id IN (1, 2)
                AND (d.file_link LIKE '/pub/%.scr' OR d.file_link LIKE '%.gif' OR d.file_link LIKE '%.png' OR d.file_link LIKE '%.jpg'))),
          'tosec', json((SELECT json_group_array(json_object('path', path) ORDER BY path) FROM tosec WHERE entry_id = e.id))))
      FROM entries e
        LEFT JOIN releases r ON r.entry_id = e.id AND r.release_seq = 0
        LEFT JOIN machinetypes m ON m.id = e.machinetype_id
        LEFT JOIN availabletypes a ON a.id = e.availabletype_id
        LEFT JOIN languages l ON l.id = e.language_id
        LEFT JOIN scores s ON s.entry_id = e.id
        LEFT JOIN\s""" + GENRES + " g ON g.id = e.genretype_id";

  /** The values the filters accept, counted the way the web service counts them: entries per value. */
  private static final String METADATA = "SELECT json_object('machinetypes', " + facet("machinetype",
      "SELECT m.text AS value, count(*) AS n FROM entries e JOIN machinetypes m ON m.id = e.machinetype_id GROUP BY 1")
      + ", 'genretypes', " + facet("genretype",
      "SELECT g.type AS value, count(*) AS n FROM entries e JOIN " + GENRES + " g ON g.id = e.genretype_id GROUP BY 1")
      + ")";

  private static final ObjectMapper MAPPER = new ObjectMapper();

  private static Connection connection;

  @Override
  public String where() {
    return WHOLE.exists() ? "ZXDB in " + WHOLE : "the ZXDB of the most voted games in this build";
  }

  /**
   * Every entry that has all the words of the query in its title or in another name it went by.
   * All the words rather than the whole phrase, because what somebody types is the name as they
   * remember it: "everyone wally" is how one asks for "Everyone's a Wally". A title that begins with
   * what was asked comes first, and then the games more people voted for, which is what this has
   * instead of the web service's scoring.
   */
  @Override
  public List<Hit> search(String query, String machineType, String genreType) {
    String asked = query == null ? "" : query.trim();
    List<String> words = asked.isEmpty() ? List.of() : List.of(asked.split("\\s+"));
    List<Object> parameters = new ArrayList<>(Arrays.asList(machineType, machineType, genreType, genreType));
    words.forEach(word -> parameters.add("%" + word + "%"));
    parameters.add(asked + "%");
    String named = words.isEmpty() ? "" : " AND e.id IN (SELECT id FROM (SELECT id, title FROM entries"
        + " UNION ALL SELECT entry_id, title FROM aliases) WHERE "
        + words.stream().map(word -> "title LIKE ?").collect(Collectors.joining(" AND ")) + ")";
    return answers(Hit.class, ENTRY + " WHERE (? IS NULL OR m.text = ?) AND (? IS NULL OR g.type = ?)" + named
        + " ORDER BY e.title LIKE ? DESC, s.votes DESC, e.title LIMIT " + MOST_AT_ONCE, parameters.toArray());
  }

  @Override
  public GameEntry game(String id) {
    return answers(Hit.class, ENTRY + " WHERE e.id = CAST(? AS INTEGER)", id).stream()
        .findFirst().map(Hit::getSource).orElse(null);
  }

  @Override
  public Metadata metadata() {
    return answers(Metadata.class, METADATA).get(0);
  }

  private static String facet(String parameter, String counted) {
    return "json_object('parameter', '" + parameter + "', 'values', json((SELECT json_group_array("
        + "json_object('value', value, 'doc_count', n)) FROM (" + counted + "))))";
  }

  /** Each row of a query read as the JSON document it is, into the classes the web service's answers go into. */
  private static synchronized <T> List<T> answers(Class<T> as, String sql, Object... parameters) {
    try (PreparedStatement query = connection().prepareStatement(sql)) {
      for (int i = 0; i < parameters.length; i++) {
        query.setObject(i + 1, parameters[i]);
      }
      List<T> found = new ArrayList<>();
      try (ResultSet rows = query.executeQuery()) {
        while (rows.next()) {
          found.add(MAPPER.readValue(rows.getString(1), as));
        }
      }
      return found;
    } catch (SQLException | IOException unreadable) {
      throw new IllegalStateException(unreadable);
    }
  }

  /** The whole database when it has been brought down; the part that ships, read into memory, until then. */
  private static Connection connection() throws SQLException, IOException {
    if (connection == null) {
      boolean whole = WHOLE.exists();
      connection = DriverManager.getConnection("jdbc:sqlite:" + (whole ? WHOLE : ":memory:"));
      if (!whole) {
        try (InputStream shipped = Zxdb.class.getResourceAsStream(SHIPPED)) {
          connection.unwrap(SQLiteConnection.class).deserialize("main", shipped.readAllBytes());
        }
      }
    }
    return connection;
  }

  /**
   * Brings the whole of ZXDB down into ~/.oozx, which answers from then on in place of the part that
   * ships. Given a folder, writes the part that ships there as well: the most voted games of what came
   * down, five thousand unless a second argument says otherwise.
   */
  public static void main(String[] args) throws IOException, SQLException {
    File building = new File(WHOLE.getPath() + ".building");
    WHOLE.getParentFile().mkdirs();
    bringDown(building);
    Files.move(building.toPath(), WHOLE.toPath(), StandardCopyOption.REPLACE_EXISTING);
    if (args.length > 0) {
      keepTheMostVoted(args.length > 1 ? Integer.parseInt(args[1]) : SHIPPED_GAMES, new File(args[0], "zxdb.sqlite"));
    }
  }

  /**
   * ZXDB as its maintainers publish it, a MySQL script, read into SQLite a statement at a time and
   * only for the tables an answer is read from.
   */
  private static void bringDown(File into) throws IOException, SQLException {
    Files.deleteIfExists(into.toPath());
    try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + into); Statement sql = db.createStatement();
         ZipInputStream dump = new ZipInputStream(new ByteArrayInputStream(DownloadAndUnzip.downloadFile(new URL(DUMP))))) {
      db.setAutoCommit(false);
      dump.getNextEntry();
      BufferedReader lines = new BufferedReader(new InputStreamReader(dump, StandardCharsets.UTF_8));
      StringBuilder statement = new StringBuilder();
      for (String line = lines.readLine(); line != null; line = lines.readLine()) {
        statement.append(sqlite(line)).append('\n');
        if (line.endsWith(";")) {
          Matcher table = TABLE.matcher(statement);
          if (table.find() && READ.contains(table.group(1))) {
            sql.executeUpdate(statement.toString());
          }
          statement.setLength(0);
        }
      }
      sql.executeUpdate("CREATE TABLE tosec (entry_id INTEGER NOT NULL, path TEXT NOT NULL)");
      try (PreparedStatement insert = db.prepareStatement("INSERT INTO tosec VALUES (?, ?)")) {
        for (String line : new String(DownloadAndUnzip.downloadFile(new URL(TOSEC)), StandardCharsets.UTF_8).split("\r?\n")) {
          String[] idAndPath = line.split("\t");
          // The list names a game's pokes among its dumps, and the web service leaves them out.
          if (idAndPath.length == 2 && !idAndPath[1].toLowerCase(Locale.ROOT).endsWith(".pok")) {
            insert.setInt(1, Integer.parseInt(idAndPath[0]));
            insert.setString(2, archived(idAndPath[1]));
            insert.addBatch();
          }
        }
        insert.executeBatch();
      }
      indexed(sql);
      db.commit();
    }
  }

  /** One line of the MySQL script as SQLite reads it: the changes ZXDB's own ZXDB_to_SQLite.py makes. */
  private static String sqlite(String mysql) {
    if (mysql.startsWith(") ENGINE")) {
      return ");";
    }
    if (KEY.matcher(mysql).matches()) {
      return "";
    }
    String line = mysql.replace("`", "").replace("\\'", "''").replace("\\\\", "\\")
        .replace("utf8_bin", "rtrim").replace("utf8_unicode_ci", "rtrim")
        .replace("CHARACTER SET utf8", "").replace("AUTO_INCREMENT", "");
    return UNIQUE.matcher(UNSIGNED.matcher(line).replaceAll("INTEGER")).replaceAll("UNIQUE (");
  }

  /**
   * A TOSEC path the way archive.org serves the set and ZXInfo hands it out: inside a folder named
   * after the title, less any final full stop, which a folder cannot end in.
   */
  private static String archived(String path) {
    int name = path.lastIndexOf('/') + 1;
    String title = path.substring(name).split(" \\(")[0].replaceAll("\\.+$", "");
    return path.substring(0, name) + title + '/' + path.substring(name);
  }

  /** The most voted games of the whole database, as the part that ships with the build. */
  private static void keepTheMostVoted(int games, File shipped) throws IOException, SQLException {
    Files.deleteIfExists(shipped.toPath());
    try (Connection db = DriverManager.getConnection("jdbc:sqlite:" + shipped); Statement sql = db.createStatement()) {
      sql.executeUpdate("ATTACH '" + WHOLE + "' AS whole");
      sql.executeUpdate("CREATE TABLE entries AS SELECT * FROM whole.entries WHERE id IN"
          + " (SELECT entry_id FROM whole.scores ORDER BY votes DESC, entry_id LIMIT " + games + ")");
      for (String table : BY_ENTRY) {
        sql.executeUpdate("CREATE TABLE " + table + " AS SELECT * FROM whole." + table
            + " WHERE entry_id IN (SELECT id FROM entries)");
      }
      sql.executeUpdate("CREATE TABLE labels AS SELECT * FROM whole.labels WHERE id IN"
          + " (SELECT label_id FROM publishers UNION SELECT label_id FROM authors UNION SELECT team_id FROM authors)");
      for (String table : NAMES) {
        sql.executeUpdate("CREATE TABLE " + table + " AS SELECT * FROM whole." + table);
      }
      indexed(sql);
    }
  }

  /** What an answer looks things up by, which the part that ships has lost with its constraints. */
  private static void indexed(Statement sql) throws SQLException {
    sql.executeUpdate("CREATE INDEX IF NOT EXISTS entries_id ON entries(id)");
    sql.executeUpdate("CREATE INDEX IF NOT EXISTS labels_id ON labels(id)");
    for (String table : BY_ENTRY) {
      sql.executeUpdate("CREATE INDEX IF NOT EXISTS " + table + "_entry ON " + table + "(entry_id)");
    }
  }
}
