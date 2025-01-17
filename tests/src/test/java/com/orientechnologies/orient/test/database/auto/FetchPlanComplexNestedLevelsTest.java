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

import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.orient.core.sql.executor.OResult;
import java.util.List;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

@Test(groups = "query")
public class FetchPlanComplexNestedLevelsTest extends DocumentDBBaseTest {

  @Parameters(value = "url")
  public FetchPlanComplexNestedLevelsTest(@Optional String url) {
    super(url);
  }

  @BeforeClass
  public void beforeClass() throws Exception {
    super.beforeClass();

    final OClass personTest = database.getMetadata().getSchema().getClass("PersonTest");
    if (personTest == null)
      database
          .getMetadata()
          .getSchema()
          .createClass("PersonTest", database.getMetadata().getSchema().getClass("V"));
    else if (personTest.getSuperClass() == null)
      personTest.setSuperClass(database.getMetadata().getSchema().getClass("V"));

    final OClass followTest = database.getMetadata().getSchema().getClass("FollowTest");
    if (followTest == null)
      database
          .getMetadata()
          .getSchema()
          .createClass("FollowTest", database.getMetadata().getSchema().getClass("E"));
    else if (followTest.getSuperClass() == null)
      followTest.setSuperClass(database.getMetadata().getSchema().getClass("E"));

    database.command("create vertex PersonTest set name = 'A'").close();
    database.command("create vertex PersonTest set name = 'B'").close();
    database.command("create vertex PersonTest set name = 'C'").close();

    database
        .command(
            "create edge FollowTest from (select from PersonTest where name = 'A') to (select from"
                + " PersonTest where name = 'B')")
        .close();
    database
        .command(
            "create edge FollowTest from (select from PersonTest where name = 'B') to (select from"
                + " PersonTest where name = 'C')")
        .close();
  }

  @Test
  public void queryAll2() {
    final List<OResult> result =
        database
            .query(
                "select @this.toJSON('fetchPlan:*:2') as json from (select from PersonTest where"
                    + " name='A')")
            .stream()
            .toList();

    Assert.assertEquals(result.size(), 1);
    String json = result.get(0).getProperty("json");

    Assert.assertNotNull(json);

    final ODocument parsed = new ODocument().fromJSON(json);

    Assert.assertNotNull(parsed.rawField("out_FollowTest.in.out_FollowTest"));
  }

  @Test
  public void queryOutWildcard2() {
    final List<OResult> result =
        database
            .query(
                "select @this.toJSON('fetchPlan:out_*:2') as json from (select from PersonTest"
                    + " where name='A')")
            .stream()
            .toList();

    Assert.assertEquals(result.size(), 1);
    String json = result.get(0).getProperty("json");

    Assert.assertNotNull(json);

    final ODocument parsed = new ODocument().fromJSON(json);

    Assert.assertNotNull(parsed.rawField("out_FollowTest.in.out_FollowTest"));
  }

  @Test
  public void queryOutOneLevelOnly() {
    final List<OResult> result =
        database
            .query(
                "select @this.toJSON('fetchPlan:[0]out_*:0') as json from (select from PersonTest"
                    + " where name='A')")
            .stream()
            .toList();

    Assert.assertEquals(result.size(), 1);
    String json = result.get(0).getProperty("json");

    Assert.assertNotNull(json);

    int pos = json.indexOf("\"in\":\"");
    Assert.assertTrue(pos > -1);

    Assert.assertEquals(json.charAt(pos) + 1, '#');
  }

  @Test
  public void startZeroGetOutStar2() {
    final List<OResult> result =
        database
            .query(
                "select @this.toJSON('fetchPlan:[0]out_*:2') as json from (select from PersonTest"
                    + " where name='A')")
            .stream()
            .toList();

    Assert.assertEquals(result.size(), 1);
    String json = result.get(0).getProperty("json");

    Assert.assertNotNull(json);

    int pos = json.indexOf("\"in\":{");
    Assert.assertTrue(pos > -1);
  }

  @Test
  public void start2GetOutStar2() {
    final List<OResult> result =
        database
            .query(
                "select @this.toJSON('fetchPlan:[2]out_*:2') as json from (select from PersonTest"
                    + " where name='A')")
            .stream()
            .toList();

    Assert.assertEquals(result.size(), 1);
    String json = result.get(0).getProperty("json");

    Assert.assertNotNull(json);

    int pos = json.indexOf("\"in\":\"");
    Assert.assertFalse(pos > -1);
  }
}
