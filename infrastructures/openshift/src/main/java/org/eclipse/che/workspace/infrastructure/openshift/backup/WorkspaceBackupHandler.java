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
package org.eclipse.che.workspace.infrastructure.openshift.backup;

import com.google.common.annotations.VisibleForTesting;
import org.eclipse.che.api.core.model.workspace.Workspace;
import org.eclipse.che.api.core.model.workspace.WorkspaceStatus;
import org.eclipse.che.api.core.notification.EventService;
import org.eclipse.che.api.core.notification.EventSubscriber;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.shared.dto.event.WorkspaceStatusEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;

import static org.eclipse.che.api.workspace.shared.Constants.WORKSPACE_STOPPED_BY;

/**
 * Provides API for updating activity timestamp of running workspaces. Stops the inactive workspaces
 * by given expiration time. Upon stopping, workspace attributes will be updated with information
 * like cause and timestamp of workspace stop.
 *
 * <p>Note that the workspace is not stopped immediately, scheduler will stop the workspaces with
 * one minute rate. If workspace idle timeout is negative, then workspace would not be stopped
 * automatically.
 *
 * @author Anton Korneta
 */
@Singleton
public class WorkspaceBackupHandler {

  public static final long MINIMAL_TIMEOUT = 300_000L;

  private static final Logger LOG = LoggerFactory.getLogger(WorkspaceBackupHandler.class);

  private final EventService eventService;
  private final OpenShiftEnvironmentBackupManager backupManager;
  private final EventSubscriber<WorkspaceStatusEvent> updateStatusChangedTimestampSubscriber;
  protected final WorkspaceManager workspaceManager;

  @Inject
  public WorkspaceBackupHandler(
      WorkspaceManager workspaceManager,
      EventService eventService,
      OpenShiftEnvironmentBackupManager backupManager) {
    this.workspaceManager = workspaceManager;
    this.eventService = eventService;
    this.backupManager = backupManager;
    this.updateStatusChangedTimestampSubscriber = new UpdateStatusChangedTimestampSubscriber();
  }

  @VisibleForTesting
  @PostConstruct
  void subscribe() {
    eventService.subscribe(updateStatusChangedTimestampSubscriber, WorkspaceStatusEvent.class);
  }


  private class UpdateStatusChangedTimestampSubscriber
      implements EventSubscriber<WorkspaceStatusEvent> {
    @Override
    public void onEvent(WorkspaceStatusEvent event) {
      String workspaceId = event.getWorkspaceId();
      WorkspaceStatus status = event.getStatus();

      // now do any special handling
      switch (status) {
        case RUNNING:
          try {
            LOG.warn(">>>>..>>.>.>.>..>.>..>.>..>.>>.>" + workspaceId + status);
            backupManager.restoreWorkspaceBackup(workspaceId);
          } catch (Exception ex) {
            LOG.warn(
                "Failed to remove stopped information attribute for workspace {}", workspaceId);
          }

          break;
        case STOPPED:
          break;
        default:
          // do nothing
      }
    }
  }
}
