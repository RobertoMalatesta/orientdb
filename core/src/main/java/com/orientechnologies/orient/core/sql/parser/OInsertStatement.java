/* Generated By:JJTree: Do not edit this line. OInsertStatement.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OBasicCommandContext;
import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.ODatabaseSession;
import com.orientechnologies.orient.core.sql.executor.OInsertExecutionPlan;
import com.orientechnologies.orient.core.sql.executor.OInsertExecutionPlanner;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.HashMap;
import java.util.Map;

public class OInsertStatement extends OStatement {

  OIdentifier targetClass;
  OIdentifier targetClusterName;
  OCluster targetCluster;
  OIndexIdentifier targetIndex;
  OInsertBody insertBody;
  OProjection returnStatement;
  OSelectStatement selectStatement;
  boolean selectInParentheses = false;
  boolean selectWithFrom = false;
  boolean unsafe = false;

  public OInsertStatement(int id) {
    super(id);
  }

  public OInsertStatement(OrientSql p, int id) {
    super(p, id);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    builder.append("INSERT INTO ");
    if (targetClass != null) {
      targetClass.toString(params, builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toString(params, builder);
      }
    }
    if (targetCluster != null) {
      targetCluster.toString(params, builder);
    }
    if (targetIndex != null) {
      targetIndex.toString(params, builder);
    }
    if (insertBody != null) {
      builder.append(" ");
      insertBody.toString(params, builder);
    }
    if (returnStatement != null) {
      builder.append(" RETURN ");
      returnStatement.toString(params, builder);
    }
    if (selectStatement != null) {
      builder.append(" ");
      if (selectWithFrom) {
        builder.append("FROM ");
      }
      if (selectInParentheses) {
        builder.append("(");
      }
      selectStatement.toString(params, builder);
      if (selectInParentheses) {
        builder.append(")");
      }
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  public void toGenericStatement(StringBuilder builder) {
    builder.append("INSERT INTO ");
    if (targetClass != null) {
      targetClass.toGenericStatement(builder);
      if (targetClusterName != null) {
        builder.append(" CLUSTER ");
        targetClusterName.toGenericStatement(builder);
      }
    }
    if (targetCluster != null) {
      targetCluster.toGenericStatement(builder);
    }
    if (targetIndex != null) {
      targetIndex.toGenericStatement(builder);
    }
    if (insertBody != null) {
      builder.append(" ");
      insertBody.toGenericStatement(builder);
    }
    if (returnStatement != null) {
      builder.append(" RETURN ");
      returnStatement.toGenericStatement(builder);
    }
    if (selectStatement != null) {
      builder.append(" ");
      if (selectWithFrom) {
        builder.append("FROM ");
      }
      if (selectInParentheses) {
        builder.append("(");
      }
      selectStatement.toGenericStatement(builder);
      if (selectInParentheses) {
        builder.append(")");
      }
    }
    if (unsafe) {
      builder.append(" UNSAFE");
    }
  }

  @Override
  public OInsertStatement copy() {
    OInsertStatement result = new OInsertStatement(-1);
    result.targetClass = targetClass == null ? null : targetClass.copy();
    result.targetClusterName = targetClusterName == null ? null : targetClusterName.copy();
    result.targetCluster = targetCluster == null ? null : targetCluster.copy();
    result.targetIndex = targetIndex == null ? null : targetIndex.copy();
    result.insertBody = insertBody == null ? null : insertBody.copy();
    result.returnStatement = returnStatement == null ? null : returnStatement.copy();
    result.selectStatement = selectStatement == null ? null : selectStatement.copy();
    result.selectInParentheses = selectInParentheses;
    result.selectWithFrom = selectWithFrom;
    result.unsafe = unsafe;
    return result;
  }

  @Override
  public OResultSet execute(
      ODatabaseSession db, Object[] args, OCommandContext parentCtx, boolean usePlanCache) {
    OBasicCommandContext ctx = new OBasicCommandContext(db);
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    Map<Object, Object> params = new HashMap<>();
    if (args != null) {
      for (int i = 0; i < args.length; i++) {
        params.put(i, args[i]);
      }
    }
    ctx.setInputParameters(params);
    OInsertExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (OInsertExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal(ctx);
    return new OLocalResultSet(executionPlan, ctx);
  }

  @Override
  public OResultSet execute(
      ODatabaseSession db, Map params, OCommandContext parentCtx, boolean usePlanCache) {
    OBasicCommandContext ctx = new OBasicCommandContext(db);
    if (parentCtx != null) {
      ctx.setParentWithoutOverridingChild(parentCtx);
    }
    ctx.setInputParameters(params);
    OInsertExecutionPlan executionPlan;
    if (usePlanCache) {
      executionPlan = createExecutionPlan(ctx, false);
    } else {
      executionPlan = (OInsertExecutionPlan) createExecutionPlanNoCache(ctx, false);
    }
    executionPlan.executeInternal(ctx);
    return new OLocalResultSet(executionPlan, ctx);
  }

  public OInsertExecutionPlan createExecutionPlan(OCommandContext ctx, boolean enableProfiling) {
    OInsertExecutionPlanner planner = new OInsertExecutionPlanner(this);
    OInsertExecutionPlan result = planner.createExecutionPlan(ctx, enableProfiling);
    result.setStatement(originalStatement);
    result.setGenericStatement(this.toGenericStatement());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OInsertStatement that = (OInsertStatement) o;

    if (selectInParentheses != that.selectInParentheses) return false;
    if (selectWithFrom != that.selectWithFrom) return false;
    if (unsafe != that.unsafe) return false;
    if (targetClass != null ? !targetClass.equals(that.targetClass) : that.targetClass != null)
      return false;
    if (targetClusterName != null
        ? !targetClusterName.equals(that.targetClusterName)
        : that.targetClusterName != null) return false;
    if (targetCluster != null
        ? !targetCluster.equals(that.targetCluster)
        : that.targetCluster != null) return false;
    if (targetIndex != null ? !targetIndex.equals(that.targetIndex) : that.targetIndex != null)
      return false;
    if (insertBody != null ? !insertBody.equals(that.insertBody) : that.insertBody != null)
      return false;
    if (returnStatement != null
        ? !returnStatement.equals(that.returnStatement)
        : that.returnStatement != null) return false;
    if (selectStatement != null
        ? !selectStatement.equals(that.selectStatement)
        : that.selectStatement != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = targetClass != null ? targetClass.hashCode() : 0;
    result = 31 * result + (targetClusterName != null ? targetClusterName.hashCode() : 0);
    result = 31 * result + (targetCluster != null ? targetCluster.hashCode() : 0);
    result = 31 * result + (targetIndex != null ? targetIndex.hashCode() : 0);
    result = 31 * result + (insertBody != null ? insertBody.hashCode() : 0);
    result = 31 * result + (returnStatement != null ? returnStatement.hashCode() : 0);
    result = 31 * result + (selectStatement != null ? selectStatement.hashCode() : 0);
    result = 31 * result + (selectInParentheses ? 1 : 0);
    result = 31 * result + (selectWithFrom ? 1 : 0);
    result = 31 * result + (unsafe ? 1 : 0);
    return result;
  }

  public OIdentifier getTargetClass() {
    return targetClass;
  }

  public OIdentifier getTargetClusterName() {
    return targetClusterName;
  }

  public OCluster getTargetCluster() {
    return targetCluster;
  }

  public OIndexIdentifier getTargetIndex() {
    return targetIndex;
  }

  public OInsertBody getInsertBody() {
    return insertBody;
  }

  public OProjection getReturnStatement() {
    return returnStatement;
  }

  public OSelectStatement getSelectStatement() {
    return selectStatement;
  }

  public boolean isSelectInParentheses() {
    return selectInParentheses;
  }

  public boolean isSelectWithFrom() {
    return selectWithFrom;
  }

  public boolean isUnsafe() {
    return unsafe;
  }
}
/* JavaCC - OriginalChecksum=ccfabcf022d213caed873e6256cb26ad (do not edit this line) */
