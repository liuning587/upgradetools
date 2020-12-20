package com.sanxing.upgrade.util;

import java.util.Calendar;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SysUtils {
	private static final String MAX_INT = String.valueOf(2147483647);
	private static final String MIN_INT = String.valueOf(-2147483648);

	private static final String REGEX_DATE = "(\\d{4})[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})";

	private static final String REGEX_24TIME = "(0?[\\d]|1[\\d]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9])";

	private static final String REGEX_IP = "(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})";

	private static Pattern patternIsDec;

	private static Pattern patternIsHex;

	private static Pattern patternIsZero;

	private static Pattern patternIsDate;
	private static Pattern patternIs24Time;
	private static Pattern patternIsIP;

	public static boolean isInteger(String value) {
		if (patternIsDec == null)
			patternIsDec = Pattern.compile("([-+]?)0*([\\d]+)");
		Matcher matcher = patternIsDec.matcher(value);
		if (!matcher.matches())
			return false;
		String str = matcher.group(2);

		if (matcher.group(1).compareTo("-") != 0) {
			if (str.length() > MAX_INT.length())
				return false;
			if (str.length() == MAX_INT.length())
				return (str.compareTo(MAX_INT) <= 0);
		} else {
			str = "-" + str;
			if (str.length() > MIN_INT.length())
				return false;
			if (str.length() == MIN_INT.length())
				return (str.compareTo(MIN_INT) <= 0);
		}
		return true;
	}

	public static boolean isHex(String value) {
		if (patternIsHex == null)
			patternIsHex = Pattern.compile("(?i)[\\da-f]+");
		Matcher matcher = patternIsHex.matcher(value);
		return matcher.matches();
	}

	public static boolean isZero(String value) {
		if (patternIsZero == null)
			patternIsZero = Pattern.compile("0+");
		Matcher matcher = patternIsZero.matcher(value);
		return matcher.matches();
	}

	public static boolean isZero(byte[] bytes, int start, int length) {
		for (int i = start; i < start + length; i++) {
			if (bytes[i] != 0)
				return false;
		}
		return true;
	}

	public static boolean isValideDate(int year, int month, int day) {
		int[] days = { 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31 };

		if (1 > year)
			return false;
		if (1 > month || 12 < month)
			return false;
		if (1 > day) {
			return false;
		}
		if ((year % 4 == 0 && year % 100 != 0) || year % 400 == 0)
			days[1] = 29;
		if (days[month - 1] < day)
			return false;
		return true;
	}

	public static boolean isDate(String value) {
		if (patternIsDate == null)
			patternIsDate = Pattern.compile("(\\d{4})[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})");
		Matcher matcher = patternIsDate.matcher(value);
		if (!matcher.matches())
			return false;
		return isValideDate(Integer.valueOf(matcher.group(1)).intValue(), Integer.valueOf(matcher.group(2)).intValue(),
				Integer.valueOf(matcher.group(3)).intValue());
	}

	public static boolean isIP(String value) {
		if (patternIsIP == null)
			patternIsIP = Pattern.compile("(\\d{1,3}).(\\d{1,3}).(\\d{1,3}).(\\d{1,3})");
		Matcher matcher = patternIsIP.matcher(value);
		if (!matcher.matches()) {
			return false;
		}
		if (Integer.valueOf(matcher.group(1)).intValue() == 0 || Integer.valueOf(matcher.group(4)).intValue() == 0)
			return false;
		return true;
	}

	public static Date getDate(String value) {
		if (patternIsDate == null)
			patternIsDate = Pattern.compile("(\\d{4})[-/\\.](\\d{1,2})[-/\\.](\\d{1,2})");
		Matcher matcher = patternIsDate.matcher(value);
		if (!matcher.matches())
			return null;
		Calendar calendar = Calendar.getInstance();
		calendar.set(1, Integer.valueOf(matcher.group(1)).intValue());
		calendar.set(2, Integer.valueOf(matcher.group(2)).intValue() - 1);
		calendar.set(5, Integer.valueOf(matcher.group(3)).intValue());
		return calendar.getTime();
	}

	public static Date getShortDate(String value) {
		return getDate("20" + value);
	}

	public static boolean isShortDate(String value) {
		return isDate("20" + value);
	}

	public static boolean is24Time(String value) {
		if (patternIs24Time == null)
			patternIs24Time = Pattern.compile("(0?[\\d]|1[\\d]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9])");
		Matcher matcher = patternIs24Time.matcher(value);
		return matcher.matches();
	}

	public static Date get24Time(String value) {
		if (patternIs24Time == null)
			patternIs24Time = Pattern.compile("(0?[\\d]|1[\\d]|2[0-3]):([0-5]?[0-9]):([0-5]?[0-9])");
		Matcher matcher = patternIs24Time.matcher(value);
		if (!matcher.matches())
			return null;
		Calendar calendar = Calendar.getInstance();
		calendar.set(11, Integer.valueOf(matcher.group(1)).intValue());
		calendar.set(12, Integer.valueOf(matcher.group(2)).intValue());
		calendar.set(13, Integer.valueOf(matcher.group(3)).intValue());
		return calendar.getTime();
	}

	public static void formatString(StringBuffer source, char fillChar, int length) {
		if (source.length() >= length)
			return;
		for (int i = source.length(); i < length; i++) {
			source.insert(0, fillChar);
		}
	}

	public static String formatString(String source, char fillChar, int length) {
		if (source.length() >= length)
			return source;
		StringBuffer buffer = new StringBuffer(source);
		for (int i = source.length(); i < length; i++)
			buffer.insert(0, fillChar);
		return buffer.toString();
	}

	public static int compareHex(String hex1, String hex2) {
		if (hex1.length() == hex2.length()) {
			return hex1.compareTo(hex2);
		}
		if (hex1.length() > hex2.length()) {
			return hex1.compareTo(formatString(hex2, '0', hex1.length()));
		}
		return formatString(hex1, '0', hex2.length()).compareTo(hex2);
	}

	public static String hexToBin(String hex, int length) {
		String strBin = hexToBin(hex);

		if (strBin.length() > length) {

			if (!isZero(strBin.substring(0, strBin.length() - length)))
				throw new IllegalArgumentException();
			return strBin.substring(strBin.length() - length);
		}
		if (strBin.length() < length) {
			return formatString(strBin, '0', length);
		}
		return strBin;
	}

	public static String hexToBin(String hex) {
		if (hex.isEmpty())
			throw new IllegalArgumentException();
		String hex2 = hex;

		int n = (hex.length() - 1) / 6 + 1;
		int m = hex.length() % 6;
		if (m != 0) {
			hex2 = formatString(hex, '0', n * 6);
		} else {
			hex2 = hex;
		}

		StringBuffer sbValue = new StringBuffer();
		StringBuffer sbTemp = new StringBuffer();

		for (int i = 0; i < n; i++) {

			String strTemp = Integer.toBinaryString(Integer.valueOf(hex2.substring(i * 6, i * 6 + 6), 16).intValue());
			sbTemp.append(strTemp);

			formatString(sbTemp, '0', 24);

			sbValue.append(sbTemp);
			sbTemp.setLength(0);
		}

		if (m != 0) {
			return sbValue.substring((6 - m) * 4);
		}
		return sbValue.toString();
	}

	public static String binToHex(String bin) {
		if (bin.isEmpty())
			throw new IllegalArgumentException();
		String bin2 = bin;
		int n = (bin.length() - 1) / 8 + 1;
		int m = bin.length() % 8;

		if (m != 0) {
			bin2 = formatString(bin, '0', n * 8);
		}
		StringBuffer sbValue = new StringBuffer();
		StringBuffer sbTemp = new StringBuffer();

		for (int i = 0; i < n; i++) {

			String strHex = Integer.toHexString(Integer.valueOf(bin2.substring(i * 8, (i + 1) * 8), 2).intValue())
					.toUpperCase();
			sbTemp.append(strHex);

			formatString(sbTemp, '0', 2);
			sbValue.append(sbTemp);
			sbTemp.setLength(0);
		}
		return sbValue.toString();
	}

	public static String intToBin(int i, int length) {
		return formatString(Integer.toBinaryString(i), '0', length);
	}

	public static String intToHex(int i, int length) {
		return formatString(Integer.toHexString(i).toUpperCase(), '0', length);
	}

	public static String intToStr(int i, int length) {
		return formatString(String.valueOf(i), '0', length);
	}

	public static int intToBcd(int i) {
		return Integer.valueOf(String.valueOf(i), 16).intValue();
	}

	public static int bcdToInt(String str) {
		return Integer.valueOf(str, 10).intValue();
	}

	public static String byteToHex(byte b) {
		String hex = Integer.toHexString(b & 0xFF).toUpperCase();
		if (1 == hex.length())
			hex = "0" + hex;
		return hex;
	}

	public static String bytesToHex(byte[] bytes) {
		StringBuffer buffer = new StringBuffer();
		byte b;
		int i;
		byte[] arrayOfByte;
		for (i = (arrayOfByte = bytes).length, b = 0; b < i;) {
			byte b1 = arrayOfByte[b];
			buffer.append(byteToHex(b1));
			b++;
		}
		return buffer.toString();
	}

	public static String bytesToHex(byte[] bytes, int pos, int length) {
		StringBuffer buffer = new StringBuffer();
		for (int i = 0; i < length; i++)
			buffer.append(byteToHex(bytes[pos + i]));
		return buffer.toString();
	}

	public static byte[] hexToBytes(String hex) {
		if (isOdd(hex.length()))
			throw new IllegalArgumentException();
		byte[] bytes = new byte[hex.length() / 2];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = Integer.valueOf(hex.substring(i * 2, i * 2 + 2), 16).byteValue();
		return bytes;
	}

	public static char[] bytesToChr(byte[] data) {
		char[] chars = new char[data.length];
		for (int i = 0; i < chars.length; i++)
			chars[i] = (char) data[i];
		return chars;
	}

	public static String reverseHex(String hex) {
		if (isOdd(hex.length()))
			throw new IllegalArgumentException();
		StringBuffer buffer = new StringBuffer();
		for (int i = hex.length() / 2 - 1; i >= 0; i--) {
			buffer.append(hex.charAt(i * 2));
			buffer.append(hex.charAt(i * 2 + 1));
		}
		return buffer.toString();
	}

	public static void reverseBytes(byte[] bytes) {
		for (int i = 0; i < bytes.length / 2; i++) {
			byte b = bytes[i];
			bytes[i] = bytes[bytes.length - i - 1];
			bytes[bytes.length - i - 1] = b;
		}
	}

	public static boolean isOdd(int i) {
		return (1 == (i & 0x1));
	}

	public static String dateToStr(Date date) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(date);

		StringBuffer buffer = new StringBuffer();
		buffer.append(calendar.get(1));
		buffer.append("-");
		buffer.append(intToStr(calendar.get(2), 2));
		buffer.append("-");
		buffer.append(intToStr(calendar.get(5), 2));

		return buffer.toString();
	}

	public static String timeToStr(Date time) {
		Calendar calendar = Calendar.getInstance();
		calendar.setTime(time);

		StringBuffer buffer = new StringBuffer();
		buffer.append(intToStr(calendar.get(11), 2));
		buffer.append(":");
		buffer.append(intToStr(calendar.get(12), 2));
		buffer.append(":");
		buffer.append(intToStr(calendar.get(13), 2));

		return buffer.toString();
	}

	public static String datetimeToStr(Date datetime) {
		return String.valueOf(dateToStr(datetime)) + " " + timeToStr(datetime);
	}

	public static int compareString(String s1, String s2) {
		if (s1 == null && s2 == null)
			return 0;
		if (s1 == null && s2 != null)
			return -1;
		if (s1 != null && s2 == null)
			return 1;
		return s1.compareTo(s2);
	}

	public static int compareDate(Date d1, Date d2) {
		if (d1 == null || d2 == null)
			return 0;
		if (d1 == null && d2 != null)
			return -1;
		if (d1 != null && d2 == null)
			return 1;
		return compareString(dateToStr(d1), dateToStr(d2));
	}

	public static int compareTime(Date t1, Date t2) {
		if (t1 == null || t2 == null)
			return 0;
		if (t1 == null && t2 != null)
			return -1;
		if (t1 != null && t2 == null)
			return 1;
		return compareString(timeToStr(t1), timeToStr(t2));
	}

	public static int compareDatetime(Date d1, Date d2) {
		if (d1 == null || d2 == null)
			return 0;
		if (d1 == null && d2 != null)
			return -1;
		if (d1 != null && d2 == null)
			return 1;
		return compareString(datetimeToStr(d1), datetimeToStr(d2));
	}
}
