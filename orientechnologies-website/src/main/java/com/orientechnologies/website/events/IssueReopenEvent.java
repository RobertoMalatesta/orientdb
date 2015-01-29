package com.orientechnologies.website.events;

import com.orientechnologies.website.model.schema.dto.IssueEvent;
import com.orientechnologies.website.repository.EventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring4.SpringTemplateEngine;

import reactor.event.Event;

import com.orientechnologies.website.configuration.AppConfig;
import com.orientechnologies.website.model.schema.dto.Issue;
import com.orientechnologies.website.model.schema.dto.OUser;

/**
 * Created by Enrico Risa on 30/12/14.
 */
@Component
public class IssueReopenEvent extends EventInternal<IssueEvent> {

  @Autowired
  @Lazy
  protected JavaMailSenderImpl sender;

  @Autowired
  protected AppConfig          config;

  @Autowired
  protected EventRepository    eventRepository;

  @Autowired
  private SpringTemplateEngine templateEngine;

  public static String         EVENT = "issue_reopen";

  @Override
  public String event() {
    return EVENT;
  }

  @Override
  public void accept(Event<IssueEvent> issueEvent) {

    IssueEvent comment = issueEvent.getData();
    IssueEvent committed = eventRepository.reload(comment);
    Issue issue = eventRepository.findIssueByEvent(committed);

    Context context = new Context();
    fillContextVariable(context, issue, comment);
    String htmlContent = templateEngine.process("newReopen.html", context);
    SimpleMailMessage mailMessage = new SimpleMailMessage();
    OUser owner = issue.getScope().getOwner();
    mailMessage.setTo(owner.getEmail());
    mailMessage.setFrom("prjhub@orientechnologies.com");
    mailMessage.setSubject(issue.getTitle());
    mailMessage.setText(htmlContent);
    sender.send(mailMessage);

  }

  private void fillContextVariable(Context context, Issue issue, IssueEvent comment) {
    context.setVariable("link", config.endpoint + "/#issues/" + issue.getIid());
    context.setVariable("body", "@" + comment.getActor().getName() + " reopened #" + issue.getIid());
  }
}
