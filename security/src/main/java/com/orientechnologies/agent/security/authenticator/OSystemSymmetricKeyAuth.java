/*
 *
 *  *  Copyright 2016 Orient Technologies LTD (info(at)orientdb.com)
 *  *
 *  *  Licensed under the Apache License, Version 2.0 (the "License");
 *  *  you may not use this file except in compliance with the License.
 *  *  You may obtain a copy of the License at
 *  *
 *  *       http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  *  Unless required by applicable law or agreed to in writing, software
 *  *  distributed under the License is distributed on an "AS IS" BASIS,
 *  *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  *  See the License for the specific language governing permissions and
 *  *  limitations under the License.
 *  *
 *  * For more information: http://www.orientdb.com
 *
 */
package com.orientechnologies.agent.security.authenticator;

import com.orientechnologies.common.log.OLogManager;
import com.orientechnologies.common.log.OLogger;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.metadata.security.OSecurityUser;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.security.OSecurityManager;
import com.orientechnologies.orient.core.security.symmetrickey.OSymmetricKey;
import com.orientechnologies.orient.core.security.symmetrickey.OUserSymmetricKeyConfig;
import com.orientechnologies.orient.server.security.authenticator.OSystemUserAuthenticator;

/**
 * Provides an OSystem user symmetric key authenticator, derived from OSystemUserAuthenticator. This
 * is used in security.json.
 *
 * @author S. Colin Leister
 */
public class OSystemSymmetricKeyAuth extends OSystemUserAuthenticator {
  private static final OLogger logger =
      OLogManager.instance().logger(OSystemSymmetricKeyAuth.class);

  // OSecurityComponent
  // Called once the Server is running.
  public void active() {
    logger.debug("OSystemSymmetricKeyAuth is active");
  }

  // OSecurityAuthenticator
  // Returns the actual username if successful, null otherwise.
  // This will authenticate username using the system database.
  public OSecurityUser authenticate(
      ODatabaseSession session, final String username, final String password) {
    OSecurityUser principal = null;

    try {
      // dbName parameter is null because we don't need to filter any roles for this.
      OSecurityUser user = getSecurity().getSystemUser(username, null);

      if (user != null && user.getAccountStatus() == OSecurityUser.STATUSES.ACTIVE) {
        ODocument doc =
            getSecurity()
                .getContext()
                .getSystemDatabase()
                .executeWithDB(
                    (db) -> {
                      return db.load(user.getIdentity().getIdentity());
                    });
        OUserSymmetricKeyConfig userConfig = new OUserSymmetricKeyConfig(doc);

        OSymmetricKey sk = OSymmetricKey.fromConfig(userConfig);

        String decryptedUsername = sk.decryptAsString(password);

        if (OSecurityManager.instance().checkPassword(username, decryptedUsername)) {
          principal = user;
        }
      }
    } catch (Exception ex) {
      logger.error("authenticate()", ex);
    }

    return principal;
  }
}
