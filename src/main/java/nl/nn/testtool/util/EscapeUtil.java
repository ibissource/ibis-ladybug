/*
 * Created on 16-Sep-09
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package nl.nn.testtool.util;

import java.util.List;

/**
 * @author m00f069
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class EscapeUtil {

	public static String escapeXml(String value) {
		if (value == null) {
			return "";
		} else {
			StringBuffer result = new StringBuffer();
			for (int i = 0; i < value.length(); i++) {
				if (value.charAt(i) == '<') {
					result.append("&lt;");
				} else if (value.charAt(i) == '>') {
					result.append("&gt;");
				} else if (value.charAt(i) == '\"') {
					result.append("&quot;");
				} else if (value.charAt(i) == '&') {
					result.append("&amp;");
				} else {
					result.append(value.charAt(i));
				}
			}
			return result.toString();
		}
	}

	public static String escapeCsv(String value) {
		if (value == null) {
			value = "";
		} else {
			boolean specialCharsFound = false;
			if (value.length() == 0) {
				specialCharsFound = true;
			} else {
				for (int i = 0; i < value.length(); i++) {
					int c = value.charAt(i);
					if (c == '"') {
						specialCharsFound = true;
						value = value.substring(0, i) + "\"" + value.substring(i);
						i++;
					} else if (c == ',' ||c == '\n' || c == '\r') {
						specialCharsFound = true;
					}
				}
			}
			if (specialCharsFound) {
				value = "\"" + value + "\"";
			}
		}
		return value;
	}
	
	public static String escapeCsv(List values) {
		StringBuffer stringBuffer = new StringBuffer();
		for (int i = 0; i < values.size() - 1; i++) {
			stringBuffer.append(escapeCsv((String)values.get(i)));
			stringBuffer.append(",");
		}
		stringBuffer.append(escapeCsv((String)values.get(values.size() - 1)));
		return stringBuffer.toString();
	}

}