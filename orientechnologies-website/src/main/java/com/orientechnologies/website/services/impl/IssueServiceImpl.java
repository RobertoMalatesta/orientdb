package com.orientechnologies.website.services.impl;

import com.orientechnologies.orient.core.id.ORecordId;
import com.orientechnologies.website.OrientDBFactory;
import com.orientechnologies.website.helper.SecurityHelper;
import com.orientechnologies.website.model.schema.*;
import com.orientechnologies.website.model.schema.dto.*;
import com.orientechnologies.website.repository.CommentRepository;
import com.orientechnologies.website.repository.EventRepository;
import com.orientechnologies.website.repository.IssueRepository;
import com.orientechnologies.website.repository.RepositoryRepository;
import com.orientechnologies.website.services.IssueService;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;
import com.tinkerpop.blueprints.impls.orient.OrientGraph;
import com.tinkerpop.blueprints.impls.orient.OrientVertex;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Enrico Risa on 27/10/14.
 */
@Service
public class IssueServiceImpl implements IssueService {

  @Autowired
  private OrientDBFactory      dbFactory;

  @Autowired
  protected CommentRepository  commentRepository;

  @Autowired
  private RepositoryRepository repoRepository;
  @Autowired
  private EventRepository      eventRepository;
  @Autowired
  private IssueRepository      issueRepository;

  @Override
  public void commentIssue(Issue issue, Comment comment) {
    createCommentRelationship(issue, comment);
  }

  @Override
  public Comment createNewCommentOnIssue(Issue issue, Comment comment) {

    comment.setUser(SecurityHelper.currentUser());
    comment.setCreatedAt(new Date());

    comment = commentRepository.save(comment);
    commentIssue(issue, comment);
    return comment;
  }

  @Override
  public void changeMilestone(Issue issue, Milestone milestone) {
    createMilestoneRelationship(issue, milestone);
  }

  @Override
  public void changeLabels(Issue issue, List<Label> labels, boolean replace) {
    createLabelsRelationship(issue, labels, replace);
  }

  @Override
  public List<Label> addLabels(Issue issue, List<String> labels) {

    List<Label> lbs = new ArrayList<Label>();

    for (String label : labels) {
      Label l = repoRepository.findLabelsByRepoAndName(issue.getRepository().getName(), label);
      lbs.add(l);
    }
    changeLabels(issue, lbs, false);

    for (Label lb : lbs) {
      IssueEvent e = new IssueEvent();
      e.setCreatedAt(new Date());
      e.setEvent("labeled");
      e.setLabel(lb);
      e.setActor(SecurityHelper.currentUser());
      e = (IssueEvent) eventRepository.save(e);
      fireEvent(issue, e);
    }
    return lbs;
  }

  @Override
  public void removeLabel(Issue issue, String label, User actor) {
    Label l = repoRepository.findLabelsByRepoAndName(issue.getRepository().getName(), label);
    if (l != null) {
      removeLabelRelationship(issue, l);
      IssueEvent e = new IssueEvent();
      e.setCreatedAt(new Date());
      e.setEvent("unlabeled");
      e.setLabel(l);
      if (actor == null) {
        e.setActor(SecurityHelper.currentUser());
      } else {
        e.setActor(actor);
      }
      e = (IssueEvent) eventRepository.save(e);
      fireEvent(issue, e);
    }
  }

  @Override
  public void fireEvent(Issue issue, Event e) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(e.getId()));

    orgVertex.addEdge(HasEvent.class.getSimpleName(), devVertex);
  }

  @Override
  public void changeUser(Issue issue, User user) {
    createUserRelationship(issue, user);

  }

  private void createUserRelationship(Issue issue, User user) {
    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));

    for (Edge edge : orgVertex.getEdges(Direction.IN, HasOpened.class.getSimpleName())) {
      edge.remove();
    }

    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(user.getRid()));
    devVertex.addEdge(HasOpened.class.getSimpleName(), orgVertex);
  }

  @Override
  public void changeAssignee(Issue issue, User user) {
    createAssigneeRelationship(issue, user);

  }

  @Override
  public void changeVersion(Issue issue, Milestone milestone) {
    createVersionRelationship(issue, milestone);
  }

  @Override
  public Issue changeState(Issue issue, String state) {
    issue.setState(state);
    return issueRepository.save(issue);
  }

  private void createAssigneeRelationship(Issue issue, User user) {
    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));

    for (Edge edge : orgVertex.getEdges(Direction.OUT, IsAssigned.class.getSimpleName())) {
      edge.remove();
    }

    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(user.getRid()));
    orgVertex.addEdge(IsAssigned.class.getSimpleName(), devVertex);
  }

  private void removeLabelRelationship(Issue issue, Label label) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));

    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasLabel.class.getSimpleName())) {
      Vertex v = edge.getVertex(Direction.IN);
      if (label.getName().equals(v.getProperty(OLabel.NAME.toString()))) {
        edge.remove();
      }

    }

  }

  private void createLabelsRelationship(Issue issue, List<Label> labels, boolean replace) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));

    if (replace) {
      for (Edge edge : orgVertex.getEdges(Direction.OUT, HasLabel.class.getSimpleName())) {
        edge.remove();
      }
    }
    for (Label label : labels) {
      OrientVertex devVertex = new OrientVertex(graph, new ORecordId(label.getId()));
      orgVertex.addEdge(HasLabel.class.getSimpleName(), devVertex);
    }

  }

  private void createVersionRelationship(Issue issue, Milestone milestone) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(milestone.getId()));
    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasVersion.class.getSimpleName())) {
      edge.remove();
    }

    orgVertex.addEdge(HasVersion.class.getSimpleName(), devVertex);
  }

  private void createMilestoneRelationship(Issue issue, Milestone milestone) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(milestone.getId()));
    for (Edge edge : orgVertex.getEdges(Direction.OUT, HasMilestone.class.getSimpleName())) {
      edge.remove();
    }

    orgVertex.addEdge(HasMilestone.class.getSimpleName(), devVertex);
  }

  private void createCommentRelationship(Issue issue, Comment comment) {

    OrientGraph graph = dbFactory.getGraph();
    OrientVertex orgVertex = new OrientVertex(graph, new ORecordId(issue.getId()));
    OrientVertex devVertex = new OrientVertex(graph, new ORecordId(comment.getId()));
    orgVertex.addEdge(HasEvent.class.getSimpleName(), devVertex);
  }
}
