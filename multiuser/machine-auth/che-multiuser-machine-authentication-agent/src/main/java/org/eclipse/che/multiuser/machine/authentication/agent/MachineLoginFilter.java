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
package org.eclipse.che.multiuser.machine.authentication.agent;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;
import static javax.servlet.http.HttpServletResponse.SC_UNAUTHORIZED;

import com.google.gson.Gson;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import java.io.IOException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.commons.auth.token.RequestTokenExtractor;
import org.eclipse.che.commons.env.EnvironmentContext;
import org.eclipse.che.commons.subject.Subject;
import org.eclipse.che.commons.subject.SubjectImpl;

/**
 * Protects user's machine from unauthorized access.
 *
 * @author Anton Korneta
 */
@Singleton
public class MachineLoginFilter implements Filter {

  private static final Gson GSON = new Gson();

  private final String apiEndpoint;
  private final HttpJsonRequestFactory requestFactory;
  private final RequestTokenExtractor tokenExtractor;

  @Inject
  public MachineLoginFilter(
      @Named("che.api") String apiEndpoint,
      HttpJsonRequestFactory requestFactory,
      RequestTokenExtractor tokenExtractor) {
    this.apiEndpoint = apiEndpoint;
    this.requestFactory = requestFactory;
    this.tokenExtractor = tokenExtractor;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
      throws IOException, ServletException {
    final HttpServletRequest httpRequest = (HttpServletRequest) request;
    final HttpSession session = httpRequest.getSession(false);
    if (httpRequest.getScheme().startsWith("ws")
        || !nullToEmpty(tokenExtractor.getToken(httpRequest)).startsWith("machine")) {
      chain.doFilter(request, response);
      return;
    }
    if (session != null && session.getAttribute("principal") != null) {
      try {
        EnvironmentContext.getCurrent().setSubject((Subject) session.getAttribute("principal"));
        chain.doFilter(request, response);
        return;
      } finally {
        EnvironmentContext.reset();
      }
    }
    final String token = tokenExtractor.getToken(httpRequest);
    if (isNullOrEmpty(token)) {
      ((HttpServletResponse) response)
          .sendError(SC_UNAUTHORIZED, "Authentication on machine failed, token is missed");
      return;
    }
    final Jwt jwt = Jwts.parser().setSigningKey(getPublicKey()).parse(token);
    if ("machine".equals(jwt.getHeader().get("kind"))) {
      final SubjectImpl subject = GSON.fromJson(jwt.getBody().toString(), SubjectImpl.class);
      try {
        EnvironmentContext.getCurrent().setSubject(subject);
        final HttpSession httpSession = httpRequest.getSession(true);
        httpSession.setAttribute("principal", subject);
        chain.doFilter(request, response);
      } finally {
        EnvironmentContext.reset();
      }
    } else {
      ((HttpServletResponse) response)
          .sendError(SC_UNAUTHORIZED, "Authentication on machine failed, token is missed");
    }
  }

  private PublicKey getPublicKey() {
    try {
      final X509EncodedKeySpec ks =
          new X509EncodedKeySpec(
              Base64.getDecoder().decode(System.getenv().get("SIGNATURE_PUBLIC_KEY")));
      final KeyFactory kf = KeyFactory.getInstance("RSA");
      return kf.generatePublic(ks);
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public void destroy() {}
}
