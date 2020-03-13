/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/

import { che } from '@eclipse-che/api';
import { TestConstants } from '../../TestConstants';
import { injectable, inject } from 'inversify';
import { DriverHelper } from '../DriverHelper';
import 'reflect-metadata';
import { WorkspaceStatus } from './WorkspaceStatus';
import { ITestWorkspaceUtil } from './ITestWorkspaceUtil';
import { error } from 'selenium-webdriver';
import { CheApiRequestHandler } from '../requestHandlers/CheApiRequestHandler';
import { CLASSES } from '../../inversify.types';

@injectable()
export class TestWorkspaceUtil implements ITestWorkspaceUtil {

    static readonly WORKSPACE_API_URL: string = 'api/workspace';

    constructor(
        @inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper,
        @inject(CLASSES.CheApiRequestHandler) private readonly processRequestHandler: CheApiRequestHandler
    ) { }

    public async waitWorkspaceStatus(namespace: string, workspaceName: string, expectedWorkspaceStatus: WorkspaceStatus) {
        const workspaceStatusApiUrl: string = `${TestWorkspaceUtil.WORKSPACE_API_URL}/${namespace}:${workspaceName}`;
        const attempts: number = TestConstants.TS_SELENIUM_WORKSPACE_STATUS_ATTEMPTS;
        const polling: number = TestConstants.TS_SELENIUM_WORKSPACE_STATUS_POLLING;
        let workspaceStatus: string = '';

        for (let i = 0; i < attempts; i++) {
            const response = await this.processRequestHandler.get(workspaceStatusApiUrl);

            if (response.status !== 200) {
                await this.driverHelper.wait(polling);
                continue;
            }

            workspaceStatus = await response.data.status;

            if (workspaceStatus === expectedWorkspaceStatus) {
                return;
            }

            await this.driverHelper.wait(polling);
        }

        throw new error.TimeoutError(`Exceeded the maximum number of checking attempts, workspace status is: '${workspaceStatus}' different to '${expectedWorkspaceStatus}'`);
    }

    public async waitPluginAdding(namespace: string, workspaceName: string, pluginName: string) {
        const workspaceStatusApiUrl: string = `${TestWorkspaceUtil.WORKSPACE_API_URL}/${namespace}:${workspaceName}`;
        const attempts: number = TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS;
        const polling: number = TestConstants.TS_SELENIUM_DEFAULT_POLLING;

        for (let i = 0; i < attempts; i++) {
            const response = await this.processRequestHandler.get(workspaceStatusApiUrl);

            if (response.status !== 200) {
                await this.driverHelper.wait(polling);
                continue;
            }

            const machines: string = JSON.stringify(response.data.runtime.machines);
            const isPluginPresent: boolean = machines.search(pluginName) > 0;

            if (isPluginPresent) {
                break;
            }

            if (i === attempts - 1) {
                throw new error.TimeoutError(`Exceeded maximum tries attempts, the '${pluginName}' plugin is not present in the workspace runtime.`);
            }

            await this.driverHelper.wait(polling);
        }
    }

    public async getListOfWorkspaceId(): Promise<string[]> {
        const getAllWorkspacesResponse = await this.processRequestHandler.get(TestWorkspaceUtil.WORKSPACE_API_URL);

        interface IMyObj {
            id: string;
            status: string;
        }

        let stringified = JSON.stringify(getAllWorkspacesResponse.data);
        let arrayOfWorkspaces = <IMyObj[]>JSON.parse(stringified);
        let wsList: Array<string> = [];

        for (let entry of arrayOfWorkspaces) {
            wsList.push(entry.id);
        }

        return wsList;
    }

    public async getIdOfRunningWorkspace(wsName: string): Promise<string> {
        const getWorkspacesByNameResponse = await this.processRequestHandler.get(`${TestWorkspaceUtil.WORKSPACE_API_URL}/:${wsName}`);
        return getWorkspacesByNameResponse.data.id;

    }

    public async getIdOfRunningWorkspaces(): Promise<Array<string>> {
        try {
            const getAllWorkspacesResponse = await this.processRequestHandler.get(TestWorkspaceUtil.WORKSPACE_API_URL);

            interface IMyObj {
                id: string;
                status: string;
            }
            let stringified = JSON.stringify(getAllWorkspacesResponse.data);
            let arrayOfWorkspaces = <IMyObj[]>JSON.parse(stringified);
            let idOfRunningWorkspace: Array<string> = new Array();

            for (let entry of arrayOfWorkspaces) {
                if (entry.status === 'RUNNING') {
                    idOfRunningWorkspace.push(entry.id);
                }
            }

            return idOfRunningWorkspace;
        } catch (err) {
            console.log(`Getting id of running workspaces failed. URL used: ${TestWorkspaceUtil.WORKSPACE_API_URL}`);
            throw err;
        }
    }

    public async removeWorkspaceById(id: string) {
        const workspaceIdUrl: string = `${TestWorkspaceUtil.WORKSPACE_API_URL}/${id}`;
        try {
            const deleteWorkspaceResponse = await this.processRequestHandler.delete(workspaceIdUrl);
            // response code 204: "No Content" expected
            if (deleteWorkspaceResponse.status !== 204) {
                throw new Error(`Can not remove workspace. Code: ${deleteWorkspaceResponse.status} Data: ${deleteWorkspaceResponse.data}`);
            }
        } catch (err) {
            console.log(`Removing of workspace failed.`);
            throw err;
        }
    }

    public async stopWorkspaceById(id: string) {
        const stopWorkspaceApiUrl: string = `${TestWorkspaceUtil.WORKSPACE_API_URL}/${id}`;

        try {
            const stopWorkspaceResponse = await this.processRequestHandler.delete(`${stopWorkspaceApiUrl}/runtime`);
            // response code 204: "No Content" expected
            if (stopWorkspaceResponse.status !== 204) {
                throw new Error(`Can not stop workspace. Code: ${stopWorkspaceResponse.status} Data: ${stopWorkspaceResponse.data}`);
            }

            for (let i = 0; i < TestConstants.TS_SELENIUM_PLUGIN_PRECENCE_ATTEMPTS; i++) {
                const wsStatus = await this.processRequestHandler.get(stopWorkspaceApiUrl);
                if (wsStatus.data.status === 'STOPPED') {
                    break;
                }
                await this.driverHelper.wait(TestConstants.TS_SELENIUM_DEFAULT_POLLING);
            }
        } catch (err) {
            console.log(`Stopping workspace failed. URL used: ${stopWorkspaceApiUrl}`);
            throw err;
        }
    }

    public async cleanUpAllWorkspaces() {
        let listOfRunningWorkspaces: Array<string> = await this.getIdOfRunningWorkspaces();
        for (const entry of listOfRunningWorkspaces) {
            await this.stopWorkspaceById(entry);
        }

        let listAllWorkspaces: Array<string> = await this.getListOfWorkspaceId();

        for (const entry of listAllWorkspaces) {
            await this.removeWorkspaceById(entry);
        }

    }

    async createWsFromDevFile(customTemplate: che.workspace.devfile.Devfile) {
        try {
             await this.processRequestHandler.post(TestWorkspaceUtil.WORKSPACE_API_URL + '/devfile', customTemplate);
        } catch (error) {
            console.error(error);
            throw error;
        }
    }

    async getBaseDevfile(): Promise<che.workspace.devfile.Devfile> {
        const baseDevfile: che.workspace.devfile.Devfile = {
            apiVersion: '1.0.0',
            metadata: {
                name: 'test-workspace'
            }
        };

        return baseDevfile;
    }

}
