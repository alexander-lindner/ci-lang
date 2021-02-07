package org.alindner.cish.lang;

import com.inamik.text.tables.Cell;
import lombok.Builder;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.inamik.text.tables.Cell.Functions.HORIZONTAL_CENTER;
import static com.inamik.text.tables.Cell.Functions.VERTICAL_CENTER;

public class FancyTable {
	private final List<List<String>> data   = new ArrayList<>();
	private final int                rows;
	private final boolean            growing;
	private final List<List<Option>> options;
	private       int                cols;
	private       boolean            header = false;
	private       Chars              border = Chars.DOUBLE_LINE;

	public FancyTable(final int rows, final int cols) {
		this.growing = false;
		this.rows = rows;
		this.cols = cols;
		this.options = IntStream.range(0, rows)
		                        .mapToObj(
				                        i -> IntStream.range(0, cols).mapToObj(i2 -> FancyTable.Option.standard()).collect(Collectors.toList())
		                        )
		                        .collect(Collectors.toList());
	}

	public FancyTable() {
		this.growing = true;
		this.rows = 0;
		this.cols = 0;
		this.options = new ArrayList<>();
	}

	public static void main(final String... args) {
		final FancyTable table2 = new FancyTable();
		table2.add(1, 2, 3);
		table2.add(4, 5, 6, 7);
		table2.add(7, 8, 9);
		table2.add(7, 8, 9);
		table2.add(7, 8, 9);
		table2.add(7, 8, 9);
		table2.add(7, 8, 90000000);
		table2.add(7, 8, 9);
		table2.add(7, 8, 9);
		table2.add("1000000000000000");
		table2.withFirstLineAsHeader();
		table2.setBorder(Chars.SINGLE_LINE);
		table2.renderTheTable();
//		System.out.println(table2.render());
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

	private FancyTable add(final Integer... cols) {
		return this.add(Arrays.stream(cols).map(String::valueOf).collect(Collectors.toList()));
	}

	private FancyTable add(final List<String> cols) {
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

	private FancyTable add(final String... cols) {
		return this.add(new ArrayList<>(List.of(cols)));
	}

	private FancyTable withFirstLineAsHeader() {
		this.header = true;
		return this;
	}

	public FancyTable setBorder(final Chars border) {
		this.border = border;
		return this;
	}

	void renderTheTable() {
		this.fixSizes();
		new Generator(this.data, this.cols, this.border).generate();
	}

	static class Generator {
		final         List<List<String>> table          = new ArrayList<>();
		final         List<Integer>      maxSizesPerCol = new ArrayList<>();
		private final List<List<String>> data;
		private final int                cols;
		private final Chars              border;
		List<List<Integer>> lengths;
		boolean             heading = true;
		boolean             footer  = true;

		public Generator(final List<List<String>> data, final int cols, final Chars border) {
			this.data = data;
			this.cols = cols;
			this.border = border;
			this.lengths = data.stream().map(strings -> strings.stream().map(String::length).collect(Collectors.toList())).collect(Collectors.toList());
		}

		void generateColsSizes() {
			for (final List<Integer> row : this.lengths) {
				for (int j = 0; j < row.size(); j++) {
					final Integer col = row.get(j);
					if (this.maxSizesPerCol.size() - 1 < j) {
						this.maxSizesPerCol.add(col);
					} else {
						if (this.maxSizesPerCol.get(j) < col) {
							this.maxSizesPerCol.set(j, col);
						}
					}
				}
			}
		}

		public void generate() {
			this.generateColsSizes();


			final List<List<String>> lists = this.data;
			for (int i = 0; i < lists.size(); i++) {
				final List<String> strings = lists.get(i);
				this.addSeparator(i);
				final List<String> tmpList = new ArrayList<>();

				for (int j = 0; j < strings.size(); j++) {
					final String s = strings.get(j);
					tmpList.add("|");
					tmpList.add(this.addPadding(s, j));
				}
				tmpList.add("|");
				this.table.add(tmpList);
			}
			this.addSeparator(lists.size());
			this.table.forEach(row -> {
				row.forEach(System.out::print);
				System.out.print("\n");
			});
		}

		List<String> getHorizontalSeparator(final char character) {
			return this.maxSizesPerCol.stream().map(length -> new String(new char[length]).replace("\0", String.valueOf(character))).collect(Collectors.toList());
		}

		private void addSeparator(final int currentRow) {
			final char separatorHorizontal;
			final char separatorVertical;
			if (((currentRow == 0 || currentRow == 1) && this.heading) || ((currentRow == this.data.size() || currentRow == this.data.size() - 1) && this.footer)) {
				separatorHorizontal = this.border.headerFooterHorizontal;
				separatorVertical = this.border.headerFooterVertical;
			} else {
				separatorHorizontal = this.border.horizontal;
				separatorVertical = this.border.vertical;
			}
			final List<String> separatorList = new ArrayList<>();
			for (int i = 0; i < this.cols; i++) {
				separatorList.add(String.valueOf(separatorVertical));
				separatorList.add(this.getHorizontalSeparator(separatorHorizontal).get(i));
			}
			separatorList.add(String.valueOf(separatorVertical));
			this.table.add(separatorList);
		}

		private String addPadding(final String s, final int index) {
			if (s.length() < this.maxSizesPerCol.get(index)) {
				final int size = this.maxSizesPerCol.get(index) - s.length();
				return s + new String(new char[size]).replace("\0", " ");
			}
			return s;
		}
	}

	static class Chars {
		public static final Chars DOUBLE_LINE = new Chars('╬', '═', '║', '╔', '╦', '╗', '╠', '╣', '╚', '╩', '╝', '-', '-');
		public static final Chars SINGLE_LINE = new Chars('┼', '─', '│', '┌', '┬', '┐', '├', '┤', '└', '┴', '┘', '=', '=');

		public final  char intersect;
		public final  char horizontal;
		public final  char vertical;
		public final  char topLeft;
		public final  char topIntersect;
		public final  char topRight;
		public final  char LeftIntersect;
		public final  char RightIntersect;
		public final  char bottomLeft;
		public final  char bottomIntersect;
		public final  char bottomRight;
		private final char headerFooterHorizontal;
		private final char headerFooterVertical;

		public Chars(final char i, final char h, final char v, final char tl, final char ti, final char tr, final char li, final char ri, final char bl, final char bi, final char br, final char hfh, final char hfv) {
			this.intersect = i;
			this.horizontal = h;
			this.vertical = v;
			this.topLeft = tl;
			this.topIntersect = ti;
			this.topRight = tr;
			this.LeftIntersect = li;
			this.RightIntersect = ri;
			this.bottomLeft = bl;
			this.bottomIntersect = bi;
			this.bottomRight = br;
			this.headerFooterHorizontal = hfh;
			this.headerFooterVertical = hfv;
		}

		// *
		public static Chars of(final char intersect) {
			return new Chars(intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect);
		}

		// +-|
		public static Chars of(final char intersect, final char horizontal, final char vertical) {
			return new Chars(intersect, horizontal, vertical, intersect, intersect, intersect, intersect, intersect, intersect, intersect, intersect, horizontal, vertical);
		}

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
}
