const config = require('../config.js');

Feature('Select hearing');

Before((I, caseViewPage) => {
	I.logInAndCreateCase(config.localAuthorityEmail, config.localAuthorityPassword, config.eventSummary, config.eventDescription);
	I.waitForElement('.tabs', 10);
	caseViewPage.goToNewActions(config.applicationActions.selectHearing);
	I.waitForElement('ccd-case-edit-page', 10);
});

Scenario('test form save with no details entered', (I) => {
	I.continueAndSubmit(config.eventSummary, config.eventDescription);
	I.see(`updated with event: ${config.applicationActions.selectHearing}`);
});

Scenario('test half form filled', (I, selectHearingPage) => {
	selectHearingPage.halfFillForm();
	I.see('Give reason');
	I.continueAndSubmit(config.eventSummary, config.eventDescription);
	I.see(`updated with event: ${config.applicationActions.selectHearing}`);
});

Scenario('test form is fully filled in', (I, selectHearingPage) => {
	selectHearingPage.fillForm();
	I.see('Give reason');
	I.continueAndSubmit(config.eventSummary, config.eventDescription);
	I.see(`updated with event: ${config.applicationActions.selectHearing}`);
});
