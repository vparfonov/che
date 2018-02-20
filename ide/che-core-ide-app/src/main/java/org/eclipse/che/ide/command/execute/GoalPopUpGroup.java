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
package org.eclipse.che.ide.command.execute;

import com.google.inject.Inject;
import com.google.inject.assistedinject.Assisted;
import org.eclipse.che.ide.api.action.ActionEvent;
import org.eclipse.che.ide.api.action.ActionManager;
import org.eclipse.che.ide.api.action.DefaultActionGroup;
import org.eclipse.che.ide.api.icon.Icon;
import org.eclipse.che.ide.api.icon.IconRegistry;
import org.vectomatic.dom.svg.ui.SVGImage;
import org.vectomatic.dom.svg.ui.SVGResource;

/**
 * Action group that represents command goal.
 *
 * @author Artem Zatsarynnyi
 */
class GoalPopUpGroup extends DefaultActionGroup {

  private final IconRegistry iconRegistry;

  @Inject
  GoalPopUpGroup(@Assisted String goalId, ActionManager actionManager, IconRegistry iconRegistry) {
    super(actionManager);

    this.iconRegistry = iconRegistry;

    setPopup(true);

    // set icon
    final SVGResource commandTypeIcon = getCommandGoalIcon();
    if (commandTypeIcon != null) {
      getTemplatePresentation().setImageElement(new SVGImage(commandTypeIcon).getElement());
    }
  }

  @Override
  public void update(ActionEvent e) {
    e.getPresentation().setText("xxx");
  }

  private SVGResource getCommandGoalIcon() {
    final Icon icon = iconRegistry.getIconIfExist("command.goal.");

    if (icon != null) {
      final SVGImage svgImage = icon.getSVGImage();

      if (svgImage != null) {
        return icon.getSVGResource();
      }
    }

    return null;
  }
}
