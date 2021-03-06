/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import {CheButton} from './che-button.directive';

/**
 * @ngdoc directive
 * @name components.directive:cheButtonNotice
 * @restrict E
 * @function
 * @element
 *
 * @description
 * `<che-button-notice>` defines a default button.
 *
 * @param {string=} che-button-title the title of the button
 * @param {string=} che-button-icon the optional icon of the button
 *
 * @usage
 *   <che-button-notice che-button-title="hello"></che-button-notice>
 *
 * @example
 * <example module="userDashboard">
 * <file name="index.html">
 * <che-button-notice che-button-title="Hello"></che-button-notice>
 * <che-button-notice che-button-title="Hello" che-button-icon="fa fa-check-square"></che-button-notice>
 * </file>
 * </example>
 * @author Florent Benoit
 */
export class CheButtonNotice extends CheButton {

  /**
   * Default constructor that is using resource
   * @ngInject for Dependency injection
   */
  constructor () {
    super();
  }


  /**
   * Template for the buttons
   */
  getTemplateStart(): string {
    return '<md-button md-theme=\"chenotice\" class=\"che-button md-accent md-raised md-hue-2\"';
  }

}
