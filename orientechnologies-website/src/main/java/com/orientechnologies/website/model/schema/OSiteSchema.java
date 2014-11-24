package com.orientechnologies.website.model.schema;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.metadata.schema.OSchema;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.website.model.schema.dto.*;

import java.util.*;

public class OSiteSchema {

  private static Map<Class<?>, EnumSet<?>> schemas      = new LinkedHashMap<Class<?>, EnumSet<?>>();

  private static List<Class<?>>            vertices     = new ArrayList<Class<?>>();
  private static Map<Class, String>        names        = new HashMap<Class, String>();
  private static Map<Class, Class>         superClasses = new HashMap<Class, Class>();

  private static List<Class<?>>            edges        = new ArrayList<Class<?>>();
  static {
    addVertexClass(OLabel.class, Label.class.getSimpleName());
    addVertexClass(OEvent.class, Event.class.getSimpleName());
    addVertexClass(OComment.class, Comment.class.getSimpleName(), OEvent.class);
    addVertexClass(OIssueEvent.class, IssueEvent.class.getSimpleName(), OEvent.class);
    addVertexClass(OIssueEventInternal.class, IssueEventInternal.class.getSimpleName(), OIssueEvent.class);
    addVertexClass(OIssue.class, Issue.class.getSimpleName());
    addVertexClass(OPriority.class, Priority.class.getSimpleName());
    addVertexClass(OScope.class, Scope.class.getSimpleName());
    addVertexClass(OClient.class, Client.class.getSimpleName());
    addVertexClass(ORepository.class, Repository.class.getSimpleName());
    addVertexClass(OMilestone.class, Milestone.class.getSimpleName());
    addVertexClass(OOrganization.class, Organization.class.getSimpleName());

    addEdgeClass(HasLabel.class);
    addEdgeClass(HasEvent.class);
    addEdgeClass(HasIssue.class);
    addEdgeClass(HasMember.class);
    addEdgeClass(HasRepo.class);
    addEdgeClass(HasClient.class);
    addEdgeClass(HasPriority.class);
    addEdgeClass(HasScope.class);
  }

  public static void addVertexClass(Class cls, String name, Class superClass) {

    names.put(cls, name);
    if (superClass != null) {
      superClasses.put(cls, superClass);
    }
    vertices.add(cls);
    addSchemaClass(cls);
  }

  public static void addVertexClass(Class cls, String name) {

    addVertexClass(cls, name, null);
  }

  public static void addSchemaClass(Class cls) {

    schemas.put(cls, EnumSet.allOf(cls));

  }

  public static void addEdgeClass(Class cls) {
    edges.add(cls);
    addSchemaClass(cls);
  }

  public static void createSchema(ODatabaseDocumentTx db) {

    if (!db.exists()) {
      db.create();
      fillSchema(db);
    }

  }

  public static void fillSchema(ODatabaseDocumentTx db) {
    OSchema schema = db.getMetadata().getSchema();

    OClass v = schema.getClass("V");
    OClass e = schema.getClass("E");
    OClass identity = schema.getClass("OIdentity");
    identity.setSuperClass(v);
    Map<Class, OClass> created = new HashMap<Class, OClass>();
    for (Class<?> clazz : schemas.keySet()) {
      OClass cls;
      if (vertices.contains(clazz)) {
        cls = schema.createClass(names.get(clazz));
        created.put(clazz, cls);
        Class superC = superClasses.get(clazz);
        if (superC != null) {
          OClass s = created.get(superC);
          if (s != null) {
            cls.setSuperClass(s);
          } else {
            cls.setSuperClass(v);
          }
        } else {
          cls.setSuperClass(v);
        }
      } else {
        cls = schema.createClass(clazz);
        cls.setSuperClass(e);
      }
      if (OTypeHolder.class.isAssignableFrom(clazz)) {
        for (Enum<?> anEnum : schemas.get(clazz)) {
          OType t = ((OTypeHolder) anEnum).getType();
          cls.createProperty(anEnum.toString(), t);
        }
      }
    }

    OSiteSchemaPopupator.populateData(db);
  }
  // Edges

}
