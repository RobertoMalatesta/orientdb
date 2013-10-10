/*
 * Copyright 2010-2013 Orient Technologies LTD
 * All Rights Reserved. Commercial License.
 *
 * NOTICE:  All information contained herein is, and remains the property of
 * Orient Technologies LTD and its suppliers, if any.  The intellectual and
 * technical concepts contained herein are proprietary to
 * Orient Technologies LTD and its suppliers and may be covered by United
 * Kingdom and Foreign Patents, patents in process, and are protected by trade
 * secret or copyright law.
 * 
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Orient Technologies LTD.
 */
package com.orientechnologies.orient.monitor.http;

import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.monitor.OMonitorPlugin;
import com.orientechnologies.orient.monitor.OMonitorUtils;
import com.orientechnologies.orient.monitor.OMonitoredServer;
import com.orientechnologies.orient.server.OServerMain;
import com.orientechnologies.orient.server.config.OServerCommandConfiguration;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedDbAbstract;

public class OServerCommandGetServerLog extends
		OServerCommandAuthenticatedDbAbstract {

	private OMonitorPlugin monitor;
	private static final String[] NAMES = { "GET|log/*" };

	private static final String TAIL = "tail";

	private static final String FILE = "file";

	private static final String SEARCH = "search";

	private static final String ALLFILES = "files";

	SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");

	public OServerCommandGetServerLog(
			final OServerCommandConfiguration iConfiguration) {
	}

	public OServerCommandGetServerLog() {
	}

	@Override
	public boolean execute(final OHttpRequest iRequest, OHttpResponse iResponse)
			throws Exception {

		if (monitor == null)
			monitor = OServerMain.server().getPluginByClass(
					OMonitorPlugin.class);
		final String[] urlParts = checkSyntax(iRequest.url, 1,
				"Syntax error: log/<type>?<value>");

		String type = urlParts[2]; // the type of the log tail search or file

		String value = iRequest.getParameter("searchvalue");

		String size = iRequest.getParameter("size");

		String logType = iRequest.getParameter("logtype");

		String dateFrom = iRequest.getParameter("dateFrom");

		String dateTo = iRequest.getParameter("dateTo");

		String hourFrom = iRequest.getParameter("hourFrom");

		String hourTo = iRequest.getParameter("hourTo");

		String selectedFile = iRequest.getParameter("file");

		// the name of the server
		String rid = iRequest.getParameter("name");
		rid = URLDecoder.decode(rid);
		OMonitoredServer s = monitor.getMonitoredServer(rid);
		ODocument server = s.getConfiguration();

		String parameters = (rid != null ? "?name=" + rid : "")
				+ (value != null ? "&searchvalue=" + value : "")
				+ (size != null ? "&size=" + size : "")
				+ (logType != null ? "&logtype=" + logType : "")
				+ (dateFrom != null ? "&dateFrom=" + dateFrom : "")
				+ (dateTo != null ? "&dateTo=" + dateTo : "")
				+ (hourFrom != null ? "&hourFrom=" + hourFrom : "")
				+ (hourTo != null ? "&hourTo=" + hourTo : "")
				+ (selectedFile != null ? "&file=" + selectedFile : "");

		final URL remoteUrl = new java.net.URL("http://" + server.field("url")
				+ "/log/" + type + parameters);

		String response = OMonitorUtils
				.fetchFromRemoteServer(server, remoteUrl);

		ODocument result = new ODocument();
		result.fromJSON(response);
		iResponse.writeRecord(result, null, "");
		return false;
	}

	@Override
	public String[] getNames() {
		return NAMES;
	}

}
