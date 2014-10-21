package com.orientechnologies.website.repository;

import com.orientechnologies.website.model.schema.dto.Developer;

/**
 * Created by Enrico Risa on 20/10/14.
 */
public interface DeveloperRepository extends BaseRepository<Developer> {

  public Developer findUserByLogin(String login);

  public Developer findByGithubToken(String token);
}
