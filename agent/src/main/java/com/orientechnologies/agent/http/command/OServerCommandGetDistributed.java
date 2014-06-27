/*
 * Copyright 2010-2013 Orient Technologies LTD (info--at--orientechnologies.com)
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
 * 
 * For more information: http://www.orientechnologies.com
 */
package com.orientechnologies.agent.http.command;

import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.server.distributed.ODistributedConfiguration;
import com.orientechnologies.orient.server.distributed.ODistributedMessageService;
import com.orientechnologies.orient.server.distributed.ODistributedServerManager;
import com.orientechnologies.orient.server.network.protocol.http.OHttpRequest;
import com.orientechnologies.orient.server.network.protocol.http.OHttpResponse;
import com.orientechnologies.orient.server.network.protocol.http.OHttpUtils;
import com.orientechnologies.orient.server.network.protocol.http.command.OServerCommandAuthenticatedServerAbstract;

import java.util.List;

public class OServerCommandGetDistributed extends OServerCommandAuthenticatedServerAbstract {
  private static final String[] NAMES = { "GET|distributed/*" };

  public OServerCommandGetDistributed() {
    super("server.profiler");
  }

  @Override
  public boolean execute(final OHttpRequest iRequest, OHttpResponse iResponse) throws Exception {
    final String[] parts = checkSyntax(iRequest.getUrl(), 2, "Syntax error: distributed/<command>/[<id>]");

    iRequest.data.commandInfo = "Distributed information";

    try {

      final String command = parts[1];
      final String id = parts.length > 2 ? parts[2] : null;

      final ODistributedServerManager manager = server.getDistributedManager();

      final ODocument doc;

      if (command.equalsIgnoreCase("node")) {

        doc = manager.getClusterConfiguration();

      } else if (command.equalsIgnoreCase("queue")) {

        final ODistributedMessageService messageService = manager.getMessageService();
        if (id == null) {
          // RETURN QUEUE NAMES
          final List<String> queues = messageService.getManagedQueueNames();
          doc = new ODocument();
          doc.field("queues", queues);
        } else {
          doc = messageService.getQueueStats(id);
        }

      } else if (command.equalsIgnoreCase("database")) {

        ODistributedConfiguration cfg = manager.getDatabaseConfiguration(id);
        doc = cfg.serialize();

      } else if (command.equalsIgnoreCase("stats")) {

        doc = manager.getStats();

      } else
        throw new IllegalArgumentException("Command '" + command + "' not supported");

      iResponse.send(OHttpUtils.STATUS_OK_CODE, "OK", OHttpUtils.CONTENT_TEXT_PLAIN, doc.toJSON(""), null);

    } catch (Exception e) {
      iResponse.send(OHttpUtils.STATUS_BADREQ_CODE, OHttpUtils.STATUS_BADREQ_DESCRIPTION, OHttpUtils.CONTENT_TEXT_PLAIN, e, null);
    }
    return false;
  }

  @Override
  public String[] getNames() {
    return NAMES;
  }
}
