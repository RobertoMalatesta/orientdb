package com.orientechnologies.website.services.impl;

import com.orientechnologies.orient.core.db.document.ODatabaseDocumentTx;
import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.orient.core.metadata.schema.OClass;
import com.orientechnologies.orient.core.sql.OCommandSQL;
import com.orientechnologies.website.OrientDBFactory;
import com.orientechnologies.website.events.ChatMessageEditEvent;
import com.orientechnologies.website.events.ChatMessageSentEvent;
import com.orientechnologies.website.events.EventManager;
import com.orientechnologies.website.exception.ServiceException;
import com.orientechnologies.website.github.GIssueState;
import com.orientechnologies.website.github.GOrganization;
import com.orientechnologies.website.github.GRepo;
import com.orientechnologies.website.github.GitHub;
import com.orientechnologies.website.helper.SecurityHelper;
import com.orientechnologies.website.model.schema.*;
import com.orientechnologies.website.model.schema.dto.*;
import com.orientechnologies.website.model.schema.dto.OUser;
import com.orientechnologies.website.model.schema.dto.web.ImportDTO;
import com.orientechnologies.website.model.schema.dto.web.hateoas.ScopeDTO;
import com.orientechnologies.website.repository.*;
import com.orientechnologies.website.security.DeveloperAuthentication;
import com.orientechnologies.website.services.OrganizationService;
import com.orientechnologies.website.services.RepositoryService;
import com.orientechnologies.website.services.reactor.GitHubIssueImporter;
import com.orientechnologies.website.services.reactor.ReactorMSG;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Reactor;
import reactor.event.Event;

import java.io.IOException;
import java.util.*;

/**
 * Created by Enrico Risa on 17/10/14.
 */
@Service
public class OrganizationServiceImpl implements OrganizationService {

  @Autowired
  protected EventManager         eventManager;

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
  private ClientRepository       clientRepository;

  @Autowired
  private EnvironmentRepository  environmentRepository;

  @Autowired
  private ScopeRepository        scopeRepository;

  @Autowired
  private SlaRepository          slaRepository;

  @Autowired
  private Reactor                reactor;

  @Autowired
  private MessageRepository      messageRepository;

  @Autowired
  private ContractRepository     contractRepository;

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

        boolean isMember = gOrganization.hasMember(username);
        if (isMember) {
          OUser developer = userRepository.findUserByLogin(username);
          if (developer == null) {
            developer = new OUser(username, null, null);
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

  @Transactional
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

  @Transactional
  @Override
  public Client registerClient(String org, Client client) {
    Organization organization = organizationRepository.findOneByName(org);
    if (organization != null) {
      client = clientRepository.save(client);
      createClientRelationship(organization, client);
      return client;
    } else {
      throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
    }
  }

  @Override
  public OUser addMemberClient(String org, Integer clientId, String username) {
    Client client = organizationRepository.findClient(org, clientId);

    if (client != null) {
      OUser developer = userRepository.findUserByLogin(username);
      if (developer == null) {
        developer = new OUser(username, null, null);
        developer = userRepository.save(developer);
      }
      createClientMembership(client, developer);
      return developer;
    } else {
      throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Client not Found");
    }
  }

  @Override
  public Repository registerRepository(String org, String repo, ImportDTO importRules) {

    Organization organization = organizationRepository.findOneByName(org);
    if (organization != null) {

      Authentication auth = SecurityContextHolder.getContext().getAuthentication();
      DeveloperAuthentication developerAuthentication = (DeveloperAuthentication) auth;
      String token = developerAuthentication.getGithubToken();

      try {

        Repository r = null;
        GitHub github = new GitHub(token);

        GRepo repository = github.repo(org + '/' + repo);

        r = repoRepository.findByOrgAndName(org, repo);
        if (r == null) {
          r = repositoryService.createRepo(repository.getName(), repository.getDescription());
          createHasRepoRelationship(organization, r);
          dbFactory.getGraph().commit();
        }
        GitHubIssueImporter.GitHubIssueMessage gitHubIssueMessage = new GitHubIssueImporter.GitHubIssueMessage(repository);
        if (importRules.getState() != null) {
          gitHubIssueMessage.setState(GIssueState.valueOf(importRules.getState().toUpperCase()));
        }
        if (importRules.getIssues() != null && !importRules.getIssues().isEmpty()) {
          gitHubIssueMessage.setIssues(importRules.getIssues());
        }

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

  @Override
  public Scope registerScope(String name, ScopeDTO scope, Integer id) {
    Organization organization = organizationRepository.findOneByName(name);
    if (organization != null) {
      Repository r = repoRepository.findByOrgAndName(name, scope.getRepository());
      boolean isNew = false;
      Scope sc = null;
      if (id != null) {
        sc = repoRepository.findScope(r.getName(), id);
        if (sc == null) {
          throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Scope not Found");
        }
      } else {
        isNew = true;
        sc = new Scope();
      }
      sc.setName(scope.getName());
      sc = scopeRepository.save(sc);
      OUser owner = userRepository.findUserByLogin(scope.getOwner());
      createOwnerScopeRelationship(sc, owner);
      List<OUser> members = new ArrayList<OUser>();
      for (String s : scope.getMembers()) {
        OUser m = userRepository.findUserByLogin(s);
        members.add(m);
      }
      createMembersScopeRelationship(sc, members);
      if (isNew)
        createRepoScopeRelationship(r, sc);

      return scopeRepository.load(sc);
    } else {
      throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
    }
  }

  @Override
  public void registerRoom(String name, Integer clientId) {
    Client client = organizationRepository.findClient(name, clientId);
    if (client != null) {
      OrientGraph graph = dbFactory.getGraph();

      ODatabaseDocumentTx tx = graph.getRawGraph();

      tx.commit();
      OClass chat = tx.getMetadata().getSchema().getOrCreateClass("Chat");
      OClass clientChat = tx.getMetadata().getSchema().getClass(("Chat" + clientId));

      if (clientChat == null) {
        clientChat = tx.getMetadata().getSchema().createClass(("Chat" + clientId));
        clientChat.setSuperClass(chat);
      }
    }
  }

  @Override
  public Message registerMessage(String name, Integer clientId, Message message) {

    Client client = organizationRepository.findClient(name, clientId);
    if (client != null) {
      message.setClientId(clientId);
      message.setDate(new Date());
      message.setSender(SecurityHelper.currentUser());
      message.setUuid(UUID.randomUUID().toString());
      eventManager.pushInternalEvent(ChatMessageSentEvent.EVENT, message);
      message = messageRepository.saveAndCommit(message);
      return message;
    }
    return null;
  }

  @Override
  public Message patchMessage(String name, Integer clientId, String messageId, Message message) {
    Message saved = messageRepository.findById(messageId);
    saved.setClientId(clientId);
    saved.setBody(message.getBody());
    eventManager.pushInternalEvent(ChatMessageEditEvent.EVENT, saved);
    message = messageRepository.save(saved);
    return message;
  }

  @Override
  public OUser registerBot(String name, String username) {

    Organization organization = organizationRepository.findOneByName(name);
    OUser developer = userRepository.findUserByLogin(username);

    if (organization != null && developer != null) {
      createBotORgRelationship(organization, developer);
      return developer;
    }
    return null;
  }

  @Override
  public Contract registerContract(String name, Contract contract) {

    Organization organization = organizationRepository.findOneByName(name);

    if (organization != null) {
      contract = contractRepository.save(contract);
      createContractRelationship(organization, contract);
    }
    return null;
  }

  private void createContractRelationship(Organization organization, Contract contract) {

    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(organization.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(contract.getId()));
    orgVertex.addEdge(HasContract.class.getSimpleName(), devVertex);
  }

  @Override
  public void checkInRoom(String name, Integer clientId) {

    final Client client = organizationRepository.findClient(name, clientId);
    OrientGraph graph = dbFactory.getGraph();
    final OUser user = SecurityHelper.currentUser();
    Map<String, Object> params = new HashMap<String, Object>() {
      {
        put("user", user.getRid());
        put("room", client.getId());
        put("timestamp", new Date());
      }
    };
    graph
        .command(
            new OCommandSQL(
                "update ChatLog SET user=:user, room=:room, timestamp=:timestamp , notified=false UPSERT WHERE user=:user and room =:room"))
        .execute(params);

  }

  @Override
  public List<Message> getClientRoomMessage(String name, Integer clientId, String beforeUuid) {
    return organizationRepository.findClientMessages(name, clientId, beforeUuid);
  }

  @Override
  public List<OUser> getClientRoomActors(String name, Integer clientId) {
    List<OUser> users = organizationRepository.findClientMembers(name, clientId);
    users.addAll(organizationRepository.findMembers(name));
    return users;
  }

  private void createMembersScopeRelationship(Scope scope, List<OUser> members) {
    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = graph.getVertex(new ORecordId(scope.getId()));
    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasMember.class.getSimpleName())) {
      edge.remove();
    }
    for (OUser member : members) {
      OrientVertex devVertex = graph.getVertex(new ORecordId(member.getRid()));
      orgVertex.addEdge(HasMember.class.getSimpleName(), devVertex);
    }

  }

  private void createBotORgRelationship(Organization organization, OUser bot) {
    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = graph.getVertex(new ORecordId(organization.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(bot.getRid()));
    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasBot.class.getSimpleName())) {
      edge.remove();
    }
    orgVertex.addEdge(HasBot.class.getSimpleName(), devVertex);
  }

  private void createOwnerScopeRelationship(Scope scope, OUser owner) {
    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = graph.getVertex(new ORecordId(scope.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(owner.getRid()));
    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasOwner.class.getSimpleName())) {
      edge.remove();
    }
    orgVertex.addEdge(HasOwner.class.getSimpleName(), devVertex);
  }

  // @Override
  // public Environment registerClientEnvironment(String name, Integer id, Environment environment) {
  // Organization organization = organizationRepository.findOneByName(name);
  // Client client = organizationRepository.findClient(name, id);
  // if (organization != null) {
  // environment = environmentRepository.save(environment);
  // createClientEnvironmentRelationship(client, environment);
  // return environment;
  // } else {
  // throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
  // }
  // }

  // @Override
  // public Sla registerClientSlaToEnvironment(String name, Integer id, String env, Sla sla) {
  // Organization organization = organizationRepository.findOneByName(name);
  // Client client = organizationRepository.findClient(name, id);
  // Environment e = organizationRepository.findClientEnvironmentById(name, id, env);
  // if (organization != null && client != null && e != null) {
  // sla = slaRepository.save(sla);
  // createEnvironmentSlaRelationship(e, sla);
  // return sla;
  // } else {
  // throw ServiceException.create(HttpStatus.NOT_FOUND.value()).withMessage("Organization not Found");
  // }
  // }

  private void createRepoScopeRelationship(Repository repo, Scope scope) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(repo.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(scope.getId()));
    orgVertex.addEdge(HasScope.class.getSimpleName(), devVertex);
  }

  private void createEnvironmentSlaRelationship(Environment environment, Sla sla) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(environment.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(sla.getId()));
    orgVertex.addEdge(HasSla.class.getSimpleName(), devVertex);
  }

  // private void createClientEnvironmentRelationship(Client client, Environment environment) {
  //
  // OrientGraph graph = dbFactory.getGraph();
  //
  // OrientVertex orgVertex = graph.getVertex(new ORecordId(client.getId()));
  // OrientVertex devVertex = graph.getVertex(new ORecordId(environment.getId()));
  // orgVertex.addEdge(HasEnvironment.class.getSimpleName(), devVertex);
  // }

  public void createMembership(Organization organization, OUser user) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(organization.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(user.getRid()));

    orgVertex.addEdge(HasMember.class.getSimpleName(), devVertex);

  }

  private void createClientRelationship(Organization organization, Client client) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(organization.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(client.getId()));

    orgVertex.addEdge(HasClient.class.getSimpleName(), devVertex);

  }

  private void createClientMembership(Client client, OUser user) {
    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(client.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(user.getRid()));

    orgVertex.addEdge(HasMember.class.getSimpleName(), devVertex);

  }

  public void createHasRepoRelationship(Organization organization, Repository repository) {

    OrientGraph graph = dbFactory.getGraph();

    OrientVertex orgVertex = graph.getVertex(new ORecordId(organization.getId()));
    OrientVertex devVertex = graph.getVertex(new ORecordId(repository.getId()));
    orgVertex.addEdge(HasRepo.class.getSimpleName(), devVertex);
  }
}
