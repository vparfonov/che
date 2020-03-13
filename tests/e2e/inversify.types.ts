
/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

const TYPES = {
    Driver: Symbol.for('Driver'),
    CheLogin: Symbol.for('CheLogin'),
    OcpLogin: Symbol.for('OcpLogin'),
    WorkspaceUtil: Symbol.for('WorkspaceUtil'),
    IAuthorizationHeaderHandler: Symbol.for('IAuthorizationHeaderHandler'),
    ITokenHandler: Symbol.for('ITokenHandler')


};

const CLASSES = {
    DriverHelper: 'DriverHelper',
    Dashboard: 'Dashboard',
    Workspaces: 'Workspaces',
    NewWorkspace: 'NewWorkspace',
    WorkspaceDetails: 'WorkspaceDetails',
    WorkspaceDetailsPlugins: 'WorkspaceDetailsPlugins',
    Ide: 'Ide',
    ProjectTree: 'ProjectTree',
    Editor: 'Editor',
    TopMenu: 'TopMenu',
    QuickOpenContainer: 'QuickOpenContainer',
    PreviewWidget: 'PreviewWidget',
    GitHubPlugin: 'GitHubPlugin',
    RightToolbar: 'RightToolbar',
    Terminal: 'Terminal',
    DebugView: 'DebugView',
    DialogWindow: 'DialogWindow',
    ScreenCatcher: 'ScreenCatcher',
    OcpLoginPage: 'OcpLoginPage',
    OcpWebConsolePage: 'OcpWebConsolePage',
    OpenWorkspaceWidget: 'OpenWorkspaceWidget',
    ContextMenu: 'ContextMenu',
    CheLoginPage: 'CheLoginPage',
    GitHubUtil: 'GitHubUtil',
    CheGitApi: 'CheGitApi',
    GitPlugin: 'GitPlugin',
    TestWorkspaceUtil: 'TestWorkspaceUtil',
    NotificationCenter: 'NotificationCenter',
    PreferencesHandler: 'PreferencesHandler',
    CheApiRequestHandler: 'CheApiRequestHandler',
    GetStarted: 'GetStarted'
};

export { TYPES, CLASSES };
