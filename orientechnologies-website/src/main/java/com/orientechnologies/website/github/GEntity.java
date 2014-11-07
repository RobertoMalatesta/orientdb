package com.orientechnologies.website.github;

import com.orientechnologies.orient.core.record.impl.ODocument;

import java.text.ParseException;
import java.util.Date;

/**
 * Created by Enrico Risa on 05/11/14.
 */
public abstract class GEntity {

  protected final GEntity owner;
  protected ODocument     _local;
  protected GitHub        github;

  protected GEntity(GitHub github, GEntity owner, String content) {
    fromJson(content);
    this.github = github;
    this.owner = owner;
  }

  public void fromJson(String json) {
    _local = new ODocument().fromJSON(json, "noMap");
  }

  protected abstract String getBaseUrl();

  protected <RET> RET get(String fieldName) {
    return _local.field(fieldName);
  }

  protected Date toDate(String date) {
    try {
      return date != null ? GitHub.format().parse(date) : null;
    } catch (ParseException e) {
      e.printStackTrace();
    }
    return null;
  }

  protected GUser toUser(ODocument doc) {
    return doc != null ? new GUser(github, this, doc.toJSON()) : null;
  }


}
