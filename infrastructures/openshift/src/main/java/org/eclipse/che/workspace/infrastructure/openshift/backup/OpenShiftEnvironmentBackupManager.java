/*
 * Copyright (c) [2012] - [2017] Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.workspace.infrastructure.openshift.backup;

import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.slf4j.LoggerFactory.getLogger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableSet;

import io.fabric8.kubernetes.api.model.Container;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.openshift.api.model.Project;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.BiConsumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.eclipse.che.api.core.NotFoundException;
import org.eclipse.che.api.core.ServerException;
import org.eclipse.che.api.core.model.workspace.runtime.Machine;
import org.eclipse.che.api.core.model.workspace.runtime.Server;
import org.eclipse.che.api.core.util.CommandLine;
import org.eclipse.che.api.core.util.ListLineConsumer;
import org.eclipse.che.api.core.util.ProcessUtil;
import org.eclipse.che.api.workspace.server.WorkspaceManager;
import org.eclipse.che.api.workspace.server.model.impl.WorkspaceImpl;
import org.eclipse.che.api.workspace.server.spi.InfrastructureException;
import org.eclipse.che.workspace.infrastructure.kubernetes.KubernetesInternalRuntime;
import org.eclipse.che.workspace.infrastructure.kubernetes.api.shared.KubernetesNamespaceMeta;
import org.eclipse.che.workspace.infrastructure.openshift.OpenShiftClientFactory;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProject;
import org.eclipse.che.workspace.infrastructure.openshift.project.OpenShiftProjectFactory;
import org.slf4j.Logger;

/**
 * Copies workspace files between docker machine and backup storage.
 *
 * @author Alexander Garagatyi
 * @author Mykola Morhun
 */
@Singleton
public class OpenShiftEnvironmentBackupManager implements EnvironmentBackupManager {

  private static final Logger LOG = getLogger(OpenShiftEnvironmentBackupManager.class);
  private static final String ERROR_MESSAGE_PREFIX =
      "Can't detect container user ids to chown backed up files of workspace ";
  // if exit code 0 script finished successfully
  private static final Set<Integer> RESTORE_SUCCESS_RETURN_CODES = ImmutableSet.of(0);
  // if exit code 0 script finished successfully
  // if exit code 24 some files are gone during transfer. It may happen on scheduled backups when
  // user performs some files operations like git checkout. So we treat this situation as
  // successful.
  private static final Set<Integer> BACKUP_SUCCESS_RETURN_CODES = ImmutableSet.of(0, 24);

  private final OpenShiftProjectFactory openShiftProjectFactory;
  private final OpenShiftClientFactory clientFactory;
  private final String backupScript;
  private final String restoreScript;
  private final int maxBackupDuration;
  private final int restoreDuration;
  private final File backupsRootDir;
  private final WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder;
  private final String projectFolderPath;
  private final ConcurrentMap<String, ReentrantLock> workspacesBackupLocks;
  private final ConcurrentMap<String, Map<String, User>> workspacesMachinesUsersInfo;
  private final WorkspaceManager workspaceManager;

  @Inject
  public OpenShiftEnvironmentBackupManager(
      OpenShiftProjectFactory openShiftProjectFactory,
      OpenShiftClientFactory clientFactory,
      @Named("machine.backup.backup_script")String backupScript,
      @Named("machine.backup.restore_script") String restoreScript,
      @Named("machine.backup.backup_duration_second") int maxBackupDurationSec,
      @Named("machine.backup.restore_duration_second") int restoreDurationSec,
      @Named("che.user.workspaces.storage") File backupsRootDir,
      WorkspaceIdHashLocationFinder workspaceIdHashLocationFinder,
      @Named("che.workspace.projects.storage") String projectFolderPath,
      WorkspaceManager workspaceManager) {
    this.openShiftProjectFactory = openShiftProjectFactory;
    this.clientFactory = clientFactory;
    this.backupScript = backupScript;
    this.restoreScript = restoreScript;
    this.maxBackupDuration = maxBackupDurationSec;
    this.restoreDuration = restoreDurationSec;
    this.backupsRootDir = backupsRootDir;
    this.workspaceIdHashLocationFinder = workspaceIdHashLocationFinder;
    this.projectFolderPath = projectFolderPath;
    this.workspaceManager = workspaceManager;

    workspacesBackupLocks = new ConcurrentHashMap<>();
    workspacesMachinesUsersInfo = new ConcurrentHashMap<>();
  }

  @Override
  public void backupWorkspace(String workspaceId) throws ServerException, NotFoundException {
    try {
      WorkspaceImpl workspace = workspaceManager.getWorkspace(workspaceId);
      if (workspace.getRuntime() == null) {
        throw new NotFoundException("Workspace is not running");
      }
      Project che = clientFactory.createOC().projects().withName("che").get();
      OpenShiftProject openShiftProject = openShiftProjectFactory.get(workspace);
      List<KubernetesNamespaceMeta> list = openShiftProjectFactory.list();
      for (KubernetesNamespaceMeta kubernetesNamespaceMeta : list) {
        LOG.warn(kubernetesNamespaceMeta.getName());
      }

      List<Pod> pods = openShiftProject.deployments().get();
      for (Pod pod : pods) {
        LOG.warn(">>>>>> :::: " + pod.getMetadata().getName());
      }
//      openShiftProject.deployments()
//          .exec(
//              kubernetesMachine.getPodName(),
//              kubernetesMachine.getContainerName(),
//              EXEC_TIMEOUT_MIN,
//              command,
//              outputConsumer)
      Server rsync = null;
      Map<String, ? extends Machine> machines = workspace.getRuntime().getMachines();
      for (Machine machine : machines.values()) {
        if (machine.getServers().containsKey("che-rsync")) {
          rsync = machine.getServers().get("che-rsync");
        }
      }

      if (rsync == null) {
        LOG.warn(">>>>>>>>>>>>>> rsync server not found");
      }

      URI url = URI.create(rsync.getUrl());
      String nodeHost = url.getHost();
      int syncPort = url.getPort();

      LOG.warn(">>>>>>>>>>>>>> rsync server found" + nodeHost + " :: " + syncPort);
      String destPath =
          workspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId).toString();
      String srcUserName =
          "user"; // getUserInfo(workspaceId, dockerDevMachine.getContainer()).name;

      backupInsideLock(workspaceId, projectFolderPath, nodeHost, syncPort, srcUserName, destPath);
    } catch (Exception e) {
      throw new ServerException(e.getLocalizedMessage(), e);
    }
  }

  /**
   * Copy files of workspace into backup storage and cleanup them in container.
   *
   * @param workspaceId id of workspace to backup
   * @param containerId id of container that contains data
   * @param nodeHost host of a node where container is running
   * @throws ServerException if any other error occurs
   */
  public void backupWorkspaceAndCleanup(String workspaceId, String containerId, String nodeHost)
      throws ServerException {
    try {
      String destPath =
          workspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId).toString();
      // if sync agent is not in machine port parameter is not used
      int syncPort = 2222; // getSyncPort(containerId);
      String srcUserName = "user"; // getUserInfo(workspaceId, containerId).name;

      backupAndCleanupInsideLock(
          workspaceId, projectFolderPath, nodeHost, syncPort, srcUserName, destPath);
    } catch (Exception e) {
      throw new ServerException(e.getLocalizedMessage(), e);
    } finally {
      // remove lock in case exception prevent removing it in regular place to prevent resources
      // leak
      // and blocking further WS start
      workspacesBackupLocks.remove(workspaceId);
      // clear user info cache
      workspacesMachinesUsersInfo.remove(workspaceId);
    }
  }

  /**
   * Copy files of workspace from backups storage into container.
   *
   * @param workspaceId id of workspace to backup
   * @throws ServerException if any error occurs
   */
  public void restoreWorkspaceBackup(String workspaceId) throws ServerException {
    try {
      WorkspaceImpl workspace = workspaceManager.getWorkspace(workspaceId);
      if (workspace.getRuntime() == null) {
        throw new NotFoundException("Workspace is not running");
      }
      Server rsync = null;
      Map<String, ? extends Machine> machines = workspace.getRuntime().getMachines();
      for (Machine machine : machines.values()) {
        if (machine.getServers().containsKey("che-rsync")) {
          rsync = machine.getServers().get("che-rsync");
        }
      }

      if (rsync == null) {
        LOG.warn(">>>>>>>>>>>>>> rsync server not found");
      }

      OpenShiftProject openShiftProject = openShiftProjectFactory.get(workspace);
      Optional<Pod> optionalPod = openShiftProject.deployments().get("che-rsync");
      if (!optionalPod.isPresent()) {
        LOG.warn(">>>>>> ::: pod not found" );
      }

      List<KubernetesNamespaceMeta> list = openShiftProjectFactory.list();
      for (KubernetesNamespaceMeta kubernetesNamespaceMeta : list) {
        LOG.warn(kubernetesNamespaceMeta.getName());
      }

      Pod pod = optionalPod.get();
      String podName = pod.getMetadata().getName();
      Container container = pod.getSpec().getContainers().get(0);

      String containerName = container.getName();
      URI url = URI.create(rsync.getUrl());
      String nodeHost = url.getHost();
      int syncPort = url.getPort();

      String srcPath =
          workspaceIdHashLocationFinder.calculateDirPath(backupsRootDir, workspaceId).toString();
      User user = getUserInfo(workspaceId);

      final BiConsumer<String, String> outputConsumer =
          (stream, text) -> {
           LOG.warn("text :: " + text);
           LOG.warn("stream ::" + stream);
          };

      String cmdRestore = "rsync --recursive --quiet --rsh=\"ssh  -i /.ssh/rsync -l user -p 2222 "
          + "-o PasswordAuthentication=no  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null\" "
          + "/projects/ " + nodeHost + ":/projects/";


      LOG.warn(">>>>>>> cmdRestore ::::: " + cmdRestore);
      LOG.warn(">>>>>>> POD ::::: " + podName + " CONTAINER ::::: " + containerName);

      openShiftProject.deployments().exec(podName, containerName, 10, new String[]{"sh", "-c", cmdRestore},
          outputConsumer);


      String cmdBackup = "rsync --recursive --quiet --rsh=\"ssh  -i /.ssh/rsync -l user -p 2222 "
          + "-o PasswordAuthentication=no  -o StrictHostKeyChecking=no -o UserKnownHostsFile=/dev/null\" "
          + nodeHost +":/projects/ /projects/";
      final int[] i = {0};

      class SayHello extends TimerTask {
        public void run() {
          try {
            openShiftProject.deployments().exec(podName, containerName, 1, new String[]{"sh", "-c", cmdBackup},
                outputConsumer);
          } catch (InfrastructureException e) {
            e.printStackTrace();
          }
          LOG.warn(">>>>>>> cmdRestore ::::: " + cmdBackup + " :: " + i[0]++);
        }
      }

//// And From your main() method or any other method
//      Timer timer = new Timer();
//      timer.schedule(new SayHello(), 5000, 5000);



    } catch (IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new ServerException(
          format("Can't restore file system of workspace %s in container %s", workspaceId));
    } catch (NotFoundException e) {
      e.printStackTrace();
    } catch (InfrastructureException e) {
      e.printStackTrace();
    }
  }

  private void backupInsideLock(
      String workspaceId,
      String srcPath,
      String srcAddress,
      int srcPort,
      String srcUserName,
      String destPath)
      throws ServerException {
    ReentrantLock lock = workspacesBackupLocks.get(workspaceId);
    // backup workspace only if no backup with cleanup before
    if (lock != null) {
      // backup workspace only if this workspace isn't under backup/restore process
      if (lock.tryLock()) {
        try {
          if (workspacesBackupLocks.get(workspaceId) == null) {
            // It is possible to reach here, because remove lock from locks map and following unlock
            // in
            // backup with cleanup method is not atomic operation.
            // In very rare case it may happens, but it is ok. Just ignore this backup
            // because it is called after cleanup
            return;
          }
          executeBackupScript(
              workspaceId, srcPath, srcAddress, srcPort, false, srcUserName, destPath);
        } finally {
          lock.unlock();
        }
      }
    } else {
      LOG.warn("Attempt to backup workspace {} after cleanup", workspaceId);
    }
  }

  private void backupAndCleanupInsideLock(
      String workspaceId,
      String srcPath,
      String srcAddress,
      int srcPort,
      String srcUserName,
      String destPath)
      throws ServerException {
    ReentrantLock lock = workspacesBackupLocks.get(workspaceId);
    if (lock != null) {
      lock.lock();
      try {
        if (workspacesBackupLocks.get(workspaceId) == null) {
          // it is possible to reach here if invoke this method again while previous one is in
          // progress
          LOG.error(
              "Backup with cleanup of the workspace {} was invoked several times simultaneously",
              workspaceId);
          return;
        }
        executeBackupScript(workspaceId, srcPath, srcAddress, srcPort, true, srcUserName, destPath);
      } finally {
        workspacesBackupLocks.remove(workspaceId);
        lock.unlock();
      }
    } else {
      LOG.warn("Attempt to backup workspace {} after cleanup", workspaceId);
    }
  }

  private void restoreBackupInsideLock(
      String workspaceId,
      String srcPath,
      String destinationPath,
      String destUserId,
      String destGroupId,
      String destUserName,
      String destAddress,
      int destPort)
      throws ServerException {
    boolean restored = false;
    ReentrantLock lock = new ReentrantLock();
    lock.lock();
    try {
      if (workspacesBackupLocks.putIfAbsent(workspaceId, lock) != null) {
        // it shouldn't happen, but for case when restore of one workspace is invoked simultaneously
        String err =
            "Restore of workspace "
                + workspaceId
                + " failed. Another restore process of the same workspace is in progress";
        LOG.error(err);
        throw new ServerException(err);
      }

      // TODO refactor that code to eliminate creation of directories here
      Files.createDirectories(Paths.get(srcPath));

      CommandLine commandLine =
          new CommandLine(
              restoreScript,
              srcPath,
              destinationPath,
              destAddress,
              Integer.toString(destPort),
              destUserId,
              destGroupId,
              destUserName);

      executeCommand(
          commandLine.asArray(),
          restoreDuration,
          destAddress,
          workspaceId,
          RESTORE_SUCCESS_RETURN_CODES);
      restored = true;
    } catch (TimeoutException e) {
      throw new ServerException(
          "Restoring of workspace "
              + workspaceId
              + " filesystem terminated due to timeout on "
              + destAddress
              + " node.");
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new ServerException(
          "Restoring of workspace "
              + workspaceId
              + " filesystem interrupted on "
              + destAddress
              + " node.");
    } catch (IOException e) {
      String error =
          "Restoring of workspace "
              + workspaceId
              + " filesystem terminated on "
              + destAddress
              + " node. "
              + e.getLocalizedMessage();
      LOG.error(error, e);
      throw new ServerException(error);
    } finally {
      lock.unlock();
      if (!restored) {
        workspacesBackupLocks.remove(workspaceId, lock);
      }
    }
  }

  private void executeBackupScript(
      String workspaceId,
      String srcPath,
      String srcAddress,
      int srcPort,
      boolean removeSourceOnSuccess,
      String srcUserName,
      String destPath)
      throws ServerException {
    CommandLine commandLine =
        new CommandLine(
            backupScript,
            srcPath,
            srcAddress,
            Integer.toString(srcPort),
            destPath,
            Boolean.toString(removeSourceOnSuccess),
            srcUserName);

    try {
      executeCommand(
          commandLine.asArray(),
          maxBackupDuration,
          srcAddress,
          workspaceId,
          BACKUP_SUCCESS_RETURN_CODES);
    } catch (TimeoutException e) {
      throw new ServerException(
          "Backup of workspace "
              + workspaceId
              + " filesystem terminated due to timeout on "
              + srcAddress
              + " node.");
    } catch (InterruptedException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new ServerException(
          "Backup of workspace "
              + workspaceId
              + " filesystem interrupted on "
              + srcAddress
              + " node.");
    } catch (IOException e) {
      LOG.error(e.getLocalizedMessage(), e);
      throw new ServerException(
          "Backup of workspace "
              + workspaceId
              + " filesystem terminated on "
              + srcAddress
              + " node. "
              + e.getLocalizedMessage());
    }
  }

  /**
   * Returns user id, group id and username in container. This method caches info about users and on
   * second and subsequent calls cached value will be returned.
   *
   * @param workspaceId id of workspace
   * @return {@code User} object with id, groupId and username filled
   * @throws IOException if connection to container fails
   * @throws ServerException if other error occurs
   */
  private User getUserInfo(String workspaceId) throws IOException, ServerException {
    //        Map<String, User> workspaceMachinesUserInfo =
    // workspacesMachinesUsersInfo.get(workspaceId);
    //        if (workspaceMachinesUserInfo == null) {
    //            workspaceMachinesUserInfo = new HashMap<>();
    //            workspacesMachinesUsersInfo.put(workspaceId, workspaceMachinesUserInfo);
    //        }
    //
    //        User user = workspaceMachinesUserInfo.get(containerId);
    //        if (user == null) {
    //            user = getUserInfoWithinContainer(workspaceId, containerId);
    //            workspaceMachinesUserInfo.put(containerId, user);
    //        }

    return new User("user", "root", "user");
  }

  /**
   * Retrieves user id, group id and username inside of container.
   *
   * @param workspaceId ID of workspace
   * @param containerId ID of container
   * @return {@code User} object with id, groupId and username filled
   * @throws IOException if connection to container fails
   * @throws ServerException if other error occurs
   */
  private User getUserInfoWithinContainer(String workspaceId, String containerId)
      throws IOException, ServerException {

    ArrayList<String> output =
        executeCommandInContainer(workspaceId, containerId, "id -u && id -g && id -u -n");

    if (output.size() != 3) {
      LOG.error("{} {}. Docker output: {}", ERROR_MESSAGE_PREFIX, workspaceId, output);
      throw new ServerException(ERROR_MESSAGE_PREFIX + workspaceId);
    }
    return new User(output.get(0), output.get(1), output.get(2));
  }

  /**
   * Executes provides command inside of specified docker container and returns output.
   *
   * @param workspaceId ID of workspace
   * @param containerId ID of container
   * @param command command to execute
   * @return ArrayList with output lines as entries
   * @throws IOException if connection to container fails
   * @throws ServerException if other error occurs
   */
  private ArrayList<String> executeCommandInContainer(
      String workspaceId, String containerId, String command) throws IOException, ServerException {
    //        Exec exec =
    //                dockerConnector.createExec(
    //                        CreateExecParams.create(containerId, new String[]{"sh", "-c",
    // command})
    //                                .withDetach(false));
    ArrayList<String> execOutput = new ArrayList<>(3);
    //        ValueHolder<Boolean> hasFailed = new ValueHolder<>(false);
    //        dockerConnector.startExec(
    //                StartExecParams.create(exec.getId()),
    //                logMessage -> {
    //                    if (logMessage.getType() != LogMessage.Type.STDOUT) {
    //                        hasFailed.set(true);
    //                    }
    //                    execOutput.add(logMessage.getContent());
    //                });
    //
    //        if (hasFailed.get()) {
    //            LOG.error("{} {}. Docker output: {}", ERROR_MESSAGE_PREFIX, workspaceId,
    // execOutput);
    //            throw new ServerException(ERROR_MESSAGE_PREFIX + workspaceId);
    //        }

    return execOutput;
  }

  //    /**
  //     * Retrieves published port for SSH in machine.
  //     *
  //     * @throws ServerException if port is not found
  //     */
  //    private int getSyncPort(String containerId) throws ServerException, EnvironmentException {
  //        ContainerInfo containerInfo;
  //        try {
  //            containerInfo = dockerConnector.inspectContainer(containerId);
  //        } catch (IOException e) {
  //            LOG.error(e.getLocalizedMessage(), e);
  //            throw new ServerException("Workspace projects files in ws-machine are not
  // accessible");
  //        }
  //        if (!containerInfo.getState().isRunning()) {
  //            throw new EnvironmentException("Container " + containerId + " unexpectedly exited");
  //        }
  //
  //        Map<String, List<PortBinding>> ports =
  //                firstNonNull(containerInfo.getNetworkSettings().getPorts(), emptyMap());
  //        List<PortBinding> portBindings = ports.get("22/tcp");
  //        if (portBindings == null || portBindings.isEmpty()) {
  //            // should not happen
  //            throw new ServerException(
  //                    "Sync port is not exposed in ws-machine. Workspace projects syncing is not
  // possible");
  //        }
  //
  //        return Integer.parseUnsignedInt(portBindings.get(0).getHostPort());
  //    }

  //    /**
  //     * Retrieves published port for SSH in machine.
  //     *
  //     * @throws ServerException if port is not found
  //     */
  //    private int getSyncPort(Machine machine) throws ServerException {
  //        Server server = machine.getRuntime().getServers().get("22/tcp");
  //        if (server == null) {
  //            throw new ServerException(
  //                    "Sync port is not exposed in ws-machine. Workspace projects syncing is not
  // possible");
  //        }
  //        return Integer.parseUnsignedInt(server.getAddress().split(":", 2)[1]);
  //    }

  @VisibleForTesting
  void executeCommand(
      String[] commandLine,
      int timeout,
      String address,
      String workspaceId,
      Set<Integer> successResponseCodes)
      throws TimeoutException, IOException, InterruptedException {
    final ListLineConsumer outputConsumer = new ListLineConsumer();
    Process process = ProcessUtil.executeAndWait(commandLine, timeout, SECONDS, outputConsumer);

    if (!successResponseCodes.contains(process.exitValue())) {
      LOG.error(
          "Error occurred during backup/restore of workspace '{}' on node '{}' : {}",
          workspaceId,
          address,
          outputConsumer.getText());
      throw new IOException("Synchronization process failed. Exit code " + process.exitValue());
    }
  }

  private static class User {

    String id;
    String groupId;
    String name;

    public User(String id, String groupId, String name) {
      this.id = id;
      this.groupId = groupId;
      this.name = name;
    }
  }
}
