package edu.ucsb.cs156.frontiers.services;

import edu.ucsb.cs156.frontiers.entities.RosterStudent;
import edu.ucsb.cs156.frontiers.models.NameAndTeam;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

/**
 * Builds the compact team tables that instructors traditionally produce by hand: every student's
 * name abbreviated as far as it can be while staying unique, laid out in several side-by-side
 * column pairs (e.g. "Name,Team,,Name,Team"). The name abbreviation, the column balancing and the
 * CSV writing are separate steps so that different tables (name to team, team to names) can share
 * them.
 */
@Service
public class TeamCsvService {

  /**
   * Converts a name to Title Case: the first letter of each word is upper case and the rest lower
   * case. A new word starts after any non-letter character, so hyphenated names and names with
   * apostrophes are handled (e.g. "MARY-JANE O'BRIEN" becomes "Mary-Jane O'Brien"). Leading and
   * trailing whitespace is removed; null becomes "".
   *
   * @param name the raw name (may be null)
   * @return the name in Title Case
   */
  public String titleCase(String name) {
    if (name == null) {
      return "";
    }
    String trimmed = name.trim();
    StringBuilder result = new StringBuilder(trimmed.length());
    boolean startOfWord = true;
    for (char c : trimmed.toCharArray()) {
      if (Character.isLetter(c)) {
        result.append(startOfWord ? Character.toUpperCase(c) : Character.toLowerCase(c));
        startOfWord = false;
      } else {
        result.append(c);
        startOfWord = true;
      }
    }
    return result.toString();
  }

  /**
   * Returns the first word of the given name: everything before the first whitespace, once leading
   * and trailing whitespace has been removed. The roster's first name field often holds a first and
   * a middle name ("Chris Edward"); we assume the first space marks the end of the first name. Null
   * becomes "".
   *
   * @param name the raw name (may be null)
   * @return the first word of the name
   */
  public String firstWord(String name) {
    if (name == null) {
      return "";
    }
    return name.trim().split("\\s+", 2)[0];
  }

  /**
   * Computes the abbreviated name and team for each student, preserving the order of the input
   * list. The first name is the first word of the roster's first name field (see {@link
   * #firstWord}), so a middle name in that field is ignored. Each name is abbreviated as far as
   * possible while remaining unique among all students:
   *
   * <ul>
   *   <li>first name only, where no other student shares that first name;
   *   <li>first name plus last initial, where that is enough to be unambiguous;
   *   <li>first name plus full last name, otherwise;
   *   <li>first name plus full last name followed by an asterisk, when another student has exactly
   *       the same first and last name (the instructor can then disambiguate by hand).
   * </ul>
   *
   * The team is the first of the student's teams, or "" if the student has no team.
   *
   * @param students the roster students, already in the desired order
   * @return one NameAndTeam per student, in the same order
   */
  public List<NameAndTeam> nameAndTeams(List<RosterStudent> students) {
    List<String> firsts = new ArrayList<>();
    List<String> withInitials = new ArrayList<>();
    List<String> fulls = new ArrayList<>();
    for (RosterStudent student : students) {
      String first = titleCase(firstWord(student.getFirstName()));
      String last = titleCase(student.getLastName());
      firsts.add(first);
      withInitials.add(join(first, last.isEmpty() ? "" : last.substring(0, 1)));
      fulls.add(join(first, last));
    }

    Map<String, Integer> firstCounts = counts(firsts);
    Map<String, Integer> withInitialCounts = counts(withInitials);
    Map<String, Integer> fullCounts = counts(fulls);

    List<NameAndTeam> result = new ArrayList<>();
    for (int i = 0; i < students.size(); i++) {
      String name;
      if (firstCounts.get(firsts.get(i)) == 1) {
        name = firsts.get(i);
      } else if (withInitialCounts.get(withInitials.get(i)) == 1) {
        name = withInitials.get(i);
      } else if (fullCounts.get(fulls.get(i)) == 1) {
        name = fulls.get(i);
      } else {
        name = fulls.get(i) + "*";
      }
      List<String> teams = students.get(i).getTeams();
      String team = teams.isEmpty() ? "" : teams.get(0);
      result.add(new NameAndTeam(name, team));
    }
    return result;
  }

  /**
   * Splits the items into the given number of columns, filling column by column, with the column
   * lengths as balanced as possible: every column gets items.size() / columns items, and the first
   * items.size() % columns columns get one extra.
   *
   * @param <T> the type of item being laid out (e.g. a student, or a whole team)
   * @param items the items, in order
   * @param columns the number of columns, at least 1
   * @return a list of exactly {@code columns} lists, the first of which is never shorter than any
   *     other
   * @throws IllegalArgumentException if columns is less than 1
   */
  public <T> List<List<T>> layoutColumns(List<T> items, int columns) {
    if (columns < 1) {
      throw new IllegalArgumentException("columns must be at least 1");
    }
    int base = items.size() / columns;
    int extra = items.size() % columns;
    List<List<T>> result = new ArrayList<>();
    int start = 0;
    for (int c = 0; c < columns; c++) {
      int size = base + (c < extra ? 1 : 0);
      result.add(items.subList(start, start + size));
      start += size;
    }
    return result;
  }

  /**
   * Writes a CSV made of several side-by-side columns separated by a blank cell. The header line
   * repeats {@code headerCells} once per column; then there is one line per row of the longest
   * column. Each column is a list of rows, and each row a list of cells with the same length as
   * {@code headerCells}. Rows missing from shorter columns are written as blank cells.
   *
   * @param writer where to write the CSV
   * @param headerCells the cells of one column's header, e.g. ["Name", "Team"]
   * @param columns the columns, each a list of rows of cells
   * @throws IOException if writing fails
   */
  public void writeColumnsCsv(
      Writer writer, List<String> headerCells, List<List<List<String>>> columns)
      throws IOException {
    int width = headerCells.size();
    List<String> blanks = new ArrayList<>();
    for (int i = 0; i < width; i++) {
      blanks.add("");
    }
    int rows = columns.stream().mapToInt(List::size).max().orElse(0);

    // try (CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT)) {
    CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT);
    List<String> header = new ArrayList<>();
    for (int c = 0; c < columns.size(); c++) {
      if (c > 0) {
        header.add("");
      }
      header.addAll(headerCells);
    }
    csvPrinter.printRecord(header);

    for (int r = 0; r < rows; r++) {
      List<String> row = new ArrayList<>();
      for (int c = 0; c < columns.size(); c++) {
        if (c > 0) {
          row.add("");
        }
        List<List<String>> column = columns.get(c);
        row.addAll(r < column.size() ? column.get(r) : blanks);
      }
      csvPrinter.printRecord(row);
    }
    // }
    // csvPrinter.flush();
  }

  /**
   * Writes the "Name,Team" table: the entries balanced across the given number of column pairs,
   * filling column by column.
   *
   * @param writer where to write the CSV
   * @param entries the entries, in order
   * @param columns the number of "Name,Team" column pairs, at least 1
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if columns is less than 1
   */
  public void writeName2TeamCsv(Writer writer, List<NameAndTeam> entries, int columns)
      throws IOException {
    List<List<List<String>>> layout = new ArrayList<>();
    for (List<NameAndTeam> column : layoutColumns(entries, columns)) {
      layout.add(column.stream().map(e -> List.of(e.name(), e.team())).toList());
    }
    writeColumnsCsv(writer, List.of("Name", "Team"), layout);
  }

  /**
   * Groups the entries by team, one list per team, with the teams in case-insensitive alphabetical
   * order by name and the entries within each team in their original order. Entries with no team (a
   * blank team name) are collected into one extra group placed last, so that unassigned students
   * are still visible.
   *
   * @param entries the entries, in order
   * @return one list per team, teams sorted by name, unassigned students last
   */
  public List<List<NameAndTeam>> groupByTeam(List<NameAndTeam> entries) {
    Map<String, List<NameAndTeam>> byTeam =
        new TreeMap<>(String.CASE_INSENSITIVE_ORDER.thenComparing(Comparator.naturalOrder()));
    List<NameAndTeam> noTeam = new ArrayList<>();
    for (NameAndTeam entry : entries) {
      if (entry.team().isEmpty()) {
        noTeam.add(entry);
      } else {
        byTeam.computeIfAbsent(entry.team(), k -> new ArrayList<>()).add(entry);
      }
    }
    List<List<NameAndTeam>> result = new ArrayList<>(byTeam.values());
    if (!noTeam.isEmpty()) {
      result.add(noTeam);
    }
    return result;
  }

  /**
   * Writes the "Team,Name" table: the teams (see {@link #groupByTeam}) balanced across the given
   * number of column pairs, filling column by column, with one row per student. Columns are
   * balanced by number of teams, not number of students, so that a team is never split across two
   * columns.
   *
   * @param writer where to write the CSV
   * @param entries the entries, in order
   * @param columns the number of "Team,Name" column pairs, at least 1
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if columns is less than 1
   */
  public void writeTeamTableCsv(Writer writer, List<NameAndTeam> entries, int columns)
      throws IOException {
    List<List<List<String>>> layout = new ArrayList<>();
    for (List<List<NameAndTeam>> column : layoutColumns(groupByTeam(entries), columns)) {
      List<List<String>> rows = new ArrayList<>();
      for (List<NameAndTeam> team : column) {
        for (NameAndTeam entry : team) {
          rows.add(List.of(entry.team(), entry.name()));
        }
      }
      layout.add(rows);
    }
    writeColumnsCsv(writer, List.of("Team", "Name"), layout);
  }

  // THE ONE IM WORKING ON DESPITE BEING IDENTICAL TO WRITETEAMTEABLECSV
  /**
   * Writes the combined out put of name2team and teamtable
   *
   * @param writer where to write the CSV
   * @param entries the entries, in order
   * @param columns not actually used but the way the backend calls it it's easier to leave it here
   * @throws IOException if writing fails
   * @throws IllegalArgumentException if columns is less than 1
   */
  public void writeFormattedTeamsCsv(Writer writer, List<NameAndTeam> entries, int columns)
      throws IOException {
    final int num_rows = 24;
    int name_columns = (int) Math.ceil(entries.size() / (float) num_rows);

    writer.write("by name" + ",".repeat(name_columns - 1) + "\r\n");
    // altered layoutColumns that doesn't fill
    List<List<NameAndTeam>> name_column_list = new ArrayList<>();
    int start = 0;
    for (int c = 0; c < name_columns; c++) {
      int size =
          Math.min(
              entries.size() - start,
              num_rows); // should just do num_rows every time until the last column
      name_column_list.add(entries.subList(start, start + size));
      start += size;
    }
    // altered writeName2TeamCsv where it doesn't fill
    List<List<List<String>>> layout = new ArrayList<>();
    for (List<NameAndTeam> column : name_column_list) {
      layout.add(column.stream().map(e -> List.of(e.name(), e.team())).toList());
    }
    writeColumnsCsv(writer, List.of("Name", "Team"), layout);

    // I don't like calling groupByTeam twice but I think this is the best way without altering
    // everything else
    final int teams_per_column = 4;
    int num_teams = groupByTeam(entries).size();
    int team_columns = (int) Math.ceil(num_teams / (float) teams_per_column);
    writer.write("by team" + ",".repeat(team_columns - 1) + "\r\n");

    writeTeamTableCsv(writer, entries, team_columns);
    // writer.write("name_columns: " + name_columns + "\nentries.size(): " + entries.size() + "\n");
  }

  private static String join(String first, String lastPart) {
    if (first.isEmpty()) {
      return lastPart;
    }
    if (lastPart.isEmpty()) {
      return first;
    }
    return first + " " + lastPart;
  }

  private static Map<String, Integer> counts(List<String> values) {
    Map<String, Integer> result = new HashMap<>();
    for (String value : values) {
      result.merge(value, 1, Integer::sum);
    }
    return result;
  }
}
