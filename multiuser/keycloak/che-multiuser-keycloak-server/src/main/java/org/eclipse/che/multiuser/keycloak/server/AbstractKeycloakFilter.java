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
package org.eclipse.che.multiuser.keycloak.server;

import static com.google.common.base.Strings.isNullOrEmpty;

import com.google.inject.Inject;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureException;
import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.eclipse.che.multiuser.machine.authentication.server.SignatureKeyManager;

/**
 * Base abstract class for the Keycloak-related servlet filters.
 *
 * <p>In particular it defines commnon use-cases when the authentication / multi-user logic should
 * be skipped
 */
public abstract class AbstractKeycloakFilter implements Filter {

  @Inject private SignatureKeyManager keyManager;

  protected boolean shouldSkipAuthentication(HttpServletRequest request, String token) {
    if (isNullOrEmpty(token)) {
      return false;
    }
    if (request.getScheme().startsWith("ws")) {
      return true;
    }
    try {
      final Jwt jwt = Jwts.parser().setSigningKey(keyManager.getKeyPair().getPublic()).parse(token);
      if ("machine".equals(jwt.getHeader().get("kind"))) {
        return true;
      }
    } catch (SignatureException ignored) {
    }
    return false;
  }

  @Override
  public void init(FilterConfig filterConfig) throws ServletException {}

  @Override
  public void destroy() {}
}
