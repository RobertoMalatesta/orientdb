package com.orientechnologies.orient.core.index;

import static org.junit.Assert.assertArrayEquals;

import com.orientechnologies.orient.core.db.OrientDB;
import com.orientechnologies.orient.core.db.OrientDBConfig;
import com.orientechnologies.orient.core.db.document.ODatabaseDocument;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

@SuppressWarnings("unchecked")
public class OSimpleKeyIndexDefinitionTest {

  private OSimpleKeyIndexDefinition simpleKeyIndexDefinition;

  @Before
  public void beforeMethod() {
    simpleKeyIndexDefinition = new OSimpleKeyIndexDefinition(OType.INTEGER, OType.STRING);
  }

  @Test
  public void testGetFields() {
    Assert.assertTrue(simpleKeyIndexDefinition.getFields().isEmpty());
  }

  @Test
  public void testGetClassName() {
    Assert.assertNull(simpleKeyIndexDefinition.getClassName());
  }

  @Test
  public void testCreateValueSimpleKey() {
    final OSimpleKeyIndexDefinition keyIndexDefinition =
        new OSimpleKeyIndexDefinition(OType.INTEGER);
    final Object result = keyIndexDefinition.createValue("2");
    Assert.assertEquals(result, 2);
  }

  @Test
  public void testCreateValueCompositeKeyListParam() {
    final Object result = simpleKeyIndexDefinition.createValue(Arrays.asList("2", "3"));

    final OCompositeKey compositeKey = new OCompositeKey(Arrays.asList(2, "3"));
    Assert.assertEquals(result, compositeKey);
  }

  @Test
  public void testCreateValueCompositeKeyNullListParam() {
    final Object result = simpleKeyIndexDefinition.createValue(Arrays.asList((Object) null));

    Assert.assertNull(result);
  }

  @Test
  public void testNullParamListItem() {
    final Object result = simpleKeyIndexDefinition.createValue(Arrays.asList("2", null));

    Assert.assertNull(result);
  }

  @Test(expected = NumberFormatException.class)
  public void testWrongParamTypeListItem() {
    simpleKeyIndexDefinition.createValue(Arrays.asList("a", "3"));
  }

  @Test
  public void testCreateValueCompositeKey() {
    final Object result = simpleKeyIndexDefinition.createValue("2", "3");

    final OCompositeKey compositeKey = new OCompositeKey(Arrays.asList(2, "3"));
    Assert.assertEquals(result, compositeKey);
  }

  @Test
  public void testCreateValueCompositeKeyNullParamList() {
    final Object result = simpleKeyIndexDefinition.createValue((List<?>) null);

    Assert.assertNull(result);
  }

  @Test
  public void testCreateValueCompositeKeyNullParam() {
    final Object result = simpleKeyIndexDefinition.createValue((Object) null);

    Assert.assertNull(result);
  }

  @Test
  public void testCreateValueCompositeKeyEmptyList() {
    final Object result = simpleKeyIndexDefinition.createValue(Collections.emptyList());

    Assert.assertNull(result);
  }

  @Test
  public void testNullParamItem() {
    final Object result = simpleKeyIndexDefinition.createValue("2", null);

    Assert.assertNull(result);
  }

  @Test(expected = NumberFormatException.class)
  public void testWrongParamType() {
    simpleKeyIndexDefinition.createValue("a", "3");
  }

  @Test
  public void testParamCount() {
    Assert.assertEquals(simpleKeyIndexDefinition.getParamCount(), 2);
  }

  @Test
  public void testParamCountOneItem() {
    final OSimpleKeyIndexDefinition keyIndexDefinition =
        new OSimpleKeyIndexDefinition(OType.INTEGER);

    Assert.assertEquals(keyIndexDefinition.getParamCount(), 1);
  }

  @Test
  public void testGetKeyTypes() {
    assertArrayEquals(
        simpleKeyIndexDefinition.getTypes(), new OType[] {OType.INTEGER, OType.STRING});
  }

  @Test
  public void testGetKeyTypesOneType() {
    final OSimpleKeyIndexDefinition keyIndexDefinition =
        new OSimpleKeyIndexDefinition(OType.BOOLEAN);

    assertArrayEquals(keyIndexDefinition.getTypes(), new OType[] {OType.BOOLEAN});
  }

  @Test
  public void testReload() {
    OrientDB orientdb = new OrientDB("memory:", OrientDBConfig.defaultConfig());
    orientdb.execute(
        "create database osimplekeyindexdefinitiontest memory users(admin identified by 'adminpwd'"
            + " role admin)");

    try (ODatabaseDocument db =
        orientdb.open("osimplekeyindexdefinitiontest", "admin", "adminpwd")) {

      final ODocument storeDocument = simpleKeyIndexDefinition.toStream();
      storeDocument.save(db.getClusterNameById(db.getDefaultClusterId()));

      final ODocument loadDocument = db.load(storeDocument.getIdentity());
      final OSimpleKeyIndexDefinition loadedKeyIndexDefinition = new OSimpleKeyIndexDefinition();
      loadedKeyIndexDefinition.fromStream(loadDocument);
      Assert.assertEquals(loadedKeyIndexDefinition, simpleKeyIndexDefinition);
    }
    orientdb.close();
  }

  @Test(expected = OIndexException.class)
  public void testGetDocumentValueToIndex() {
    simpleKeyIndexDefinition.getDocumentValueToIndex(new ODocument());
  }
}
