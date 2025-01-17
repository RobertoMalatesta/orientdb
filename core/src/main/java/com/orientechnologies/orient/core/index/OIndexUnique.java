/*
 *
 *  *  Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
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
 *  * For more information: http://orientdb.com
 *
 */
package com.orientechnologies.orient.core.index;

import com.orientechnologies.orient.core.exception.OInvalidIndexEngineIdException;
import com.orientechnologies.orient.core.id.ORID;
import com.orientechnologies.orient.core.index.engine.IndexEngineValidator;
import com.orientechnologies.orient.core.index.engine.UniqueIndexEngineValidator;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.tx.OTransactionIndexChangesPerKey;

/**
 * Index implementation that allows only one value for a key.
 *
 * @author Luca Garulli (l.garulli--(at)--orientdb.com)
 */
public class OIndexUnique extends OIndexOneValue {

  private final IndexEngineValidator<Object, ORID> uniqueValidator =
      new UniqueIndexEngineValidator(this);

  public OIndexUnique(OIndexMetadata im, final OStorage storage) {
    super(im, storage);
  }

  @Override
  public boolean isNativeTxSupported() {
    return true;
  }

  @Override
  public boolean canBeUsedInEqualityOperators() {
    return true;
  }

  @Override
  public boolean supportsOrderedIterations() {
    while (true) {
      try {
        return storage.hasIndexRangeQuerySupport(indexId);
      } catch (OInvalidIndexEngineIdException ignore) {
        doReloadIndexEngine();
      }
    }
  }

  @Override
  public Iterable<OTransactionIndexChangesPerKey.OTransactionIndexEntry> interpretTxKeyChanges(
      OTransactionIndexChangesPerKey changes) {
    return changes.interpret(OTransactionIndexChangesPerKey.Interpretation.Unique);
  }

  @Override
  public IndexEngineValidator<Object, ORID> getUniqueValidator() {
    return uniqueValidator;
  }
}
