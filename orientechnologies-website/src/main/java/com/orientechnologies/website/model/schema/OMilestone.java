package com.orientechnologies.website.model.schema;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OType;
import com.orientechnologies.orient.core.record.impl.ODocument;
import com.orientechnologies.website.model.schema.dto.Comment;
import com.orientechnologies.website.model.schema.dto.Milestone;
import com.tinkerpop.blueprints.impls.orient.OrientBaseGraph;

import java.util.Date;

/**
 * Created by Enrico Risa on 04/11/14.
 */
public enum OMilestone implements OTypeHolder<Milestone> {
  NUMBER("number") {
    @Override
    public OType getType() {
      return OType.INTEGER;
    }
  },
  TITLE("title") {
    @Override
    public OType getType() {
      return OType.INTEGER;
    }
  },
  CREATOR("creator") {
    @Override
    public OType getType() {
      return OType.LINK;
    }
  },
  DESCRIPTION("description") {
    @Override
    public OType getType() {
      return OType.INTEGER;
    }
  },
  STATE("state") {
    @Override
    public OType getType() {
      return OType.INTEGER;
    }
  },
  CREATED_AT("createdAt") {
    @Override
    public OType getType() {
      return OType.DATETIME;
    }
  },
  UPDATED_AT("updatedAt") {
    @Override
    public OType getType() {
      return OType.DATETIME;
    }
  },
  DUE_ON("dueOn") {
    @Override
    public OType getType() {
      return OType.DATETIME;
    }
  };

  private String name;

  OMilestone(String name) {
    this.name = name;
  }

  @Override
  public String toString() {
    return name;
  }

  @Override
  public Milestone fromDoc(ODocument doc, OrientBaseGraph graph) {

    Milestone milestone = new Milestone();
    milestone.setId(doc.getIdentity().toString());
    milestone.setNumber((Integer) doc.field(NUMBER.toString()));
    milestone.setTitle((String) doc.field(TITLE.toString()));
    milestone.setDescription((String) doc.field(DESCRIPTION.toString()));
    milestone.setState((String) doc.field(STATE.toString()));
    milestone.setCreatedAt((Date) doc.field(CREATED_AT.toString()));
    milestone.setDueOn((Date) doc.field(DUE_ON.toString()));
    milestone.setCreator(OUser.NAME.fromDoc((ODocument) doc.field(CREATOR.toString()), graph));
    return milestone;

  }

  @Override
  public ODocument toDoc(Milestone entity, OrientBaseGraph graph) {

    ODocument doc;
    if (entity.getId() == null) {
      doc = new ODocument(entity.getClass().getSimpleName());
    } else {
      doc = graph.getRawGraph().load(new ORecordId(entity.getId()));
    }
    doc.field(NUMBER.toString(), entity.getNumber());
    doc.field(TITLE.toString(), entity.getTitle());
    doc.field(STATE.toString(), entity.getTitle());
    doc.field(DESCRIPTION.toString(), entity.getDescription());
    doc.field(CREATED_AT.toString(), entity.getCreatedAt());
    doc.field(UPDATED_AT.toString(), entity.getUpdatedAt());
    doc.field(DUE_ON.toString(), entity.getDueOn());
    doc.field(CREATOR.toString(), (entity.getCreator() != null ? new ORecordId(entity.getCreator().getId()) : null));
    return doc;
  }
}
