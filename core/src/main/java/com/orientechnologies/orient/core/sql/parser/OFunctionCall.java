/* Generated By:JJTree: Do not edit this line. OFunctionCall.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=O,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package com.orientechnologies.orient.core.sql.parser;

import com.orientechnologies.orient.core.command.OCommandContext;
import com.orientechnologies.orient.core.db.record.OIdentifiable;
import com.orientechnologies.orient.core.exception.OCommandExecutionException;
import com.orientechnologies.orient.core.sql.OSQLEngine;
import com.orientechnologies.orient.core.sql.executor.AggregationContext;
import com.orientechnologies.orient.core.sql.executor.OFunctionAggregationContext;
import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultInternal;
import com.orientechnologies.orient.core.sql.functions.OIndexableSQLFunction;
import com.orientechnologies.orient.core.sql.functions.OSQLFunction;
import com.orientechnologies.orient.core.sql.functions.graph.OSQLFunctionMove;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class OFunctionCall extends SimpleNode {

  protected OIdentifier name;

  protected List<OExpression> params = new ArrayList<OExpression>();

  public OFunctionCall(int id) {
    super(id);
  }

  public OFunctionCall(OrientSql p, int id) {
    super(p, id);
  }

  public boolean isStar() {

    if (this.params.size() != 1) {
      return false;
    }
    OExpression param = params.get(0);
    if (param.mathExpression == null || !(param.mathExpression instanceof OBaseExpression)) {

      return false;
    }
    OBaseExpression base = (OBaseExpression) param.mathExpression;
    if (base.getIdentifier() == null || base.getIdentifier().suffix == null) {
      return false;
    }
    return base.getIdentifier().suffix.star;
  }

  public List<OExpression> getParams() {
    return params;
  }

  public void setParams(List<OExpression> params) {
    this.params = params;
  }

  public void addParam(OExpression param) {
    this.params.add(param);
  }

  public void toString(Map<Object, Object> params, StringBuilder builder) {
    name.toString(params, builder);
    builder.append("(");
    boolean first = true;
    for (OExpression expr : this.params) {
      if (!first) {
        builder.append(", ");
      }
      expr.toString(params, builder);
      first = false;
    }
    builder.append(")");
  }

  public void toGenericStatement(StringBuilder builder) {
    name.toGenericStatement(builder);
    builder.append("(");
    boolean first = true;
    for (OExpression expr : this.params) {
      if (!first) {
        builder.append(", ");
      }
      expr.toGenericStatement(builder);
      first = false;
    }
    builder.append(")");
  }

  public Object execute(Object targetObjects, OCommandContext ctx) {
    return execute(targetObjects, ctx, name.getStringValue());
  }

  private Object execute(Object targetObjects, OCommandContext ctx, String name) {
    List<Object> paramValues = new ArrayList<Object>();

    Object record = null;

    if (record == null) {
      if (targetObjects instanceof OIdentifiable) {
        record = (OIdentifiable) targetObjects;
      } else if (targetObjects instanceof OResult) {
        record = ((OResult) targetObjects).toElement();
      } else {
        record = targetObjects;
      }
    }
    if (record == null) {
      Object current = ctx == null ? null : ctx.getVariable("$current");
      if (current != null) {
        if (current instanceof OIdentifiable) {
          record = current;
        } else if (current instanceof OResult) {
          record = ((OResult) current).toElement();
        } else {
          record = current;
        }
      }
    }
    for (OExpression expr : this.params) {
      if (targetObjects instanceof OResult) {
        paramValues.add(expr.execute((OResult) targetObjects, ctx));
      } else if (record instanceof OIdentifiable) {
        paramValues.add(expr.execute((OIdentifiable) record, ctx));
      } else if (record instanceof OResult) {
        paramValues.add(expr.execute((OResult) record, ctx));
      } else if (record == null) {
        paramValues.add(expr.execute((OResult) record, ctx));
      } else {
        throw new OCommandExecutionException("Invalid value for $current: " + record);
      }
    }
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name);
    if (function != null) {
      function.config(this.params.toArray());

      validateFunctionParams(function, paramValues);

      if (record instanceof OIdentifiable) {
        return function.execute(
            targetObjects, (OIdentifiable) record, null, paramValues.toArray(), ctx);
      } else if (record instanceof OResult) {
        return function.execute(
            targetObjects,
            ((OResult) record).getElement().orElse(null),
            null,
            paramValues.toArray(),
            ctx);
      } else if (record == null) {
        return function.execute(targetObjects, null, null, paramValues.toArray(), ctx);
      } else {
        throw new OCommandExecutionException("Invalid value for $current: " + record);
      }
    } else {
      throw new OCommandExecutionException("Function not found: " + name);
    }
  }

  private void validateFunctionParams(OSQLFunction function, List<Object> paramValues) {
    if (function.getMaxParams() == -1 || function.getMaxParams() > 0) {
      if (paramValues.size() < function.getMinParams()
          || (function.getMaxParams() > -1 && paramValues.size() > function.getMaxParams())) {
        String params;
        if (function.getMinParams() == function.getMaxParams()) {
          params = "" + function.getMinParams();
        } else {
          params = function.getMinParams() + "-" + function.getMaxParams();
        }
        throw new OCommandExecutionException(
            "Syntax error: function '"
                + function.getName()
                + "' needs "
                + params
                + " argument(s) while has been received "
                + paramValues.size());
      }
    }
  }

  public boolean isIndexedFunctionCall() {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    return (function instanceof OIndexableSQLFunction);
  }

  /**
   * see OIndexableSQLFunction.searchFromTarget()
   *
   * @param target
   * @param ctx
   * @param operator
   * @param rightValue
   * @return
   */
  public Iterable<OIdentifiable> executeIndexedFunction(
      OFromClause target, OCommandContext ctx, OBinaryCompareOperator operator, Object rightValue) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function)
          .searchFromTarget(
              target, operator, rightValue, ctx, this.getParams().toArray(new OExpression[] {}));
    }
    return null;
  }

  /**
   * @param target query target
   * @param ctx execution context
   * @param operator operator at the right of the function
   * @param rightValue value to compare to function result
   * @return the approximate number of items returned by the condition execution, -1 if the
   *     extimation cannot be executed
   */
  public long estimateIndexedFunction(
      OFromClause target, OCommandContext ctx, OBinaryCompareOperator operator, Object rightValue) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function)
          .estimate(
              target, operator, rightValue, ctx, this.getParams().toArray(new OExpression[] {}));
    }
    return -1;
  }

  /**
   * tests if current function is an indexed function AND that function can also be executed without
   * using the index
   *
   * @param target the query target
   * @param context the execution context
   * @param operator
   * @param right
   * @return true if current function is an indexed function AND that function can also be executed
   *     without using the index, false otherwise
   */
  public boolean canExecuteIndexedFunctionWithoutIndex(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function)
          .canExecuteInline(
              target, operator, right, context, this.getParams().toArray(new OExpression[] {}));
    }
    return false;
  }

  /**
   * tests if current function is an indexed function AND that function can be used on this target
   *
   * @param target the query target
   * @param context the execution context
   * @param operator
   * @param right
   * @return true if current function is an indexed function AND that function can be used on this
   *     target, false otherwise
   */
  public boolean allowsIndexedFunctionExecutionOnTarget(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function)
          .allowsIndexedExecution(
              target, operator, right, context, this.getParams().toArray(new OExpression[] {}));
    }
    return false;
  }

  /**
   * tests if current expression is an indexed function AND the function has also to be executed
   * after the index search. In some cases, the index search is accurate, so this condition can be
   * excluded from further evaluation. In other cases the result from the index is a superset of the
   * expected result, so the function has to be executed anyway for further filtering
   *
   * @param target the query target
   * @param context the execution context
   * @return true if current expression is an indexed function AND the function has also to be
   *     executed after the index search.
   */
  public boolean executeIndexedFunctionAfterIndexSearch(
      OFromClause target, OCommandContext context, OBinaryCompareOperator operator, Object right) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    if (function instanceof OIndexableSQLFunction) {
      return ((OIndexableSQLFunction) function)
          .shouldExecuteAfterSearch(
              target, operator, right, context, this.getParams().toArray(new OExpression[] {}));
    }
    return false;
  }

  public boolean isExpand() {
    return name.getStringValue().equalsIgnoreCase("expand");
  }

  public boolean needsAliases(Set<String> aliases) {
    for (OExpression param : params) {
      if (param.needsAliases(aliases)) {
        return true;
      }
    }
    return false;
  }

  public boolean isAggregate() {
    if (isAggregateFunction()) {
      return true;
    }

    for (OExpression exp : params) {
      if (exp.isAggregate()) {
        return true;
      }
    }

    return false;
  }

  public SimpleNode splitForAggregation(
      AggregateProjectionSplit aggregateProj, OCommandContext ctx) {
    if (isAggregate()) {
      OFunctionCall newFunct = new OFunctionCall(-1);
      newFunct.name = this.name;
      OIdentifier functionResultAlias = aggregateProj.getNextAlias();

      if (isAggregateFunction()) {

        if (isStar()) {
          for (OExpression param : params) {
            newFunct.getParams().add(param);
          }
        } else {
          for (OExpression param : params) {
            if (param.isAggregate()) {
              throw new OCommandExecutionException(
                  "Cannot calculate an aggregate function of another aggregate function "
                      + toString());
            }
            OIdentifier nextAlias = aggregateProj.getNextAlias();
            OProjectionItem paramItem = new OProjectionItem(-1);
            paramItem.alias = nextAlias;
            paramItem.expression = param;
            aggregateProj.getPreAggregate().add(paramItem);

            newFunct.params.add(new OExpression(nextAlias));
          }
        }
        aggregateProj.getAggregate().add(createProjection(newFunct, functionResultAlias));
        return new OExpression(functionResultAlias);
      } else {
        if (isStar()) {
          for (OExpression param : params) {
            newFunct.getParams().add(param);
          }
        } else {
          for (OExpression param : params) {
            newFunct.getParams().add(param.splitForAggregation(aggregateProj, ctx));
          }
        }
      }
      return newFunct;
    }
    return this;
  }

  private boolean isAggregateFunction() {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    function.config(this.params.toArray());
    return function.aggregateResults();
  }

  private OProjectionItem createProjection(OFunctionCall newFunct, OIdentifier alias) {
    OLevelZeroIdentifier l0 = new OLevelZeroIdentifier(-1);
    l0.functionCall = newFunct;
    OBaseIdentifier l1 = new OBaseIdentifier(-1);
    l1.levelZero = l0;
    OBaseExpression l2 = new OBaseExpression(-1);
    l2.setIdentifier(l1);
    OExpression l3 = new OExpression(-1);
    l3.mathExpression = l2;
    OProjectionItem item = new OProjectionItem(-1);
    item.alias = alias;
    item.expression = l3;
    return item;
  }

  public boolean isEarlyCalculated(OCommandContext ctx) {

    if (isTraverseFunction()) return false;

    for (OExpression param : params) {
      if (!param.isEarlyCalculated(ctx)) {
        return false;
      }
    }

    return true;
  }

  private boolean isTraverseFunction() {
    if (name == null) {
      return false;
    }
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getValue());
    if (function instanceof OSQLFunctionMove) {
      return true;
    }
    return false;
  }

  public AggregationContext getAggregationContext(OCommandContext ctx) {
    OSQLFunction function = OSQLEngine.getInstance().getFunction(name.getStringValue());
    function.config(this.params.toArray());

    OFunctionAggregationContext result = new OFunctionAggregationContext(function, this.params);
    return result;
  }

  @Override
  public OFunctionCall copy() {
    OFunctionCall result = new OFunctionCall(-1);
    result.name = name;
    result.params = params.stream().map(x -> x.copy()).collect(Collectors.toList());
    return result;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    OFunctionCall that = (OFunctionCall) o;

    if (name != null ? !name.equals(that.name) : that.name != null) return false;
    if (params != null ? !params.equals(that.params) : that.params != null) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = name != null ? name.hashCode() : 0;
    result = 31 * result + (params != null ? params.hashCode() : 0);
    return result;
  }

  public boolean refersToParent() {
    if (params != null) {
      for (OExpression param : params) {
        if (param != null && param.refersToParent()) {
          return true;
        }
      }
    }
    return false;
  }

  public OIdentifier getName() {
    return name;
  }

  public OMethodCall toMethod() {
    OMethodCall result = new OMethodCall(-1);
    result.methodName = name.copy();
    result.params = params.stream().map(x -> x.copy()).collect(Collectors.toList());
    return result;
  }

  public OResult serialize() {
    OResultInternal result = new OResultInternal();
    if (name != null) {
      result.setProperty("name", name.serialize());
    }
    if (params != null) {
      result.setProperty(
          "collection", params.stream().map(x -> x.serialize()).collect(Collectors.toList()));
    }
    return result;
  }

  public void deserialize(OResult fromResult) {
    if (fromResult.getProperty("name") != null) {
      name = OIdentifier.deserialize(fromResult.getProperty("name"));
    }
    if (fromResult.getProperty("params") != null) {
      params = new ArrayList<>();
      List<OResult> ser = fromResult.getProperty("params");
      for (OResult item : ser) {
        OExpression exp = new OExpression(-1);
        exp.deserialize(item);
        params.add(exp);
      }
    }
  }

  public void extractSubQueries(OIdentifier letAlias, SubQueryCollector collector) {
    for (OExpression param : this.params) {
      param.extractSubQueries(letAlias, collector);
    }
  }

  public void extractSubQueries(SubQueryCollector collector) {
    for (OExpression param : this.params) {
      param.extractSubQueries(collector);
    }
  }

  public boolean isCacheable() {
    if (isGraphFunction()) {
      return true;
    }
    return false; // TODO
  }

  private boolean isGraphFunction() {
    String string = name.getStringValue();
    if (string.equalsIgnoreCase("out")) {
      return true;
    }
    if (string.equalsIgnoreCase("outE")) {
      return true;
    }
    if (string.equalsIgnoreCase("outV")) {
      return true;
    }
    if (string.equalsIgnoreCase("in")) {
      return true;
    }
    if (string.equalsIgnoreCase("inE")) {
      return true;
    }
    if (string.equalsIgnoreCase("inV")) {
      return true;
    }
    if (string.equalsIgnoreCase("both")) {
      return true;
    }
    if (string.equalsIgnoreCase("bothE")) {
      return true;
    }
    if (string.equalsIgnoreCase("bothV")) {
      return true;
    }
    return false;
  }
}
/* JavaCC - OriginalChecksum=290d4e1a3f663299452e05f8db718419 (do not edit this line) */
