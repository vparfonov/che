/*********************************************************************
 * Copyright (c) 2019 Red Hat, Inc.
 *
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 **********************************************************************/
import 'reflect-metadata';
import { injectable, inject } from 'inversify';
import { DriverHelper } from '../../utils/DriverHelper';
import { CLASSES } from '../../inversify.types';
import { By } from 'selenium-webdriver';
import { Logger } from '../../utils/Logger';
import { TestConstants } from '../../TestConstants';

@injectable()
export class OcpLoginPage {

    private static readonly LOGIN_PAGE_OPENSHIFT_XPATH: string = '//*[contains(text(), \'Welcome\')]';

    constructor(
        @inject(CLASSES.DriverHelper) private readonly driverHelper: DriverHelper) { }

    async openLoginPageOpenShift(url: string) {
        Logger.debug('OcpLoginPage.openLoginPageOpenShift');

        await this.driverHelper.navigateToUrl(url);
    }

    async waitOpenShiftLoginWelcomePage() {
        Logger.debug('OcpLoginPage.waitOpenShiftLoginWelcomePage');

        await this.driverHelper.waitVisibility(By.xpath(OcpLoginPage.LOGIN_PAGE_OPENSHIFT_XPATH));
    }

    async clickOnLoginProviderTitle() {
        Logger.debug('OcpLoginPage.clickOnLoginProviderTitle');

        const loginProviderTitleLocator: By = By.css(`a[title=\'Log in with ${TestConstants.TS_OCP_LOGIN_PAGE_PROVIDER_TITLE}\']`);
        await this.driverHelper.waitAndClick(loginProviderTitleLocator);
    }

    async waitAuthorizeOpenShiftIdentityProviderPage() {
        Logger.debug('OcpLoginPage.waitAuthorizeOpenShiftIdentityProviderPage');

        const authorizeOpenshiftIdentityProviderPageLocator: By = By.xpath('//h1[text()=\'Authorize Access\']');
        await this.driverHelper.waitVisibility(authorizeOpenshiftIdentityProviderPageLocator);
    }

    async clickOnApproveAuthorizeAccessButton() {
        Logger.debug('OcpLoginPage.clickOnApproveAuthorizeAccessOpenshift');

        const approveAuthorizeAccessOcpLocator: By = By.css('input[name=\'approve\']');
        await this.driverHelper.waitAndClick(approveAuthorizeAccessOcpLocator);
    }

    async enterUserNameOpenShift(userName: string) {
        Logger.debug(`OcpLoginPage.enterUserNameOpenShift "${userName}"`);

        await this.driverHelper.enterValue(By.id('inputUsername'), userName);
    }

    async enterPasswordOpenShift(passw: string) {
        Logger.debug(`OcpLoginPage.enterPasswordOpenShift "${passw}"`);

        await this.driverHelper.enterValue(By.id('inputPassword'), passw);
    }

    async clickOnLoginButton() {
        Logger.debug('OcpLoginPage.clickOnLoginButton');

        const loginButtonlocator: By = By.css('button[type=submit]');
        await this.driverHelper.waitAndClick(loginButtonlocator);
    }

    async waitDisappearanceOpenShiftLoginWelcomePage() {
        Logger.debug('OcpLoginPage.waitDisappearanceOpenShiftLoginWelcomePage');

        await this.driverHelper.waitDisappearance(By.xpath(OcpLoginPage.LOGIN_PAGE_OPENSHIFT_XPATH));
    }

}
