package com.orientechnologies.website.services.impl;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.website.OrientDBFactory;
import com.orientechnologies.website.exception.ServiceException;
import com.orientechnologies.website.github.GOrganization;
import com.orientechnologies.website.github.GRepo;
import com.orientechnologies.website.github.GitHub;
import com.orientechnologies.website.model.schema.HasMember;
import com.orientechnologies.website.model.schema.HasRepo;
import com.orientechnologies.website.model.schema.dto.User;
import com.orientechnologies.website.model.schema.dto.Organization;
import com.orientechnologies.website.model.schema.dto.Repository;
import com.orientechnologies.website.repository.OrganizationRepository;
import com.orientechnologies.website.repository.RepositoryRepository;
import com.orientechnologies.website.repository.UserRepository;
import com.orientechnologies.website.security.DeveloperAuthentication;
import com.orientechnologies.website.services.OrganizationService;
import com.orientechnologies.website.services.RepositoryService;
import com.orientechnologies.website.services.reactor.GitHubIssueImporter;
import com.orientechnologies.website.services.reactor.ReactorMSG;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.Reactor;
import reactor.event.Event;

import java.io.IOException;

/**
 * Created by Enrico Risa on 17/10/14.
 */
@Service
public class OrganizationServiceImpl implements OrganizationService {

  @Autowired
  private OrientDBFactory        dbFactory;

  @Autowired
  private OrganizationRepository organizationRepository;

  @Autowired
  private UserRepository         userRepository;

  @Autowired
  private RepositoryService      repositoryService;

  @Autowired
  private RepositoryRepository   repoRepository;

  @Autowired
  private Reactor                reactor;

  @Override
  public void addMember(String org, String username) throws ServiceException {

    Organization organization = organizationRepository.findOneByName(org);
    if (organization != null) {
      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      DeveloperAuthentication developerAuthentication = (DeveloperAuthentication) auth;
      String token = developerAuthentication.getGithubToken();

      try {

        GitHub gitHub = new GitHub(token);

        GOrganization gOrganization = gitHub.organization(org);
        // GitHub github = GitHub.connectUsingOAuth(token);
        // GHUser user = github.getUser(username);
        // GHOrganization ghOrganization = github.getOrganization(org);
        // boolean isMember = ghOrganization.hasMember(user);
        boolean isMember = gOrganization.hasMember(username);
        if (isMember) {
          User developer = userRepository.findUserByLogin(username);
          if (developer == null) {
            developer = new User(username, null, null);
            developer = userRepository.save(developer);
          }
          createMembership(organization, developer);
        } else {
          throw ServiceException.create(HttpStatus.NOT_FOUND.value())
              .withMessage("Organization %s has no member %s", org, username);
        }

      } catch (IOException e) {
        e.printStackTrace();
      }

    } else {
      throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
    }

  }

  @Override
  public void registerOrganization(String name) throws ServiceException {

    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    DeveloperAuthentication developerAuthentication = (DeveloperAuthentication) auth;

    String token = developerAuthentication.getGithubToken();

    if (token == null) {
      throw ServiceException.create(HttpStatus.FORBIDDEN.value());
    }

    // TODO Check if the username is the owner of the repo. there is no clean way to do that now
    // TODO see http://stackoverflow.com/questions/20144295/github-api-v3-determine-if-user-is-an-owner-of-an-organization

    try {
      GitHub github = new GitHub(token);
      GOrganization organization = github.organization(name);
      Organization org = createOrganization(organization.getLogin(), organization.getName());
      createMembership(org, developerAuthentication.getUser());

    } catch (IOException e) {
      e.printStackTrace();
    }

  }

  @Override
  public Repository registerRepository(String org, String repo) {

    Organization organization = organizationRepository.findOneByName(org);
    if (organization != null) {

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      DeveloperAuthentication developerAuthentication = (DeveloperAuthentication) auth;
      String token = developerAuthentication.getGithubToken();

      try {

        Repository r = null;
        GitHub github = new GitHub(token);

        GRepo repository = github.repo(org + '/' + repo);
        // Github gu = new RtGithub(token);
        // Repo repo1 = gu.repos().get(new Coordinates.Simple(org + '/' + repo));

        // GHRepository repository = github.getRepository(org + '/' + repo);

        r = repoRepository.findByOrgAndName(org, repo);
        if (r == null) {
          r = repositoryService.createRepo(repository.getName(), repository.getDescription());
          createHasRepoRelationship(organization, r);
          dbFactory.getGraph().commit();
        }
        GitHubIssueImporter.GitHubIssueMessage gitHubIssueMessage = new GitHubIssueImporter.GitHubIssueMessage(repository);

        reactor.notify(ReactorMSG.ISSUE_IMPORT, Event.wrap(gitHubIssueMessage));
        return r;
      } catch (IOException e) {
        e.printStackTrace();
      } finally {

      }
      return null;
    } else {
      throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
    }
  }

  @Override
  public Organization createOrganization(String name, String description) {

    Organization org = new Organization();
    org.setName(name);
    return organizationRepository.save(org);
  }

  private void createMembership(Organization organization, User user) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(organization.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(user.getId()));

    orgVertex.addEdge(HasMember.class.getSimpleName(), devVertex);

  }

  private void createHasRepoRelationship(Organization organization, Repository repository) {

    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(organization.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(repository.getId()));
    orgVertex.addEdge(HasRepo.class.getSimpleName(), devVertex);
  }
}
