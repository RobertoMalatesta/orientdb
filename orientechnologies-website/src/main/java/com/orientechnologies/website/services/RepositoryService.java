package com.orientechnologies.website.services;

import com.orientechnologies.website.model.schema.dto.Issue;
import com.orientechnologies.website.model.schema.dto.Label;
import com.orientechnologies.website.model.schema.dto.Milestone;
import com.orientechnologies.website.model.schema.dto.Repository;
import com.orientechnologies.website.model.schema.dto.web.IssueDTO;

public interface RepositoryService {

  public Repository createRepo(String name, String description);

  public void createIssue(Repository repo, Issue issue);

  public Issue openIssue(Repository repository, IssueDTO issue);

  public Issue patchIssue(Issue original, IssueDTO patch);

  public void addLabel(Repository repo, Label label);

  public void addMilestone(Repository repoDtp, Milestone m);
}
