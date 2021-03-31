const config = require('../config.js');

const children = require('../fixtures/children.js');
const respondents = require('../fixtures/respondents.js');
const applicant = require('../fixtures/applicant.js');
const solicitor = require('../fixtures/solicitor.js');
const others = require('../fixtures/others.js');
const otherProceedings = require('../fixtures/otherProceedingData');
const ordersAndDirectionsNeeded = require('../fixtures/ordersAndDirectionsNeeded.js');
const tabAssertionHelper = require('../helpers/tab_assertion_helper');

let caseId;

Feature('Local authority creates application');

BeforeSuite(async ({I}) => caseId = await I.submitNewCase(config.swanseaLocalAuthorityUserOne));

Before(async ({I}) => await I.navigateToCaseDetailsAs(config.swanseaLocalAuthorityUserOne, caseId));

Scenario('local authority sees task list', async ({caseViewPage}) => {
  caseViewPage.selectTab(caseViewPage.tabs.startApplication);

  await caseViewPage.checkTaskIsFinished(config.applicationActions.changeCaseName);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterOrdersAndDirectionsNeeded);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterHearingNeeded);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterGrounds);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterRiskAndHarmToChildren);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterFactorsAffectingParenting);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.uploadDocuments);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterApplicant);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterChildren);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterRespondents);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterAllocationProposal);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterOtherProceedings);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterInternationalElement);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterOthers);
  caseViewPage.checkTaskIsNotStarted(config.applicationActions.enterAttendingHearing);

  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority changes case name @create-case-with-mandatory-sections-only', async ({I, caseViewPage, changeCaseNameEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.changeCaseName);
  await changeCaseNameEventPage.changeCaseName('New case name');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  await I.seeEventSubmissionConfirmation(config.applicationActions.changeCaseName);
  caseViewPage.seeInCaseTitle('New case name');
  caseViewPage.seeInCaseTitle(caseId);

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  await caseViewPage.checkTaskIsFinished(config.applicationActions.changeCaseName);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.changeCaseName);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters orders and directions @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterOrdersAndDirectionsNeededEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterOrdersAndDirectionsNeeded);
  await enterOrdersAndDirectionsNeededEventPage.checkCareOrder();
  enterOrdersAndDirectionsNeededEventPage.checkInterimCareOrder();
  enterOrdersAndDirectionsNeededEventPage.checkSupervisionOrder();
  enterOrdersAndDirectionsNeededEventPage.checkInterimSupervisionOrder();
  enterOrdersAndDirectionsNeededEventPage.checkEducationSupervisionOrder();
  enterOrdersAndDirectionsNeededEventPage.checkEmergencyProtectionOrder();
  enterOrdersAndDirectionsNeededEventPage.selectPreventRemovalFromAddressEPOType();
  await enterOrdersAndDirectionsNeededEventPage.enterAddress(ordersAndDirectionsNeeded.address);
  enterOrdersAndDirectionsNeededEventPage.checkOtherOrder();
  enterOrdersAndDirectionsNeededEventPage.checkWhereabouts();
  enterOrdersAndDirectionsNeededEventPage.checkEntry();
  enterOrdersAndDirectionsNeededEventPage.checkSearch();
  enterOrdersAndDirectionsNeededEventPage.checkProtectionOrdersOther();
  enterOrdersAndDirectionsNeededEventPage.enterProtectionOrdersDetails('Test');
  enterOrdersAndDirectionsNeededEventPage.checkContact();
  enterOrdersAndDirectionsNeededEventPage.checkAssessment();
  enterOrdersAndDirectionsNeededEventPage.checkMedicalPractitioner();
  enterOrdersAndDirectionsNeededEventPage.checkExclusion();
  enterOrdersAndDirectionsNeededEventPage.enterWhoIsExcluded('John Doe');
  enterOrdersAndDirectionsNeededEventPage.checkProtectionDirectionsOther();
  enterOrdersAndDirectionsNeededEventPage.enterProtectionDirectionsDetails('Test');
  enterOrdersAndDirectionsNeededEventPage.enterOrderDetails('Test');
  enterOrdersAndDirectionsNeededEventPage.checkDirections();
  enterOrdersAndDirectionsNeededEventPage.enterDirections('Test');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterOrdersAndDirectionsNeeded);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Which orders do you need?'], ['Care order', 'Interim care order', 'Supervision order', 'Interim supervision order', 'Education supervision order', 'Emergency protection order', 'Variation or discharge of care or supervision order']);
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'What type of EPO are you requesting?'], 'Prevent removal from an address');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Do you need any of these related orders?'], ['Information on the whereabouts of the child', 'Authorisation for entry of premises', 'Authorisation to search for another child on the premises', 'Other order under section 48 of the Children Act 1989']);
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Give details'], 'Test');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Do you need any of these directions?'], ['Contact with any named person', 'A medical or psychiatric examination, or another assessment of the child', 'To be accompanied by a registered medical practitioner, nurse or midwife', 'An exclusion requirement', 'Other direction relating to an emergency protection order']);
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Who\'s excluded?'], 'John Doe');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Give details'], 'Test');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Which order do you need?'], 'Test');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Do you need any other directions?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Orders and directions needed', 'Give details'], 'Test');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterOrdersAndDirectionsNeeded);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterOrdersAndDirectionsNeeded);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters hearing @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterHearingNeededEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterHearingNeeded);
  await enterHearingNeededEventPage.enterTimeFrame();
  enterHearingNeededEventPage.enterHearingType();
  enterHearingNeededEventPage.enterWithoutNoticeHearing();
  enterHearingNeededEventPage.enterReducedHearing();
  enterHearingNeededEventPage.enterRespondentsAware();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterHearingNeeded);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'When do you need a hearing?'], 'Same day');
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'Give reason'], 'test reason');
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'What type of hearing do you need?'], 'Contested interim care order');
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'Do you need a without notice hearing?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'Do you need a hearing with reduced notice?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Hearing needed', 'Are respondents aware of proceedings?'], 'No');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterHearingNeeded);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterHearingNeeded);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters children @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterChildrenEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterChildren);
  await enterChildrenEventPage.enterChildDetails('Bran', 'Stark', '01', '08', '2015');
  await enterChildrenEventPage.defineChildSituation('01', '11', '2017');
  await enterChildrenEventPage.enterAddress(children[0].address);
  await enterChildrenEventPage.enterKeyDatesAffectingHearing();
  await enterChildrenEventPage.enterSummaryOfCarePlan();
  await enterChildrenEventPage.defineAdoptionIntention();
  await enterChildrenEventPage.enterParentsDetails();
  await enterChildrenEventPage.enterSocialWorkerDetails();
  await enterChildrenEventPage.defineChildAdditionalNeeds();
  await enterChildrenEventPage.enterContactDetailsHidden('No');
  await enterChildrenEventPage.enterLitigationIssues('Yes', 'mock reason');
  await I.addAnotherElementToCollection();
  await enterChildrenEventPage.enterChildDetails('Susan', 'Wilson', '01', '07', '2016', 'Girl');
  await enterChildrenEventPage.defineChildSituation('02', '11', '2017');
  await enterChildrenEventPage.enterAddress(children[1].address);
  await enterChildrenEventPage.enterKeyDatesAffectingHearing();
  await enterChildrenEventPage.enterSummaryOfCarePlan();
  await enterChildrenEventPage.defineAdoptionIntention();
  await enterChildrenEventPage.enterParentsDetails();
  await enterChildrenEventPage.enterSocialWorkerDetails();
  await enterChildrenEventPage.defineChildAdditionalNeeds();
  await enterChildrenEventPage.enterContactDetailsHidden('Yes');
  await enterChildrenEventPage.enterLitigationIssues('No');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterChildren);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'First name'], 'Bran');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'Last name'], 'Stark');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'Date of birth'], '1 Aug 2015');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'Gender'], 'Boy');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'Child\'s living situation'], 'Living with respondents');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'What date did they start staying here?'], '1 Nov 2017');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Building and Street'], 'Flat 2');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Address Line 2'], 'Caversham House 15-17');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Address Line 3'], 'Church Road');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Town or City'], 'Reading');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Postcode/Zipcode'], 'RG4 7AA');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Country'], 'United Kingdom');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Key dates for this child'], 'Tuesday the 11th');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Brief summary of care and contact plan'], 'care plan summary');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Are you considering adoption at this stage?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Mother\'s full name'], 'Laura Smith');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Father\'s full name'], 'David Smith');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Does the father have parental responsibility?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Name of social worker'], 'James Jackson');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Social worker\'s telephone number', 'Telephone number'], '01234567890');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Does the child have any additional needs?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Do you need contact details hidden from other parties?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Do you believe this child will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Give details, including assessment outcomes and referrals to health services'], 'mock reason');

  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'First name'], 'Susan');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Last name'], 'Wilson');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Date of birth'], '1 Jul 2016');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Gender'], 'Girl');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Child\'s living situation'], 'Living with respondents');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'What date did they start staying here?'], '2 Nov 2017');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Key dates for this child'], 'Tuesday the 11th');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Brief summary of care and contact plan'], 'care plan summary');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Are you considering adoption at this stage?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Mother\'s full name'], 'Laura Smith');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Father\'s full name'], 'David Smith');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Does the father have parental responsibility?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Party', 'Name of social worker'], 'James Jackson');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Social worker\'s telephone number', 'Telephone number'], '01234567890');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Does the child have any additional needs?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Do you need contact details hidden from other parties?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Child 2', 'Do you believe this child will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'No');

  caseViewPage.selectTab(caseViewPage.tabs.confidential);
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'First name'], 'Susan');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Party', 'Last name'], 'Wilson');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Building and Street'], '2 Three Tuns Wynd');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Address Line 2'], 'High Street');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Address Line 3'], 'Stokesley');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Town or City'], 'Middlesbrough');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Postcode/Zipcode'], 'TS9 5DQ');
  tabAssertionHelper.seeInTab(I, ['Child 1', 'Current address', 'Country'], 'United Kingdom');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsCompleted(config.applicationActions.enterChildren);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterChildren);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters respondents @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterRespondentsEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterRespondents);
  await enterRespondentsEventPage.enterRespondent(respondents[0]);
  await enterRespondentsEventPage.enterContactDetailsHidden('No', 'mock reason');
  await enterRespondentsEventPage.enterLitigationIssues('Yes', 'mock reason');
  await enterRespondentsEventPage.enterRepresentationDetails('Yes', respondents[0]);
  await I.addAnotherElementToCollection();
  await enterRespondentsEventPage.enterRespondent(respondents[1]);
  await enterRespondentsEventPage.enterContactDetailsHidden('Yes', 'mock reason');
  await enterRespondentsEventPage.enterLitigationIssues('No');
  await enterRespondentsEventPage.enterRepresentationDetails('No');

  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterRespondents);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'First name'], respondents[0].firstName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'Last name'], respondents[0].lastName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'Date of birth'], '1 Jan 1980');
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'Gender'], respondents[0].gender);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Building and Street'], respondents[0].address.buildingAndStreet.lineOne);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Address Line 2'], respondents[0].address.buildingAndStreet.lineTwo);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Address Line 3'], respondents[0].address.buildingAndStreet.lineThree);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Town or City'], respondents[0].address.town);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Postcode/Zipcode'], respondents[0].address.postcode);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Country'], respondents[0].address.country);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Telephone', 'Telephone number'], respondents[0].telephone);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'What is the respondent\'s relationship to the child or children in this case?'], respondents[0].relationshipToChild);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Do you need contact details hidden from other parties?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Do you believe this person will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Give details, including assessment outcomes and referrals to health services'], 'mock reason');
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Representative', 'Representative\'s first name'], respondents[0].solicitor.firstName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Representative', 'Representative\'s last name'], respondents[0].solicitor.lastName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Representative', 'Email address'], respondents[0].solicitor.email);
  tabAssertionHelper.seeOrganisationInTab(I, ['Respondents 1', 'Representative', 'Name'], 'Swansea City Council');
  let address = Object.values(respondents[0].solicitor.organisationAddress);
  tabAssertionHelper.seeOrganisationInTab(I, ['Respondents 1', 'Representative', 'Address'], address);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Building and Street'], respondents[0].solicitor.address.buildingAndStreet.lineOne);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Address Line 2'], respondents[0].solicitor.address.buildingAndStreet.lineTwo);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Address Line 3'], respondents[0].solicitor.address.buildingAndStreet.lineThree);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Town or City'], respondents[0].solicitor.address.town);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Postcode/Zipcode'], respondents[0].solicitor.address.postcode);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Managing office', 'Country'], respondents[0].solicitor.address.country);

  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Party', 'First name'], respondents[1].firstName);
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Party', 'Last name'], respondents[1].lastName);
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Party', 'Date of birth'], '1 Jan 1955');
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Party', 'Gender'], respondents[1].gender);
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'What is the respondent\'s relationship to the child or children in this case?'], respondents[1].relationshipToChild);
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Do you need contact details hidden from other parties?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Give reason'], 'mock reason');
  tabAssertionHelper.seeInTab(I, ['Respondents 2', 'Do you believe this person will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'No');

  caseViewPage.selectTab(caseViewPage.tabs.confidential);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'First name'], respondents[1].firstName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Party', 'Last name'], respondents[1].lastName);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Building and Street'], respondents[1].address.buildingAndStreet.lineOne);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Address Line 2'], respondents[1].address.buildingAndStreet.lineTwo);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Address Line 3'], respondents[1].address.buildingAndStreet.lineThree);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Town or City'], respondents[1].address.town);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Postcode/Zipcode'], respondents[1].address.postcode);
  tabAssertionHelper.seeInTab(I, ['Respondents 1', 'Current address', 'Country'], respondents[1].address.country);

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsCompleted(config.applicationActions.enterRespondents);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterRespondents);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters applicant @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterApplicantEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterApplicant);
  await enterApplicantEventPage.enterApplicantDetails(applicant);
  await enterApplicantEventPage.enterSolicitorDetails(solicitor);
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterApplicant);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Party', 'Name of applicant'], applicant.name);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Party', 'Payment by account (PBA) number'], applicant.pbaNumber);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Party', 'Client code'], applicant.clientCode);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Party', 'Customer reference'], applicant.customerReference);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Address', 'Building and Street'], applicant.address.buildingAndStreet.lineOne);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Address', 'Address Line 2'], applicant.address.buildingAndStreet.lineTwo);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Address', 'Town or City'], applicant.address.town);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Address', 'County'], applicant.address.county);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Address', 'Postcode/Zipcode'], applicant.address.postcode);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Telephone number', 'Telephone number'], applicant.telephoneNumber);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Telephone number', 'Name of person to contact'], applicant.nameOfPersonToContact);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Job title'], applicant.jobTitle);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Mobile number', 'Mobile number'], applicant.mobileNumber);
  tabAssertionHelper.seeInTab(I, ['Applicants 1', 'Email', 'Email'], applicant.email);
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'Solicitor\'s full name'], 'John Smith');
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'Solicitor\'s mobile number'], '7000000000');
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'Solicitor\'s telephone number'], '00000000000');
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'Solicitor\'s email'], 'solicitor@email.com');
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'DX number'], '160010 Kingsway 7');
  tabAssertionHelper.seeInTab(I, ['Solicitor', 'Solicitor\'s reference'], 'reference');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsCompleted(config.applicationActions.enterApplicant);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterApplicant);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters others to be given notice', async ({I, caseViewPage, enterOthersEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterOthers);
  await enterOthersEventPage.enterOtherDetails(others[0]);
  await enterOthersEventPage.enterRelationshipToChild('Tim Smith');
  await enterOthersEventPage.enterContactDetailsHidden('No');
  await enterOthersEventPage.enterLitigationIssues('No');
  await I.addAnotherElementToCollection('Other person');
  await enterOthersEventPage.enterOtherDetails(others[1]);
  await enterOthersEventPage.enterRelationshipToChild('Tim Smith');
  await enterOthersEventPage.enterContactDetailsHidden('Yes');
  await enterOthersEventPage.enterLitigationIssues('Yes', 'mock reason');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterOthers);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Person 1', 'Full name'], 'John Smith');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Person 1', 'Date of birth'], '1 Jan 1985');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Person 1', 'Gender'], 'Male');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Person 1', 'Place of birth'], 'Scotland');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Building and Street'], 'Flat 2');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Address Line 2'], 'Caversham House 15-17');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Address Line 3'], 'Church Road');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Town or City'], 'Reading');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Postcode/Zipcode'], 'RG4 7AA');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Current address', 'Country'], 'United Kingdom');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Telephone number'], '07888288288');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'What is this person\'s relationship to the child or children in this case?'], 'Tim Smith');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Do you need contact details hidden from other parties?'], 'No');
  tabAssertionHelper.seeInTab(I, ['Others to be given notice', 'Do you believe this person will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'No');

  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Full name'], 'Paul Wilsdon');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Date of birth'], '1 Jan 1984');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Gender'], 'Male');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Place of birth'], 'Wales');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'What is this person\'s relationship to the child or children in this case?'], 'Tim Smith');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Do you need contact details hidden from other parties?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Give reason'], 'mock reason');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Do you believe this person will have problems with litigation capacity (understanding what\'s happening in the case)?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Other person 1', 'Give details, including assessment outcomes and referrals to health services'], 'mock reason');

  caseViewPage.selectTab(caseViewPage.tabs.confidential);
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Full name'], 'Paul Wilsdon');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Building and Street'], '2 Three Tuns Wynd');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Address Line 2'], 'High Street');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Address Line 3'], 'Stokesley');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Town or City'], 'Middlesbrough');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Postcode/Zipcode'], 'TS9 5DQ');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Current address', 'Country'], 'United Kingdom');
  tabAssertionHelper.seeInTab(I, ['Others 1', 'Telephone number'], '07888288288');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsInProgress(config.applicationActions.enterOthers);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterOthers);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters grounds for application @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterGroundsForApplicationEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterGrounds);
  await enterGroundsForApplicationEventPage.enterThresholdCriteriaDetails();
  await enterGroundsForApplicationEventPage.enterGroundsForEmergencyProtectionOrder();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterGrounds);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['How does this case meet the threshold criteria?', 'The child concerned is suffering or is likely to suffer significant harm because they are:'], 'Not receiving care that would be reasonably expected from a parent');
  tabAssertionHelper.seeInTab(I, ['How are there grounds for an emergency protection order?', ''], [enterGroundsForApplicationEventPage.fields.groundsForApplication.harmIfNotMoved, enterGroundsForApplicationEventPage.fields.groundsForApplication.harmIfMoved, enterGroundsForApplicationEventPage.fields.groundsForApplication.urgentAccessRequired]);

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterGrounds);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterGrounds);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters risk and harm to children', async ({I, caseViewPage, enterRiskAndHarmToChildrenEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterRiskAndHarmToChildren);
  await enterRiskAndHarmToChildrenEventPage.completePhysicalHarm();
  enterRiskAndHarmToChildrenEventPage.completeEmotionalHarm();
  enterRiskAndHarmToChildrenEventPage.completeSexualAbuse();
  enterRiskAndHarmToChildrenEventPage.completeNeglect();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterRiskAndHarmToChildren);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Physical harm including non-accidental injury'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Select all that apply'], 'Past harm');
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Emotional harm'], 'No');
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Sexual abuse'], 'No');
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Neglect'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Risks and harm to children', 'Select all that apply'], ['Past harm', 'Future risk of harm']);

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterRiskAndHarmToChildren);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterRiskAndHarmToChildren);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters factors affecting parenting', async ({I, caseViewPage, enterFactorsAffectingParentingEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterFactorsAffectingParenting);
  await enterFactorsAffectingParentingEventPage.completeAlcoholOrDrugAbuse();
  enterFactorsAffectingParentingEventPage.completeDomesticViolence();
  enterFactorsAffectingParentingEventPage.completeAnythingElse();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterFactorsAffectingParenting);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Alcohol or drug abuse'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Give details'], 'mock reason');
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Domestic violence'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Give details'], 'mock reason');
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Anything else'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Factors affecting parenting', 'Give details'], 'mock reason');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterFactorsAffectingParenting);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterFactorsAffectingParenting);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters international element', async ({I, caseViewPage, enterInternationalElementEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterInternationalElement);
  await enterInternationalElementEventPage.fillForm();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');
  I.seeEventSubmissionConfirmation(config.applicationActions.enterInternationalElement);

  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['International element', 'Are there any suitable carers outside of the UK?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['International element', 'Give reason'], 'test');
  tabAssertionHelper.seeInTab(I, ['International element', 'Are you aware of any significant events that have happened outside the UK?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['International element', 'Give reason'], 'test');
  tabAssertionHelper.seeInTab(I, ['International element', 'Are you aware of any issues with the jurisdiction of this case - for example under the Brussels 2 regulation?'], 'No');
  tabAssertionHelper.seeInTab(I, ['International element', 'Are you aware of any proceedings outside the UK?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['International element', 'Give reason'], 'test');
  tabAssertionHelper.seeInTab(I, ['International element', 'Has, or should, a government or central authority in another country been involved in this case?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['International element', 'Give reason'], 'International involvement reason');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterInternationalElement);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterInternationalElement);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters other proceedings', async ({I, caseViewPage, enterOtherProceedingsEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterOtherProceedings);
  enterOtherProceedingsEventPage.selectYesForProceeding();
  await enterOtherProceedingsEventPage.enterProceedingInformation(otherProceedings[0]);
  await I.addAnotherElementToCollection();
  await enterOtherProceedingsEventPage.enterProceedingInformation(otherProceedings[1]);
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterOtherProceedings);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Are there any past or ongoing proceedings relevant to this case?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Are these previous or ongoing proceedings?'], 'Ongoing');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Case number'], '000000');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Date started'], '01/01/01');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Date ended'], '02/01/01');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Orders made'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Judge'], 'District Judge Martin Brown');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Names of children involved'], 'Joe Bloggs');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Name of guardian'], 'John Smith');
  tabAssertionHelper.seeInTab(I, ['Other proceedings', 'Is the same guardian needed?'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Are these previous or ongoing proceedings?'], 'Previous');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Case number'], '000123');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Date started'], '02/02/02');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Date ended'], '03/03/03');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Orders made'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Judge'], 'District Judge Martin Brown');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Names of children involved'], 'James Simpson');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Name of guardian'], 'David Burns');
  tabAssertionHelper.seeInTab(I, ['Additional proceedings 1', 'Is the same guardian needed?'], 'Yes');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterOtherProceedings);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterOtherProceedings);
  await caseViewPage.checkTaskIsUnavailable(config.applicationActions.submitCase);
});

Scenario('local authority enters allocation proposal @create-case-with-mandatory-sections-only', async ({I, caseViewPage, enterAllocationProposalEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterAllocationProposal);
  await enterAllocationProposalEventPage.selectAllocationProposal('Magistrate');
  await enterAllocationProposalEventPage.enterProposalReason('test');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterAllocationProposal);
  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterAllocationProposal);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterAllocationProposal);
});

Scenario('local authority enters attending hearing', async ({I, caseViewPage, enterAttendingHearingEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.enterAttendingHearing);
  await enterAttendingHearingEventPage.enterInterpreter();
  enterAttendingHearingEventPage.enterWelshProceedings();
  enterAttendingHearingEventPage.enterIntermediary();
  enterAttendingHearingEventPage.enterDisabilityAssistance();
  enterAttendingHearingEventPage.enterExtraSecurityMeasures();
  enterAttendingHearingEventPage.enterSomethingElse();
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.enterAttendingHearing);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Interpreter'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Give details including person, language and dialect'], 'French translator');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Spoken or written Welsh'], 'No');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Intermediary'], 'No');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Facilities or assistance for a disability'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Give details'], 'learning difficulty');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Separate waiting room or other security measures'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Give details'], 'Separate waiting rooms');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Something else'], 'Yes');
  tabAssertionHelper.seeInTab(I, ['Attending the hearing', 'Give details'], 'I need this for this person');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsFinished(config.applicationActions.enterAttendingHearing);
  await caseViewPage.checkTaskIsAvailable(config.applicationActions.enterAttendingHearing);
});

Scenario('local authority adds multiple application documents', async ({I, caseViewPage, addApplicationDocumentsEventPage}) => {
  await caseViewPage.goToNewActions(config.applicationActions.addApplicationDocuments);

  await addApplicationDocumentsEventPage.addApplicationDocument('Threshold', config.testPdfFile);
  await addApplicationDocumentsEventPage.addApplicationDocument('SWET', config.testPdfFile, undefined, 'Genogram included');
  await addApplicationDocumentsEventPage.addApplicationDocument('Other', config.testPdfFile, 'Medical report');
  await I.seeCheckAnswersAndCompleteEvent('Save and continue');

  I.seeEventSubmissionConfirmation(config.applicationActions.addApplicationDocuments);
  caseViewPage.selectTab(caseViewPage.tabs.viewApplication);
  tabAssertionHelper.seeInTab(I, ['Documents 1', 'Type of document'], 'Threshold');
  tabAssertionHelper.seeInTab(I, ['Documents 1', 'File'], 'mockFile.pdf');
  tabAssertionHelper.seeInTab(I, ['Documents 3', 'Uploaded by'], 'kurt@swansea.gov.uk');

  tabAssertionHelper.seeInTab(I, ['Documents 2', 'Type of document'], 'SWET');
  tabAssertionHelper.seeInTab(I, ['Documents 2', 'File'], 'mockFile.pdf');
  tabAssertionHelper.seeInTab(I, ['Documents 3', 'Uploaded by'], 'kurt@swansea.gov.uk');

  tabAssertionHelper.seeInTab(I, ['Documents 3', 'Type of document'], 'Other');
  tabAssertionHelper.seeInTab(I, ['Documents 3', 'File'], 'mockFile.pdf');
  tabAssertionHelper.seeInTab(I, ['Documents 3', 'Uploaded by'], 'kurt@swansea.gov.uk');

  caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  caseViewPage.checkTaskIsInProgress(config.applicationActions.uploadDocuments);
});

let feeToPay = '2055'; //Need to remember this between tests.. default in case the test below fails

Scenario('local authority submits application @create-case-with-mandatory-sections-only', async ({I, caseViewPage, submitApplicationEventPage}) => {
  await caseViewPage.selectTab(caseViewPage.tabs.startApplication);
  await caseViewPage.startTask(config.applicationActions.submitCase);

  feeToPay = await submitApplicationEventPage.getFeeToPay();
  submitApplicationEventPage.seeDraftApplicationFile();
  await submitApplicationEventPage.giveConsent();
  await I.completeEvent('Submit', null, true);

  I.seeEventSubmissionConfirmation(config.applicationActions.submitCase);
  caseViewPage.selectTab(caseViewPage.tabs.documents);
  I.see('New_case_name.pdf');
});

Scenario('HMCTS admin check the payment', async ({I, caseViewPage, paymentHistoryPage}) => {
  await I.navigateToCaseDetailsAs(config.hmctsAdminUser, caseId);
  caseViewPage.selectTab(caseViewPage.tabs.paymentHistory);
  await paymentHistoryPage.checkPayment(feeToPay, applicant.pbaNumber);
}).retry(1); // retry due to async nature of the payment and the payment could be still processing..
