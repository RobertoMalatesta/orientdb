package com.orientechnologies.website.repository;

import com.orientechnologies.website.hateoas.Page;
import com.orientechnologies.website.model.schema.dto.*;

import java.util.List;

public interface OrganizationRepository extends BaseRepository<Organization> {

  public Organization findOneByName(String name);

  public List<Issue> findOrganizationIssues(String name, String q, String page, String perPage);

  public Page<Issue> findOrganizationIssuesPagedProfiled(String name, String q, String page, String perPage);

  public List<Repository> findOrganizationRepositories(String name);

  public List<Client> findClients(String name);

  public List<Priority> findPriorities(String name);

  public Priority findPriorityByNumber(String name, Integer number);

  public List<Scope> findScopes(String name);

  public Client findClient(String name, Integer clientId);

  public Repository findOrganizationRepository(String name, String repo);

  public Repository findOrganizationRepositoryByScope(String name, Integer scope);

  public Issue findSingleOrganizationIssueByRepoAndNumber(String name, String repo, Long number);

  public Issue findSingleOrganizationIssueByNumber(String name, Long number);

  public List<Comment> findSingleOrganizationIssueCommentByRepoAndNumber(String owner, String repo, Long number);

  public List<Event> findEventsByOwnerRepoAndIssueNumber(String owner, String repo, Long number);

  public List<OUser> findClientMembers(String org, Integer clientId);

  public Environment findClientEnvironmentById(String org, Integer clientId, String env);

  public List<Environment> findClientEnvironments(String org, Integer clientId);

  public List<OUser> findTeamMembers(String owner, String repo);

  public Milestone findMilestoneByOwnerRepoAndNumberIssueAndNumberMilestone(String owner, String repo, Integer iNumber,
      Integer mNumber);

  public List<Milestone> findRepoMilestones(String owner, String repo);

  public List<Label> findRepoLabels(String owner, String repo);

  List<OUser> findMembers(String name);

  List<Milestone> findMilestones(String name);

  List<Label> findLabels(String name);

  List<Sla> findClientEnvironmentSla(String organizationName, Integer clientId, String env);

}
