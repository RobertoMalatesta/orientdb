package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OExecutionStream;
import com.orientechnologies.orient.core.storage.OStorage;
import com.orientechnologies.orient.core.storage.OStorage.LOCKING_STRATEGY;

public class UnlockRecordStep extends AbstractExecutionStep {
  private final OStorage.LOCKING_STRATEGY lockStrategy;

  public UnlockRecordStep(
      OStorage.LOCKING_STRATEGY lockStrategy, OCommandContext ctx, boolean enableProfiling) {
    super(ctx, enableProfiling);
    this.lockStrategy = lockStrategy;
  }

  @Override
  public OExecutionStream internalStart(OCommandContext ctx) throws OTimeoutException {
    OExecutionStream upstream = getPrev().get().start(ctx);
    return upstream.map(this::mapResult);
  }

  private OResult mapResult(OResult result, OCommandContext ctx) {
    if (LOCKING_STRATEGY.EXCLUSIVE_LOCK.equals(lockStrategy)) {
      result.getElement().ifPresent(x -> ctx.getDatabase().unlock(x.getIdentity()));
    }
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ UNLOCK RECORD");
    result.append("\n");
    result.append(spaces);
    result.append("  lock strategy: " + lockStrategy);

    return result.toString();
  }
}
