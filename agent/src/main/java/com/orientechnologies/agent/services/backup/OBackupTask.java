/*
 * Copyright 2015 OrientDB LTD (info(at)orientdb.com)
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 *   For more information: http://www.orientdb.com
 */

package com.orientechnologies.agent.services.backup;

import com.orientechnologies.agent.services.backup.log.OBackupLog;
import com.orientechnologies.agent.services.backup.log.OBackupLogType;
import com.orientechnologies.agent.services.backup.strategy.OBackupStrategy;
import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import com.orientechnologies.enterprise.server.OEnterpriseServer;
import com.orientechnologies.orient.core.Orient;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.io.IOException;
import java.util.Date;
import java.util.TimerTask;

/** Created by Enrico Risa on 25/03/16. */
public class OBackupTask implements OBackupListener {
  private static final OLogger logger = OLogManager.instance().logger(OBackupTask.class);
  private OBackupStrategy strategy;
  private TimerTask task;
  private OBackupListener listener;
  private OEnterpriseServer server;

  public OBackupTask(OBackupStrategy strategy, OEnterpriseServer server) {
    this.strategy = strategy;
    this.server = server;
    schedule();
  }

  private void schedule() {
    if (strategy.isEnabled()) {
      final Date nextExecution = strategy.scheduleNextExecution(this);
      task =
          Orient.instance()
              .scheduleTask(
                  () -> {
                    server
                        .getDatabases()
                        .execute(
                            () -> {
                              try {
                                final long start = tickStart();
                                strategy.doBackup(OBackupTask.this);
                                tickEnd(start);
                              } catch (final IOException e) {
                                logger.error("Error %s", e, e.getMessage());
                              }
                            });
                  },
                  nextExecution,
                  0);
      logger.info(
          "Scheduled [%s] task :%s. Next execution will be %s ",
          strategy.getMode(), strategy.getUUID(), nextExecution);
    }
    strategy.retainLogs();
  }

  private long tickStart() {
    logger.info("Backup started %s ", strategy.getMode());
    return System.currentTimeMillis();
  }

  private void tickEnd(long start) {
    logger.info("Backup %s in (ms): %d", strategy.getMode(), (System.currentTimeMillis() - start));
  }

  public OBackupStrategy getStrategy() {
    return strategy;
  }

  public void changeConfig(final OBackupConfig config, final ODocument doc) {
    if (task != null) {
      task.cancel();
    }
    strategy.deleteLastScheduled();
    final OBackupStrategy strategy = config.strategy(doc, this.strategy.getLogger());

    if (!this.strategy.equals(strategy)) {
      strategy.markLastBackup();
    }
    this.strategy = strategy;
    schedule();
  }

  @Override
  public Boolean onEvent(final ODocument cfg, final OBackupLog log) {
    final boolean canContinue = invokeListener(cfg, log);
    if (OBackupLogType.BACKUP_FINISHED.equals(log.getType())
        || OBackupLogType.BACKUP_ERROR.equals(log.getType())) {
      if (canContinue) {
        schedule();
      }
    }
    return true;
  }

  private Boolean invokeListener(ODocument cfg, OBackupLog log) {
    if (listener != null) {
      try {
        return listener.onEvent(cfg, log);
      } catch (Exception e) {
        logger.info("Error invoking listener on event  [%s] ", log.getType());
      }
    }
    return true;
  }

  public void stop() {
    if (task != null) {
      task.cancel();
      logger.info("Cancelled schedule backup on database  [%s] ", strategy.getDbName());
    }
  }

  public void registerListener(final OBackupListener listener) {
    this.listener = listener;
  }

  public void restore(ODocument doc) {
    strategy.doRestore(this, doc);
  }

  public void deleteBackup(final long unitId, final long timestamp) {
    strategy.doDeleteBackup(this, unitId, timestamp);
  }
}
