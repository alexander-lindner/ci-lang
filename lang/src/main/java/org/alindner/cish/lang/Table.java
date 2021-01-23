package org.alindner.cish.lang;

import com.inamik.text.tables.GridTable;

public class Table {

	public static GridTable of(int rows, int cols) {
	//	Lib.addImport("com.inamik.text.tables.Cell"," com.inamik.text.tables.line.RightAlign");
		return GridTable.of(rows, cols);
	}

}
