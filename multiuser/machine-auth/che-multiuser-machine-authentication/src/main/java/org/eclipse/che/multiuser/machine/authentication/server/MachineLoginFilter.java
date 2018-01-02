/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.multiuser.machine.authentication.server;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.gson.Gson;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import java.io.IOException;
import java.security.Principal;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import org.eclipse.che.api.user.server.UserManager;
import org.eclipse.che.commons.auth.token.RequestTokenExtractor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;
import org.eclipse.che.multiuser.api.permission.server.PermissionChecker;

/** @author Max Shaposhnik (mshaposhnik@codenvy.com) */
@Singleton
public class MachineLoginFilter implements Filter {

  private static final Gson GSON = new Gson();

  @Inject private RequestTokenExtractor tokenExtractor;

  @Inject private MachineTokenRegistry machineTokenRegistry;

  @Inject private UserManager userManager;

  @Inject private SignatureKeyManager keyManager;

  @Inject private PermissionChecker permissionChecker;

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final String token = tokenExtractor.getToken(httpRequest);
    if (httpRequest.getScheme().startsWith("ws") || isNullOrEmpty(token)) {
      chain.doFilter(request, response);
    } else {
      final Jwt jwt;
      try {
        jwt = Jwts.parser().setSigningKey(keyManager.getKeyPair().getPublic()).parse(token);
      } catch (SignatureException ex) {
        // not machine request
        chain.doFilter(request, response);
        return;
      }
      if ("machine".equals(jwt.getHeader().get("kind"))) {
        final SubjectImpl subject = GSON.fromJson(jwt.getBody().toString(), SubjectImpl.class);
        try {
          EnvironmentContext.getCurrent().setSubject(subject);
          chain.doFilter(addUserInRequest(httpRequest, subject), response);
        } finally {
          EnvironmentContext.reset();
        }
      }
    }
  }

  private HttpServletRequest addUserInRequest(
      final HttpServletRequest httpRequest, final Subject subject) {
    return new HttpServletRequestWrapper(httpRequest) {
      @Override
      public String getRemoteUser() {
        return subject.getUserName();
      }

      @Override
      public Principal getUserPrincipal() {
        return subject::getUserName;
      }
    };
  }

  @Override
  public void destroy() {}
}
