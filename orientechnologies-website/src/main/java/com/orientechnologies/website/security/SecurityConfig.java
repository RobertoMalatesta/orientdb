package com.orientechnologies.website.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import com.orientechnologies.website.services.TokenAuthenticationService;

/**
 * Created by Enrico Risa on 21/10/14.
 */

@Configuration
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

  @Autowired
  private TokenAuthenticationService tokenAuthenticationService;

  @Override
  protected void configure(HttpSecurity http) throws Exception {

    http.csrf().disable();
    http.authorizeRequests().antMatchers("/api/v1/github/**").permitAll().antMatchers("/api/v1/**").authenticated().and()
        .addFilterBefore(new StatelessAuthenticationFilter(tokenAuthenticationService), UsernamePasswordAuthenticationFilter.class);
  }

}
