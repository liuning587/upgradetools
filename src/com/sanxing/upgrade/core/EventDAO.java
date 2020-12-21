package com.sanxing.upgrade.core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Date;

public class EventDAO {
	private static final String SQL_INSERT = "INSERT INTO UPGRADETOOLS.EVENT (TerminalAddr, Time, Type, Remark) VALUES (?, ?, ?, ?)";
	private static final String SQL_LOAD = "SELECT * FROM UPGRADETOOLS.EVENT WHERE TerminalAddr = ?";
	private static final String SQL_DELETE = "DELETE FROM UPGRADETOOLS.EVENT WHERE TerminalAddr = ?";
	private static final String SQL_CLEAR = "DELETE FROM UPGRADETOOLS.EVENT";

	public static void insert(String terminalAddr, Object[] events) throws SQLException {
		PreparedStatement pstmt = DBManager
				.getPrepStmt("INSERT INTO UPGRADETOOLS.EVENT (TerminalAddr, Time, Type, Remark) VALUES (?, ?, ?, ?)");
		try {
			byte b;
			int i;
			Object[] arrayOfObject;
			for (i = (arrayOfObject = events).length, b = 0; b < i;) {
				Object event = arrayOfObject[b];
				pstmt.setString(1, terminalAddr);
				pstmt.setTime(2, new Time(((Event) event).time().getTime()));
				pstmt.setInt(3, ((Event) event).type().ordinal());
				pstmt.setString(4, ((Event) event).toString());
				pstmt.addBatch();
				b++;
			}

			pstmt.executeBatch();
		} finally {
			pstmt.close();
		}
	}

	public static void load(String terminalAddr, EventList list) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt("SELECT * FROM UPGRADETOOLS.EVENT WHERE TerminalAddr = ?");
		try {
			pstmt.setString(1, terminalAddr);
			ResultSet rs = pstmt.executeQuery();
			while (rs.next()) {
				Event event = Event.create(EventType.values()[rs.getInt("Type")], rs.getString("Remark"));
				event.setTime(new Date(rs.getTime("Time").getTime()));
				list.add(event);
			}
		} finally {
			pstmt.close();
		}
	}

	public static void delete(String terminalAddr) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt("DELETE FROM UPGRADETOOLS.EVENT WHERE TerminalAddr = ?");
		try {
			pstmt.setString(1, terminalAddr);
			pstmt.execute();
		} finally {
			pstmt.close();
		}
	}

	public static void clear() throws SQLException {
		Statement stmt = DBManager.getStmt();
		try {
			stmt.execute("DELETE FROM UPGRADETOOLS.EVENT");
		} finally {
			stmt.close();
		}
	}
}
