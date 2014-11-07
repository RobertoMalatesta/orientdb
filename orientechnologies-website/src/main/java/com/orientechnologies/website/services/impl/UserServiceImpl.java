package com.orientechnologies.website.services.impl;

import com.orientechnologies.website.model.schema.dto.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.orientechnologies.website.github.GUser;
import com.orientechnologies.website.github.GitHub;
import com.orientechnologies.website.repository.UserRepository;
import com.orientechnologies.website.services.OrganizationService;
import com.orientechnologies.website.services.UserService;

/**
 * Created by Enrico Risa on 20/10/14.
 */
@Service
public class UserServiceImpl implements UserService {

  @Autowired
  private UserRepository      userRepository;

  @Autowired
  private OrganizationService organizationService;

  @Override
  public void initUser(String token) {

    try {
      GitHub github = new GitHub(token);
      GUser self = github.user();
      User user = userRepository.findUserByLogin(self.getLogin());
      String email = self.getEmail();

      if (user == null) {
        user = new User(self.getLogin(), token, email);
      } else {
        user.setToken(token);
        user.setEmail(email);
      }
      userRepository.save(user);

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
