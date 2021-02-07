package org.alindner.cish.lang;

import com.inamik.text.tables.Cell;
import com.inamik.text.tables.GridTable;
import com.inamik.text.tables.SimpleTable;
import com.inamik.text.tables.grid.Border;
import com.inamik.text.tables.grid.Util;
import lombok.Builder;
import lombok.Data;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.inamik.text.tables.Cell.Functions.HORIZONTAL_CENTER;
import static com.inamik.text.tables.Cell.Functions.VERTICAL_CENTER;

public class Table {
	private final List<List<String>> data   = new ArrayList<>();
	private final int                rows;
	private final boolean            growing;
	private final List<List<Option>> options;
	private       int                cols;
	private       boolean            header = false;
	private       Border             border = Borders.doubleLine();

	public Table(final int rows, final int cols) {
		this.growing = false;
		this.rows = rows;
		this.cols = cols;
		this.options = IntStream.range(0, rows)
		                        .mapToObj(
				                        i -> IntStream.range(0, cols).mapToObj(i2 -> Table.Option.standard()).collect(Collectors.toList())
		                        )
		                        .collect(Collectors.toList());
	}

	public Table() {
		this.growing = true;
		this.rows = 0;
		this.cols = 0;
		this.options = new ArrayList<>();
	}

	public static void main(final String... args) {
		final Table table = new Table(3, 3);
		table.add(1, 2, 3);
		table.add(4, 5, 6);
		table.add(7, 8, 9);
		table.withFirstLineAsHeader();
		Console.print(table);
		System.out.println(table.render());


		final Table table2 = new Table();
		table2.add(1, 2, 3);
		table2.add(4, 5, 6, 7);
		table2.add(7, 8, 9);
		table2.add("1000000000000000");
		table2.withFirstLineAsHeader();
		table2.setBorder(Borders.simple());
		System.out.println(table2.render());
	}

	public String render() {
		if (this.data.size() != this.rows && !this.growing) {
			Log.error("to few rows", new TableException("Rows size mismatch"));
		}

		this.fixSizes();
		final List<List<String>> currentData = new ArrayList<>(this.data);
		if (this.header) {
			currentData.add(1, IntStream.range(0, this.cols).mapToObj(operand -> "").collect(Collectors.toList()));
			this.options.add(1, IntStream.range(0, this.cols).mapToObj(operand -> Option.builder().build()).collect(Collectors.toList()));
		}
		SimpleTable s = SimpleTable.of();
		for (int i = 0; i < currentData.size(); i++) {
			s = s.nextRow();
			final List<String> rows        = currentData.get(i);
			final List<Option> optionsRows = this.options.get(i);
			for (int j = 0; j < rows.size(); j++) {
				final String col    = rows.get(j);
				final Option option = optionsRows.get(j);
				s = s.nextCell()
				     .addLine(col)
				     .applyToCell(option.getVertical().withHeight(option.getHeight()))
				     .applyToCell(option.getHorizontal().withWidth(option.getWidth()).withChar(option.getBackground()));
			}
		}


		final GridTable table = this.border.apply(s.toGrid());
		try {
			final ByteArrayOutputStream stream = new ByteArrayOutputStream();
			try (final PrintStream ps = new PrintStream(stream, true, StandardCharsets.UTF_8.name())) {
				Util.print(table, ps);
			}
			return stream.toString(StandardCharsets.UTF_8.name());
		} catch (final UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return "";
	}

	private void fixSizes() {
		if (this.growing) {
			for (final List<String> col : this.data) {
				final int size = this.cols - col.size();
				IntStream.range(0, size).forEach(i -> col.add(""));
			}
			final int rowSize = this.data.size() - this.options.size();
			IntStream.range(0, rowSize).forEach(i -> this.options.add(new ArrayList<>()));

			for (final List<Option> col : this.options) {
				final int size = this.cols - col.size();
				IntStream.range(0, size).forEach(i -> col.add(Option.standard()));
			}
		}
	}

	private Table add(final Integer... cols) {
		return this.add(Arrays.stream(cols).map(String::valueOf).collect(Collectors.toList()));
	}

	private Table add(final List<String> cols) {
		if (!this.growing) {
			if (this.data.size() == this.rows) {
				Log.error("Maximum rows are exceeded", new TableException("Rows size mismatch"));
			}
			if (cols.size() < this.cols) {
				Log.error("Provided collum is to small. ", new TableException("Column size mismatch"));
			}
			if (cols.size() > this.cols) {
				Log.error("Provided collum is to big. ", new TableException("Column size mismatch"));
			}
		}
		if (cols.size() > this.cols) {
			this.cols = cols.size();
		}


		this.data.add(cols);
		return this;
	}

	private Table add(final String... cols) {
		return this.add(new ArrayList<>(List.of(cols)));
	}

	private Table withFirstLineAsHeader() {
		this.header = true;
		return this;
	}

	public Table setBorder(final Border border) {
		this.border = border;
		return this;
	}

	public static class TableException extends Exception {

		private static final long serialVersionUID = 5210815130192208359L;

		public TableException(final String s) {
			super(s);
		}
	}

	@Data
	@Builder
	private static class Option {
		@Builder.Default
		char          background = ' ';
		@Builder.Default
		int           width      = 3;
		@Builder.Default
		int           height     = 3;
		@Builder.Default
		Cell.Function horizontal = HORIZONTAL_CENTER;
		@Builder.Default
		Cell.Function vertical   = VERTICAL_CENTER;

		public static Option standard() {
			return Option.builder().build();
		}
	}

	private static class Borders {
		private static Border singleLine() {return (Border.SINGLE_LINE);}

		private static Border doubleLine() {return (Border.DOUBLE_LINE);}

		private static Border simple()     {return (Border.of(Border.Chars.of('+', '-', '|')));}
	}
}
