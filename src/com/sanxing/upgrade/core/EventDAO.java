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
		PreparedStatement pstmt = DBManager.getPrepStmt(SQL_INSERT);
		try {
			for (int i = 0; i < events.length; i++) {
				Object event = events[i];
				pstmt.setString(1, terminalAddr);
				pstmt.setTime(2, new Time(((Event) event).time().getTime()));
				pstmt.setInt(3, ((Event) event).type().ordinal());
				pstmt.setString(4, ((Event) event).toString());
				pstmt.addBatch();
			}

			pstmt.executeBatch();
		} finally {
			pstmt.close();
		}
	}

	public static void load(String terminalAddr, EventList list) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt(SQL_LOAD);
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
		PreparedStatement pstmt = DBManager.getPrepStmt(SQL_DELETE);
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
			stmt.execute(SQL_CLEAR);
		} finally {
			stmt.close();
		}
	}
}
