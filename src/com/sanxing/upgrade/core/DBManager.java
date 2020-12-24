package com.sanxing.upgrade.core;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class DBManager {
	private static Connection conn = null;

	private static final String driver = "org.apache.derby.jdbc.EmbeddedDriver";

	private static final String url = "jdbc:derby:";

	private static final String dbName = "db";

	private static final String SQL_CREATE_TBL_TASK = "CREATE TABLE UPGRADETOOLS.TASK (TerminalAddr VARCHAR(12) NOT NULL,OldVersion VARCHAR(16),CurrentVersion VARCHAR(16),State INTEGER,RcvRate DECIMAL(3, 2),FileSign VARCHAR(100),Remark VARCHAR(500),LastTime TIME,CONSTRAINT SQL070512054941980 PRIMARY KEY (TerminalAddr))";

	private static final String SQL_CREATE_IDX_TASK = "CREATE UNIQUE INDEX SQL070512054941980 ON UPGRADETOOLS.TASK(TerminalAddr)";

	private static final String SQL_CREATE_TBL_EVENT = "CREATE TABLE UPGRADETOOLS.EVENT (TerminalAddr VARCHAR(12) NOT NULL,Time TIME,Type INT,Remark VARCHAR(500))";

	private static final String SQL_CREATE_IDX_EVENT = "CREATE INDEX SQL070512054941981 ON UPGRADETOOLS.EVENT(TerminalAddr)";

	public static void initialize() throws Exception {
		Class.forName(driver);
		boolean isExists = false;
		try {
			conn = DriverManager.getConnection(url + dbName); // "jdbc:derby:db"
			isExists = true;
		} catch (Exception exception) {
		}

		if (!isExists) {
			conn = DriverManager.getConnection(url + dbName + ";create=true"); // "jdbc:derby:db;create=true"

			Statement stmt = getStmt();

			try {
				stmt.execute(SQL_CREATE_TBL_TASK);
				stmt.execute(SQL_CREATE_IDX_TASK);

				stmt.execute(SQL_CREATE_TBL_EVENT);
				stmt.execute(SQL_CREATE_IDX_EVENT);
			} finally {
				stmt.close();
			}
		}
	}

	public static Statement getStmt() throws SQLException {
		return conn.createStatement();
	}

	public static PreparedStatement getPrepStmt(String sql) throws SQLException {
		return conn.prepareStatement(sql);
	}
}
