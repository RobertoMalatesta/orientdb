package com.orientechnologies.website.controllers;

import com.orientechnologies.website.configuration.ApiVersion;
import com.orientechnologies.website.hateoas.Page;
import com.orientechnologies.website.hateoas.assembler.IssueAssembler;
import com.orientechnologies.website.hateoas.assembler.PagedResourceAssembler;
import com.orientechnologies.website.hateoas.assembler.TopicAssembler;
import com.orientechnologies.website.helper.SecurityHelper;
import com.orientechnologies.website.model.schema.dto.*;
import com.orientechnologies.website.model.schema.dto.web.ImportDTO;
import com.orientechnologies.website.model.schema.dto.web.IssueDTO;
import com.orientechnologies.website.model.schema.dto.web.hateoas.IssueResource;
import com.orientechnologies.website.model.schema.dto.web.hateoas.ScopeDTO;
import com.orientechnologies.website.model.schema.dto.web.hateoas.TopicResource;
import com.orientechnologies.website.repository.OrganizationRepository;
import com.orientechnologies.website.repository.RepositoryRepository;
import com.orientechnologies.website.repository.TagRepository;
import com.orientechnologies.website.repository.TopicRepository;
import com.orientechnologies.website.security.OSecurityManager;
import com.orientechnologies.website.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.hateoas.PagedResources;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@EnableAutoConfiguration
@RequestMapping(ApiUrls.ORGS_V1)
@ApiVersion(1)
public class OrganizationController extends ExceptionController {

  @Autowired
  private OrganizationRepository orgRepository;

  @Autowired
  private OrganizationService    organizationService;

  @Autowired
  private RepositoryRepository   repoRepository;

  @Autowired
  private RepositoryService      repositoryService;

  @Autowired
  private IssueAssembler         issueAssembler;

  @Autowired
  private TopicAssembler         topicAssembler;

  @Autowired
  private PagedResourceAssembler pagedResourceAssembler;

  @Autowired
  private UserService            userService;

  @Autowired
  private TopicService           topicService;

  @Autowired
  private TopicRepository        topicRepository;

  @Autowired
  private TagRepository          tagRepository;

  @Autowired
  private TagService             tagService;

  @Autowired
  private OSecurityManager       securityManager;

  @RequestMapping(value = "{name}", method = RequestMethod.GET)
  public ResponseEntity<Organization> getOrganizationInfo(@PathVariable("name") String name) {

    Organization organization = orgRepository.findOneByName(name);
    if (organization == null) {
      return new ResponseEntity<Organization>(HttpStatus.NOT_FOUND);
    } else {
      return new ResponseEntity<Organization>(organization, HttpStatus.OK);
    }
  }

  @RequestMapping(value = "{name}/issues", method = RequestMethod.GET)
  public ResponseEntity<PagedResources<IssueResource>> getOrganizationIssuesPaged(@PathVariable("name") String name,
      @RequestParam(value = "q", defaultValue = "") String q, @RequestParam(value = "page", defaultValue = "1") String page,
      @RequestParam(value = "per_page", defaultValue = "10") String perPage) {

    Page<Issue> issues = orgRepository.findOrganizationIssuesPagedProfiled(name, q, page, perPage);
    return new ResponseEntity<PagedResources<IssueResource>>(pagedResourceAssembler.toResource(issues, issueAssembler),
        HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/board/issues", method = RequestMethod.GET)
  public ResponseEntity<PagedResources<IssueResource>> getBoardIssues(@PathVariable("name") String name,
      @RequestParam(value = "q", defaultValue = "") String q, @RequestParam(value = "page", defaultValue = "1") String page,
      @RequestParam(value = "per_page", defaultValue = "10") String perPage) {
    Page<Issue> issues = orgRepository.findOrganizationIssuesPagedProfiled(name, q, page, perPage);
    return new ResponseEntity<PagedResources<IssueResource>>(pagedResourceAssembler.toResource(issues, issueAssembler),
        HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/issues/{number}", method = RequestMethod.GET)
  public ResponseEntity<Issue> getOrganizationSingleIssue(@PathVariable("name") String organization,
      @PathVariable("number") Long number) {

    Issue issue = orgRepository.findSingleOrganizationIssueByNumber(organization, number);

    OUser user = SecurityHelper.currentUser();
    if (Boolean.TRUE.equals(issue.getConfidential())) {

      if (!userService.isMember(user, organization) && !userService.isSupport(user, organization)) {

        Client client = issue.getClient();
        Client currentClient = userService.getClient(user, organization);
        if (currentClient == null || client == null || client.getClientId() != currentClient.getClientId()) {
          return new ResponseEntity<Issue>(HttpStatus.NOT_FOUND);
        }
      }

    }
    userService.profileIssue(user, issue, organization);
    return issue != null ? new ResponseEntity<Issue>(issue, HttpStatus.OK) : new ResponseEntity<Issue>(HttpStatus.NOT_FOUND);

  }

  @RequestMapping(value = "{name}/repos", method = RequestMethod.GET)
  public ResponseEntity<List<Repository>> getOrganizationRepositories(@PathVariable("name") String name) {
    return new ResponseEntity<List<Repository>>(orgRepository.findOrganizationRepositories(name), HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/members/{username}", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public void addMemberToOrg(@PathVariable("name") String name, @PathVariable("username") String username) {

    organizationService.addMember(name, username);
  }

  @RequestMapping(value = "{name}/members", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<OUser> findMembers(@PathVariable("name") String name) {
    return orgRepository.findMembers(name);
  }

  @RequestMapping(value = "{name}/milestones", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Milestone> findMilestones(@PathVariable("name") String name) {
    return orgRepository.findMilestones(name);
  }

  @RequestMapping(value = "{name}/milestones/current", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Milestone> findCurrentMilestones(@PathVariable("name") String name) {
    return orgRepository.findCurrentMilestones(name);
  }

  @RequestMapping(value = "{name}/milestones/{title:.+}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity patchMilestone(@PathVariable("name") String name, @PathVariable("title") String title,
      @RequestBody Milestone milestone) {

    orgRepository.setCurrentMilestones(name, title, milestone.getCurrent());
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/labels", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Label> findLabels(@PathVariable("name") String name) {
    return orgRepository.findLabels(name);
  }

  @RequestMapping(value = "{name}/bots", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<OUser> findBots(@PathVariable("name") String name) {
    return orgRepository.findBots(name);
  }

  @RequestMapping(value = "{name}/contracts", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Contract> findContracts(@PathVariable("name") String name) {
    return orgRepository.findContracts(name);
  }

  @RequestMapping(value = "{name}/contracts", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Contract> addContractToOrg(@PathVariable("name") String name, @RequestBody Contract contract) {
    OUser user = SecurityHelper.currentUser();

    if (userService.isMember(user, name)) {
      Contract c = organizationService.registerContract(name, contract);
      return new ResponseEntity<Contract>(c, HttpStatus.OK);
    } else {
      return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/contracts/{uuid}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Contract> patchContractToOrg(@PathVariable("name") String name, @PathVariable("uuid") String uuid,
      @RequestBody Contract contract) {
    OUser user = SecurityHelper.currentUser();

    if (userService.isMember(user, name)) {
      Contract c = organizationService.patchContract(name, uuid, contract);
      return new ResponseEntity<Contract>(c, HttpStatus.OK);
    } else {
      return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/bots/{username}", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public OUser addBotToOrg(@PathVariable("name") String name, @PathVariable("username") String username) {
    return organizationService.registerBot(name, username);
  }

  @RequestMapping(value = "{name}/clients", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public Client addClientToOrg(@PathVariable("name") String name, @RequestBody Client client) {
    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name)) {
      return organizationService.registerClient(name, client);
    }
    return null;
  }

  @RequestMapping(value = "{name}/clients/{id}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public Client patchClient(@PathVariable("name") String name, @PathVariable("id") Integer clientId, @RequestBody Client client) {
    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name)) {
      return organizationService.patchClient(name, clientId, client);
    }
    return null;
  }

  @RequestMapping(value = "{name}/clients/{id}/room", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity registerRoom(@PathVariable("name") String name, @PathVariable("id") Integer clientId) {
    organizationService.registerRoom(name, clientId);
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/clients/{id}/room", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Message>> getRoomMessages(@PathVariable("name") String name, @PathVariable("id") Integer clientId,
      @RequestParam(value = "before", defaultValue = "") String beforeUuid) {
    OUser user = SecurityHelper.currentUser();
    Client client = userService.getClient(user, name);
    if (userService.isMember(user, name) || (userService.isClient(user, name) && client.getClientId().equals(clientId))) {
      return new ResponseEntity(organizationService.getClientRoomMessage(name, clientId, beforeUuid), HttpStatus.OK);
    } else {
      return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/clients/{id}/room/actors", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<OUser>> getRoomActors(@PathVariable("name") String name, @PathVariable("id") Integer clientId,
      @RequestParam(value = "before", defaultValue = "") String beforeUuid) {
    return new ResponseEntity(organizationService.getClientRoomActors(name, clientId), HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/clients/{id}/room", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Message> registerMessage(@PathVariable("name") String name, @PathVariable("id") Integer clientId,
      @RequestBody Message message) {
    OUser user = SecurityHelper.currentUser();
    Client client = userService.getClient(user, name);
    if (userService.isMember(user, name) || (userService.isClient(user, name) && client.getClientId().equals(clientId))) {
      return new ResponseEntity(organizationService.registerMessage(name, clientId, message), HttpStatus.OK);
    } else {
      return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/clients/{id}/room/{messageId}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Message> patchMessage(@PathVariable("name") String name, @PathVariable("id") Integer clientId,
      @PathVariable("messageId") String messageId, @RequestBody Message message) {
    OUser user = SecurityHelper.currentUser();
    Client client = userService.getClient(user, name);
    if (userService.isMember(user, name) || (userService.isClient(user, name) && client.getClientId().equals(clientId))) {
      return new ResponseEntity(organizationService.patchMessage(name, clientId, messageId, message), HttpStatus.OK);
    } else {
      return new ResponseEntity(null, HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/clients/{id}/room/checkin", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity checkInRoom(@PathVariable("name") String name, @PathVariable("id") Integer clientId) {
    organizationService.checkInRoom(name, clientId);
    return new ResponseEntity(HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/clients/{id}/members/{username}", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public OUser addMemberClientToOrg(@PathVariable("name") String name, @PathVariable("id") Integer id,
      @PathVariable("username") String username) {
    return organizationService.addMemberClient(name, id, username);
  }

  @RequestMapping(value = "{name}/clients", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Client>> findClients(@PathVariable("name") String name) {

    if (securityManager.isCurrentMemberOrSupport(name)) {
      return new ResponseEntity<List<Client>>(orgRepository.findClients(name), HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Client>>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/rooms", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Room>> findRooms(@PathVariable("name") String name) {

    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name) || userService.isClient(user, name)) {
      return new ResponseEntity<List<Room>>(orgRepository.findRooms(name), HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Room>>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/clients/{id}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Client> findClient(@PathVariable("name") String name, @PathVariable("id") Integer id) {
    if (securityManager.isCurrentMember(name)) {
      return new ResponseEntity<Client>(orgRepository.findClient(name, id), HttpStatus.OK);
    } else {
      return new ResponseEntity<Client>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/clients/{id}/members", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<OUser>> findClientMembers(@PathVariable("name") String name, @PathVariable("id") Integer id) {

    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name)) {
      return new ResponseEntity<List<OUser>>(orgRepository.findClientMembers(name, id), HttpStatus.OK);
    } else {
      return new ResponseEntity<List<OUser>>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/clients/{id}/contracts", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Contract>> findClientContracts(@PathVariable("name") String name, @PathVariable("id") Integer id) {

    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name) || userService.isClient(user, name)) {
      return new ResponseEntity<List<Contract>>(orgRepository.findClientContracts(name, id), HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Contract>>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/clients/{id}/contracts", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Contract> postClientContract(@PathVariable("name") String name, @PathVariable("id") Integer id,
      @RequestBody Contract contract) {

    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name) || userService.isClient(user, name)) {
      return new ResponseEntity<Contract>(organizationService.registerClientContract(name, id, contract.getUuid(),
          contract.getFrom(), contract.getTo()), HttpStatus.OK);
    } else {
      return new ResponseEntity<Contract>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/clients/{id}/environments", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Environment>> findClientEnvironments(@PathVariable("name") String name, @PathVariable("id") Integer id) {
    OUser user = SecurityHelper.currentUser();
    if (userService.isMember(user, name)) {
      return new ResponseEntity<List<Environment>>(orgRepository.findClientEnvironments(name, id), HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Environment>>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}", method = RequestMethod.POST)
  public ResponseEntity<Organization> registerOrganization(@PathVariable("name") String name) {
    Organization organization = orgRepository.findOneByName(name);

    if (organization == null) {
      organizationService.registerOrganization(name);
      return new ResponseEntity<Organization>(organization, HttpStatus.OK);
    } else {
      return new ResponseEntity<Organization>(HttpStatus.CONFLICT);
    }
  }

  @RequestMapping(value = "{name}/priorities", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Priority> findPriorities(@PathVariable("name") String name) {
    return orgRepository.findPriorities(name);
  }

  @RequestMapping(value = "{name}/issues", method = RequestMethod.POST)
  public ResponseEntity<Issue> createIssue(@PathVariable("name") String owner, @RequestBody IssueDTO issue) {

    Repository r = orgRepository.findOrganizationRepositoryByScope(owner, issue.getScope());

    return r != null ? new ResponseEntity<Issue>(repositoryService.openIssue(r, issue), HttpStatus.OK) : new ResponseEntity<Issue>(
        HttpStatus.NOT_FOUND);
  }

  @RequestMapping(value = "{name}/scopes", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Scope> findScopes(@PathVariable("name") String name) {
    return orgRepository.findScopes(name);
  }

  @RequestMapping(value = "{name}/repos/{repo}", method = RequestMethod.POST)
  public ResponseEntity<Repository> registerRepository(@PathVariable("name") String name, @PathVariable("repo") String repo,
      @RequestBody ImportDTO importRules) {

    Repository rep = organizationService.registerRepository(name, repo, importRules);
    return new ResponseEntity<Repository>(rep, HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/scopes", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public Scope registerScope(@PathVariable("name") String name, @RequestBody ScopeDTO scope) {
    return organizationService.registerScope(name, scope, null);
  }

  @RequestMapping(value = "{name}/scopes/{id}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public Scope patchScope(@PathVariable("name") String name, @PathVariable("id") Integer id, @RequestBody ScopeDTO scope) {
    return organizationService.registerScope(name, scope, id);
  }

  @RequestMapping(value = "{name}/events", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<Scope> findEvents(@PathVariable("name") String name) {
    return orgRepository.findScopes(name);
  }

  /* TOPICS */
  @RequestMapping(value = "{name}/topics", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<PagedResources<TopicResource>> getKnowledgePaged(@PathVariable("name") String name,
      @RequestParam(value = "q", defaultValue = "") String q, @RequestParam(value = "page", defaultValue = "1") String page,
      @RequestParam(value = "per_page", defaultValue = "10") String perPage) {

    Page<Topic> topics = orgRepository.findOrganizationTopics(name, q, page, perPage);
    return new ResponseEntity<PagedResources<TopicResource>>(pagedResourceAssembler.toResource(topics, topicAssembler),
        HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/announcements", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Topic>> getAnnouncements(@PathVariable("name") String name) {

    if (securityManager.isCurrentClient(name)) {
      List<Topic> topics = orgRepository.findOrganizationTopicsForClients(name);
      return new ResponseEntity<List<Topic>>(topics, HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Topic>>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/topics", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Topic> postTopic(@PathVariable("name") String name, @RequestBody Topic topic) {

    if (securityManager.isCurrentMemberOrSupport(name)) {
      Topic topic1 = organizationService.registerTopic(name, topic);
      return new ResponseEntity<Topic>(topic1, HttpStatus.OK);
    } else {
      return new ResponseEntity<Topic>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/topics/{number}/comments", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public TopicComment postTopicComment(@PathVariable("name") String name, @PathVariable("number") Long uuid,
      @RequestBody TopicComment topicComment) {
    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, uuid);
    return topicService.postComment(singleTopicByNumber, topicComment);
  }

  @RequestMapping(value = "{name}/topics/{number}/comments", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public List<TopicComment> getTopicCommentc(@PathVariable("name") String name, @PathVariable("number") Long uuid) {
    return topicRepository.findTopicComments(name, uuid);
  }

  @RequestMapping(value = "{name}/topics/{number}/comments/{uuid}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<TopicComment> deleteSingleTopicComment(@PathVariable("name") String name,
      @PathVariable("number") Long number, @PathVariable("uuid") String uuid) {

    TopicComment comment = topicRepository.findTopicCommentByUUID(name, number, uuid);

    if (comment != null) {

      topicService.deleteSingleTopicComment(comment);

      return new ResponseEntity<TopicComment>(HttpStatus.OK);
    } else {
      return new ResponseEntity<TopicComment>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/topics/{number}/comments/{uuid}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<TopicComment> getSingleTopicComment(@PathVariable("name") String name, @PathVariable("number") Long number,
      @PathVariable("uuid") String uuid) {

    TopicComment comment = topicRepository.findTopicCommentByUUID(name, number, uuid);

    if (comment != null) {
      return new ResponseEntity<TopicComment>(comment, HttpStatus.OK);
    } else {
      return new ResponseEntity<TopicComment>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/topics/{number}/comments/{uuid}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<TopicComment> patchSingleTopicComment(@PathVariable("name") String name,
      @PathVariable("number") Long number, @PathVariable("uuid") String uuid, @RequestBody TopicComment topicComment) {

    TopicComment comment = topicRepository.findTopicCommentByUUID(name, number, uuid);

    if (comment != null) {

      TopicComment patched = topicService.patchComment(comment, topicComment);
      return new ResponseEntity<TopicComment>(patched, HttpStatus.OK);
    } else {
      return new ResponseEntity<TopicComment>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/topics/{number}/tags", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity addTagsToTopic(@PathVariable("name") String name, @PathVariable("number") Long number,
      @RequestBody List<Tag> tags) {

    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, number);

    if (singleTopicByNumber != null) {
      topicService.tagsTopic(singleTopicByNumber, tags);

      return new ResponseEntity<TopicComment>(HttpStatus.OK);
    } else {
      return new ResponseEntity<TopicComment>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/topics/{number}/tags/{uuid}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity deleteSingleTopicTag(@PathVariable("name") String name, @PathVariable("number") Long number,
      @PathVariable("uuid") String uuid) {

    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, number);

    if (singleTopicByNumber != null) {
      topicService.deleteSingleTopicTag(singleTopicByNumber, uuid);

      return new ResponseEntity<TopicComment>(HttpStatus.OK);
    } else {
      return new ResponseEntity<TopicComment>(HttpStatus.NOT_FOUND);
    }
  }

  @RequestMapping(value = "{name}/topics/{number}", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Topic> getSingleTopic(@PathVariable("name") String name, @PathVariable("number") Long uuid) {
    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, uuid);
    if (singleTopicByNumber != null) {

      if (Boolean.TRUE.equals(singleTopicByNumber.getConfidential())) {
        if (!securityManager.isCurrentMemberOrSupport(name)) {
          return new ResponseEntity<Topic>(HttpStatus.UNAUTHORIZED);
        }
      }
      return new ResponseEntity<Topic>(singleTopicByNumber, HttpStatus.OK);
    } else {
      return new ResponseEntity<Topic>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/topics/{number}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Topic> deleteSingleTopic(@PathVariable("name") String name, @PathVariable("number") Long uuid) {
    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, uuid);
    topicService.deleteSingleTopic(singleTopicByNumber);
    return new ResponseEntity<Topic>(HttpStatus.OK);
  }

  @RequestMapping(value = "{name}/topics/{number}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Topic> patchSingleTopic(@PathVariable("name") String name, @PathVariable("number") Long uuid,
      @RequestBody Topic patch) {
    Topic singleTopicByNumber = orgRepository.findSingleTopicByNumber(name, uuid);
    if (singleTopicByNumber == null) {
      return new ResponseEntity<Topic>(HttpStatus.NOT_FOUND);
    }
    Topic t = topicService.patchTopic(singleTopicByNumber, patch);
    return new ResponseEntity<Topic>(t, HttpStatus.OK);
  }

  // TAGS

  @RequestMapping(value = "{name}/tags", method = RequestMethod.POST)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Tag> postTag(@PathVariable("name") String name, @RequestBody Tag tag) {

    if (securityManager.isCurrentMember(name)) {
      tag = organizationService.registerTag(name, tag);
      return new ResponseEntity<Tag>(tag, HttpStatus.OK);
    } else {
      return new ResponseEntity<Tag>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/tags/{uuid}", method = RequestMethod.PATCH)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Tag> updateTag(@PathVariable("name") String name, @PathVariable("uuid") String uuid, @RequestBody Tag tag) {

    if (securityManager.isCurrentMember(name)) {
      Tag t = tagService.patchTagByUUID(name, uuid, tag);
      return new ResponseEntity<Tag>(t, HttpStatus.OK);
    } else {
      return new ResponseEntity<Tag>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/tags/{uuid}", method = RequestMethod.DELETE)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<Tag> updateTag(@PathVariable("name") String name, @PathVariable("uuid") String uuid) {

    if (securityManager.isCurrentMember(name)) {
      tagService.dropTag(name, uuid);
      return new ResponseEntity<Tag>(HttpStatus.OK);
    } else {
      return new ResponseEntity<Tag>(HttpStatus.NOT_FOUND);
    }

  }

  @RequestMapping(value = "{name}/tags", method = RequestMethod.GET)
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<List<Tag>> getTags(@PathVariable("name") String name) {

    if (securityManager.isCurrentMember(name)) {

      List<Tag> tags = new ArrayList<Tag>();
      Iterable<Tag> all = tagRepository.findAll();
      for (Tag tag : all) {
        tags.add(tag);
      }
      return new ResponseEntity<List<Tag>>(tags, HttpStatus.OK);
    } else {
      return new ResponseEntity<List<Tag>>(HttpStatus.NOT_FOUND);
    }

  }
}
