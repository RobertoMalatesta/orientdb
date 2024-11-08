/*
 * Copyright 2010-2016 OrientDB LTD (http://orientdb.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.orientechnologies.orient.test.database.auto;

import com.orientechnologies.orient.core.sql.executor.OResult;
import com.orientechnologies.orient.core.sql.executor.OResultSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test
public class PreparedStatementTest extends DocumentDBBaseTest {
  @Parameters(value = "url")
  public PreparedStatementTest(@Optional String url) {
    super(url);
  }

  @BeforeClass
  @Override
  public void beforeClass() throws Exception {
    super.beforeClass();
    database.command("CREATE CLASS PreparedStatementTest1");
    database.command("insert into PreparedStatementTest1 (name, surname) values ('foo1', 'bar1')");
    database.command(
        "insert into PreparedStatementTest1 (name, listElem) values ('foo2', ['bar2'])");
  }

  @Test
  public void testUnnamedParamTarget() {
    Iterable<OResult> result =
        database.command("select from ?", "PreparedStatementTest1").stream().toList();

    Set<String> expected = new HashSet<String>();
    expected.add("foo1");
    expected.add("foo2");
    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertTrue(expected.contains(doc.getProperty("name")));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamTarget() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("className", "PreparedStatementTest1");
    Iterable<OResult> result = database.command("select from :className", params).stream().toList();

    Set<String> expected = new HashSet<String>();
    expected.add("foo1");
    expected.add("foo2");
    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertTrue(expected.contains(doc.getProperty("name")));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamTargetRid() {

    List<OResult> result =
        database.command("select from PreparedStatementTest1 limit 1").stream().toList();

    OResult record = result.iterator().next();

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("inputRid", record.getIdentity().get());
    Iterable<OResult> oldResult =
        database.command("select from :inputRid", params).stream().toList();

    boolean found = false;
    for (OResult doc : oldResult) {
      found = true;
      Assert.assertEquals(doc.getIdentity().get(), record.getIdentity());
      Assert.assertEquals(doc.<Object>getProperty("name"), record.getProperty("name"));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testUnnamedParamTargetRid() {

    Iterable<OResult> result =
        database.command("select from PreparedStatementTest1 limit 1").stream().toList();

    OResult record = result.iterator().next();
    result = database.command("select from ?", record.getIdentity()).stream().toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getIdentity().get(), record.getIdentity().get());
      Assert.assertEquals(doc.<Object>getProperty("name"), record.getProperty("name"));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamTargetDocument() {

    Iterable<OResult> result =
        database.query("select from PreparedStatementTest1 limit 1").stream().toList();

    OResult record = result.iterator().next();

    Map<String, Object> params = new HashMap<String, Object>();
    params.put("inputRid", record.getIdentity().get());
    List<OResult> result1 = database.command("select from :inputRid", params).stream().toList();

    boolean found = false;
    for (OResult doc : result1) {
      found = true;
      Assert.assertEquals(doc.getIdentity().get(), record.getIdentity().get());
      Assert.assertEquals(doc.<Object>getProperty("name"), record.getProperty("name"));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testUnnamedParamTargetDocument() {

    Iterable<OResult> result =
        database.command("select from PreparedStatementTest1 limit 1").stream().toList();

    OResult record = result.iterator().next();
    result = database.command("select from ?", record).stream().toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getIdentity().get(), record.getIdentity().get());
      Assert.assertEquals(doc.<Object>getProperty("name"), record.getProperty("name"));
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testUnnamedParamFlat() {
    OResultSet result = database.query("select from PreparedStatementTest1 where name = ?", "foo1");

    boolean found = false;
    while (result.hasNext()) {
      OResult doc = result.next();
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamFlat() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("name", "foo1");
    OResultSet result =
        database.query("select from PreparedStatementTest1 where name = :name", params);

    boolean found = false;
    while (result.hasNext()) {
      OResult doc = result.next();
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testUnnamedParamInArray() {
    List<OResult> result =
        database.command("select from PreparedStatementTest1 where name in [?]", "foo1").stream()
            .toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamInArray() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("name", "foo1");
    List<OResult> result =
        database
            .command("select from PreparedStatementTest1 where name in [:name]", params)
            .stream()
            .toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testUnnamedParamInArray2() {
    List<OResult> result =
        database
            .command("select from PreparedStatementTest1 where name in [?, 'antani']", "foo1")
            .stream()
            .toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testNamedParamInArray2() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("name", "foo1");
    List<OResult> result =
        database
            .command("select from PreparedStatementTest1 where name in [:name, 'antani']", params)
            .stream()
            .toList();

    boolean found = false;
    for (OResult doc : result) {
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testSubqueryUnnamedParamFlat() {
    OResultSet result =
        database.query(
            "select from (select from PreparedStatementTest1 where name = ?) where name = ?",
            "foo1",
            "foo1");

    boolean found = false;
    while (result.hasNext()) {
      OResult doc = result.next();
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testSubqueryNamedParamFlat() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("name", "foo1");
    OResultSet result =
        database.query(
            "select from (select from PreparedStatementTest1 where name = :name) where name ="
                + " :name",
            params);

    boolean found = false;
    while (result.hasNext()) {
      OResult doc = result.next();
      found = true;
      Assert.assertEquals(doc.getProperty("name"), "foo1");
    }
    Assert.assertTrue(found);
  }

  @Test
  public void testFunction() {
    Map<String, Object> params = new HashMap<String, Object>();
    params.put("one", 1);
    params.put("three", 3);
    OResultSet result = database.query("select max(:one, :three) as maximo", params);

    boolean found = false;
    while (result.hasNext()) {
      OResult doc = result.next();
      found = true;
      Assert.assertEquals(doc.<Object>getProperty("maximo"), 3);
    }
    Assert.assertTrue(found);
  }
}
