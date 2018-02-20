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
package org.eclipse.che.ide.command.toolbar.commands;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.web.bindery.event.shared.EventBus;
import org.eclipse.che.ide.api.command.CommandAddedEvent;
import org.eclipse.che.ide.api.command.CommandExecutor;
import org.eclipse.che.ide.api.command.CommandGoal;
import org.eclipse.che.ide.api.command.CommandImpl;
import org.eclipse.che.ide.api.command.CommandManager;
import org.eclipse.che.ide.api.command.CommandRemovedEvent;
import org.eclipse.che.ide.api.command.CommandUpdatedEvent;
import org.eclipse.che.ide.api.command.CommandsLoadedEvent;
import org.eclipse.che.ide.api.mvp.Presenter;
import org.eclipse.che.ide.api.workspace.model.MachineImpl;

/** Presenter drives the UI for executing commands. */
@Singleton
public class ExecuteCommandPresenter implements Presenter, ExecuteCommandView.ActionDelegate {

  private final ExecuteCommandView view;
  private final Provider<CommandExecutor> commandExecutorProvider;

  @Inject
  public ExecuteCommandPresenter(
      ExecuteCommandView view,
      CommandManager commandManager,
      Provider<CommandExecutor> commandExecutorProvider,
      EventBus eventBus) {
    this.view = view;
    this.commandExecutorProvider = commandExecutorProvider;

    view.setDelegate(this);

    eventBus.addHandler(
        CommandsLoadedEvent.getType(), e -> commandManager.getCommands().forEach(view::addCommand));
    eventBus.addHandler(CommandAddedEvent.getType(), e -> view.addCommand(e.getCommand()));
    eventBus.addHandler(CommandRemovedEvent.getType(), e -> view.removeCommand(e.getCommand()));
    eventBus.addHandler(
        CommandUpdatedEvent.getType(),
        e -> {
          view.removeCommand(e.getInitialCommand());
          view.addCommand(e.getUpdatedCommand());
        });
  }

  @Override
  public void go(AcceptsOneWidget container) {
    container.setWidget(view);
  }

  @Override
  public void onCommandExecute(CommandImpl command) {
    commandExecutorProvider.get().executeCommand(command);
  }

  @Override
  public void onCommandExecute(CommandImpl command, MachineImpl machine) {
    commandExecutorProvider.get().executeCommand(command, machine.getName());
  }

  @Override
  public void onGuide(CommandGoal goal) {}
}
