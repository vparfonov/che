/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.selenium.core.client;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import java.io.IOException;
import org.eclipse.che.api.core.BadRequestException;
import org.eclipse.che.api.core.ConflictException;
import org.eclipse.che.api.core.ForbiddenException;
import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.UnauthorizedException;
import org.eclipse.che.api.core.rest.HttpJsonRequestFactory;
import org.eclipse.che.api.core.rest.HttpJsonResponse;
import org.eclipse.che.selenium.core.provider.TestApiEndpointUrlProvider;

/** @author Musienko Maxim */
@Singleton
public class TestUserPreferencesServiceClient {

  private static final String ACTIVATE_CONTRIBUTION_TAB_BY_PROJECT_SELECTION_PROPERTY =
      "git.contribute.activate.projectSelection";
  private final String apiEndpoint;
  private final HttpJsonRequestFactory httpRequestFactory;

  @Inject
  public TestUserPreferencesServiceClient(
      TestApiEndpointUrlProvider apiEndpointProvider, HttpJsonRequestFactory httpRequestFactory)
      throws Exception {
    this.apiEndpoint = apiEndpointProvider.get().toString();
    this.httpRequestFactory = httpRequestFactory;

    // Set application.confirmExit property to 'never' to avoid web page closing confirmation pop-up
    this.setProperty("theia-user-preferences", "{\"application.confirmExit\":\"never\"}");
  }

  public void addGitCommitter(String committerName, String committerEmail) throws Exception {
    httpRequestFactory
        .fromUrl(apiEndpoint + "preferences")
        .usePutMethod()
        .setBody(
            ImmutableMap.of(
                "git.committer.name", committerName,
                "git.committer.email", committerEmail))
        .request();
  }

  public HttpJsonResponse setProperty(String propertyName, String propertyValue) throws Exception {
    return httpRequestFactory
        .fromUrl(apiEndpoint + "preferences")
        .usePutMethod()
        .setBody(ImmutableMap.of(propertyName, propertyValue))
        .request();
  }

  public String getPreferences() throws Exception {
    return httpRequestFactory
        .fromUrl(apiEndpoint + "preferences")
        .useGetMethod()
        .request()
        .asString();
  }

  public void restoreDefaultContributionTabPreference()
      throws ForbiddenException, BadRequestException, IOException, ConflictException,
          NotFoundException, ServerException, UnauthorizedException {
    httpRequestFactory
        .fromUrl(apiEndpoint + "preferences")
        .useDeleteMethod()
        .setBody(ImmutableList.of(ACTIVATE_CONTRIBUTION_TAB_BY_PROJECT_SELECTION_PROPERTY))
        .request();
  }
}
