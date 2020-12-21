package com.sanxing.upgrade.core;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Time;
import java.util.Date;

public class TaskDAO {
	private static final String SQL_INSERT = "INSERT INTO UPGRADETOOLS.TASK (TerminalAddr, OldVersion, CurrentVersion, State, RcvRate, FileSign, Remark, LastTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
	private static final String SQL_UPDATE = "UPDATE UPGRADETOOLS.TASK Set OldVersion = ?, CurrentVersion = ?, State = ?, RcvRate = ?, FileSign = ?, Remark = ?, LastTime = ? WHERE TerminalAddr = ?";
	private static final String SQL_DELETE = "DELETE FROM UPGRADETOOLS.TASK WHERE TerminalAddr = ?";
	private static final String SQL_CLEAR = "DELETE FROM UPGRADETOOLS.TASK";
	private static final String SQL_LOAD = "SELECT * FROM UPGRADETOOLS.TASK";

	public static void insert(Task task) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt(
				"INSERT INTO UPGRADETOOLS.TASK (TerminalAddr, OldVersion, CurrentVersion, State, RcvRate, FileSign, Remark, LastTime) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
		try {
			pstmt.setString(1, task.getTerminalAddr());
			pstmt.setString(2, task.getOldVersion());
			pstmt.setString(3, task.getCurrentVersion());
			pstmt.setInt(4, task.getState());
			pstmt.setFloat(5, task.getRcvRate());
			pstmt.setString(6, task.getFileSign());
			pstmt.setString(7, task.getRemark());
			if (task.getLastTime() != null) {
				pstmt.setTime(8, new Time(task.getLastTime().getTime()));
			} else {
				pstmt.setTime(8, (Time) null);
			}
			pstmt.execute();
		} finally {
			pstmt.close();
		}
	}

	public static void update(Task task) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt(
				"UPDATE UPGRADETOOLS.TASK Set OldVersion = ?, CurrentVersion = ?, State = ?, RcvRate = ?, FileSign = ?, Remark = ?, LastTime = ? WHERE TerminalAddr = ?");
		try {
			pstmt.setString(1, task.getOldVersion());
			pstmt.setString(2, task.getCurrentVersion());
			pstmt.setInt(3, task.getState());
			pstmt.setFloat(4, task.getRcvRate());
			pstmt.setString(5, task.getFileSign());
			pstmt.setString(6, task.getRemark());
			if (task.getLastTime() != null) {
				pstmt.setTime(7, new Time(task.getLastTime().getTime()));
			} else {
				pstmt.setTime(7, (Time) null);
			}
			pstmt.setString(8, task.getTerminalAddr());

			pstmt.execute();
		} finally {
			pstmt.close();
		}
	}

	public static void delete(Task task) throws SQLException {
		PreparedStatement pstmt = DBManager.getPrepStmt("DELETE FROM UPGRADETOOLS.TASK WHERE TerminalAddr = ?");
		try {
			pstmt.setString(1, task.getTerminalAddr());
			pstmt.execute();
		} finally {
			pstmt.close();
		}
	}

	public static void clear() throws SQLException {
		Statement stmt = DBManager.getStmt();
		try {
			stmt.execute("DELETE FROM UPGRADETOOLS.TASK");
		} finally {
			stmt.close();
		}
	}

	public static void load(TaskList list) throws SQLException {
		Statement stmt = DBManager.getStmt();
		try {
			ResultSet rs = stmt.executeQuery("SELECT * FROM UPGRADETOOLS.TASK");
			while (rs.next()) {
				Task task = new Task(rs.getString("TerminalAddr"));
				task.setOldVersion(rs.getString("OldVersion"));
				task.setCurrentVersion(rs.getString("CurrentVersion"));
				task.addState(rs.getInt("State"));
				task.setRcvRate(rs.getFloat("RcvRate"));
				task.setFileSign(rs.getString("FileSign"));
				task.setRemark(rs.getString("Remark"));
				Time time = rs.getTime("LastTime");
				if (time != null)
					task.setLastTime(new Date(time.getTime()));
				list.put(task);
			}
		} finally {
			stmt.close();
		}
	}
}
