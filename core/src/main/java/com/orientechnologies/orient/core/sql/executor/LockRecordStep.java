package com.orientechnologies.orient.core.sql.executor;

import com.orientechnologies.common.concur.OTimeoutException;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.sql.executor.resultset.OResultSetMapper;
import com.orientechnologies.orient.core.storage.OStorage;

public class LockRecordStep extends AbstractExecutionStep {
  private final OStorage.LOCKING_STRATEGY lockStrategy;

  public LockRecordStep(
      OStorage.LOCKING_STRATEGY lockStrategy, OCommandContext ctx, boolean enableProfiling) {
    super(ctx, enableProfiling);
    this.lockStrategy = lockStrategy;
  }

  @Override
  public OResultSet syncPull(OCommandContext ctx) throws OTimeoutException {
    OResultSet upstream = getPrev().get().syncPull(ctx);
    return new OResultSetMapper(upstream, (result) -> mapResult(ctx, result));
  }

  private OResult mapResult(OCommandContext ctx, OResult result) {
    result
        .getElement()
        .ifPresent(x -> ctx.getDatabase().getTransaction().lockRecord(x, lockStrategy));
    return result;
  }

  @Override
  public String prettyPrint(int depth, int indent) {
    String spaces = OExecutionStepInternal.getIndent(depth, indent);
    StringBuilder result = new StringBuilder();
    result.append(spaces);
    result.append("+ LOCK RECORD");
    result.append("\n");
    result.append(spaces);
    result.append("  lock strategy: " + lockStrategy);

    return result.toString();
  }
}
