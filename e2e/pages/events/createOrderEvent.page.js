const {I} = inject();
const judgeAndLegalAdvisor = require('../../fragments/judgeAndLegalAdvisor');
const orders = require('../../fixtures/orders.js');
const postcodeLookup = require('../../fragments/addressPostcodeLookup');

module.exports = {
  fields: {
    title: '#order_title',
    details: '#order_details',
    orderTypeList: '#orderTypeAndDocument_type',
    orderSubtypeList: '#orderTypeAndDocument_subtype',
    orderUploadedTypeList: '#orderTypeAndDocument_uploadedOrderType',
    order: {
      name: '#orderTypeAndDocument_orderName',
      description: '#orderTypeAndDocument_orderDescription',
    },
    directionsNeeded: {
      id: '#orderFurtherDirections_directionsNeeded',
      options: {
        yes: '#orderFurtherDirections_directionsNeeded-Yes',
        no: '#orderFurtherDirections_directionsNeeded-No',
      },
    },
    directions: '#orderFurtherDirections_directions',
    exclusionClauseNeeded: {
      id: '#orderExclusionClause_exclusionClauseNeeded',
      options: {
        yes: '#orderExclusionClause_exclusionClauseNeeded-Yes',
        no: '#orderExclusionClause_exclusionClauseNeeded-No',
      },
    },
    exclusionClause: '#orderExclusionClause_exclusionClause',
    dateOfIssue: {
      id: '#dateOfIssue',
    },
    interimEndDate: {
      id: '#interimEndDate_interimEndDate',
      options: {
        endOfProceedings: 'At the end of the proceedings',
        namedDate: 'At the end of a named date',
        specificTimeNamedDate: 'At a specific time on a named date',
      },
      endDate: {
        day: '#interimEndDate_endDate-day',
        month: '#interimEndDate_endDate-month',
        year: '#interimEndDate_endDate-year',
      },
    },
    childSelector: {
      id: '#childSelector_childSelector',
      selector: function (index) {
        return `#childSelector_option${index}`;
      },
      selectorText: 'Yes',
    },
    careOrderSelector: {
      id: '#careOrderSelector_careOrderSelector',
      selector: function (index) {
        return `#careOrderSelector_option${index}`;
      },
      selectorText: 'Discharge order',
    },
    allChildren: {
      id: '#orderAppliesToAllChildren',
      options: {
        yes: 'Yes',
        no: 'No',
      },
    },
    months: '#orderMonths',
    epo: {
      childrenDescription: {
        radioGroup: '#epoChildren_descriptionNeeded',
        description: '#epoChildren_description',
      },
      type: '#epoType',
      removalAddress: '#epoRemovalAddress_epoRemovalAddress',
      includePhrase: '#epoPhrase_includePhrase',
      endDate: {
        id: '#epoEndDate',
        second: '#epoEndDate-second',
        minute: '#epoEndDate-minute',
        hour: '#epoEndDate-hour',
        day: '#epoEndDate-day',
        month: '#epoEndDate-month',
        year: '#epoEndDate-year',
      },
    },
    judgeAndLegalAdvisorTitleId: '#judgeAndLegalAdvisor_judgeTitle',
    closeCase: {
      id: '#closeCaseFromOrder',
      options: {
        yes: '#closeCaseFromOrder-Yes',
        no: '#closeCaseFromOrder-No',
      },
    },
    uploadedOrder: '#uploadedOrder',
    checkYourOrder: '#checkYourOrder_label',
  },

  selectType(type, subtype, orderType) {
    within(this.fields.orderTypeList, () => {
      I.click(locate('label').withText(type));
    });
    if (subtype) {
      within(this.fields.orderSubtypeList, () => {
        I.click(locate('label').withText(subtype));
      });
    }
    if (orderType) {
      I.selectOption(this.fields.orderUploadedTypeList, orderType);
    }
  },

  enterOrderNameAndDescription(name, description) {
    I.fillField(this.fields.order.name, name);
    I.fillField(this.fields.order.description, description);
  },

  async uploadOrder(order) {
    await I.attachFile(this.fields.uploadedOrder, order);
  },

  checkOrder(orderChecks) {
    I.see(orderChecks.familyManCaseNumber);
    I.see(orderChecks.children);
    I.see(orderChecks.order);
  },

  enterC21OrderDetails() {
    I.fillField(this.fields.title, orders[0].title);
    I.fillField(this.fields.details, orders[0].details);
  },

  enterJudgeAndLegalAdvisor(judgeLastName, legalAdvisorName, judgeTitle = judgeAndLegalAdvisor.fields.judgeTitleRadioGroup.herHonourJudge,
    judgeEmailAddress) {
    judgeAndLegalAdvisor.selectJudgeTitle('', judgeTitle);
    judgeAndLegalAdvisor.enterJudgeLastName(judgeLastName);
    judgeAndLegalAdvisor.enterJudgeEmailAddress(judgeEmailAddress);
    judgeAndLegalAdvisor.enterLegalAdvisorName(legalAdvisorName);
  },

  useAlternateJudge() {
    judgeAndLegalAdvisor.useAlternateJudge();
  },

  useAllocatedJudge(legalAdvisorName) {
    judgeAndLegalAdvisor.useAllocatedJudge();
    judgeAndLegalAdvisor.enterLegalAdvisorName(legalAdvisorName);
  },

  enterDirections(directions) {
    I.click(this.fields.directionsNeeded.options.yes);
    I.fillField(this.fields.directions, directions);
  },

  enterExclusionClause(exclusionClause) {
    I.click(this.fields.exclusionClauseNeeded.options.yes);
    I.fillField(this.fields.exclusionClause, exclusionClause);
  },

  enterNumberOfMonths(numOfMonths) {
    I.fillField(this.fields.months, numOfMonths);
  },

  async enterChildrenDescription(description) {
    within(this.fields.epo.childrenDescription.radioGroup, () => {
      I.click(locate('label').withText('Yes'));
    });

    await I.fillField(this.fields.epo.childrenDescription.description, description);
  },

  selectEpoType(type) {
    within(this.fields.epo.type, () => {
      I.click(locate('label').withText(type));
    });
  },

  enterRemovalAddress(address) {
    within(this.fields.epo.removalAddress, () => {
      postcodeLookup.enterAddressManually(address);
    });
  },

  includePhrase(option) {
    within(this.fields.epo.includePhrase, () => {
      I.click(locate('label').withText(option));
    });
  },

  enterEpoEndDate(date) {
    I.fillDateAndTime(date, this.fields.epo.endDate.id);
  },

  async selectEndOfProceedings() {
    within(this.fields.interimEndDate.id, () => {
      I.click(locate('label').withText(this.fields.interimEndDate.options.endOfProceedings));
    });
  },

  async enterDateOfIssue(date) {
    I.fillDate(date);
  },

  async selectAndEnterNamedDate(date) {
    await within(this.fields.interimEndDate.id, () => {
      I.click(locate('label').withText(this.fields.interimEndDate.options.namedDate));
    });
    I.click(this.fields.interimEndDate.options.namedDate);
    I.fillField(this.fields.interimEndDate.endDate.day, date.day);
    I.fillField(this.fields.interimEndDate.endDate.month, date.month);
    I.fillField(this.fields.interimEndDate.endDate.year, date.year);
  },

  async selectChildren(children = []) {
    for (let child of children) {
      within(this.fields.childSelector.selector(child), () => {
        I.click(locate('label').withText(this.fields.childSelector.selectorText));
      });
    }
  },

  async selectCareOrder(careOrders = []) {
    for (let order of careOrders) {
      within(this.fields.careOrderSelector.selector(order), () => {
        I.click(locate('label').withText(this.fields.careOrderSelector.selectorText));
      });
    }
  },

  async useAllChildren() {
    within(this.fields.allChildren.id, () => {
      I.click(locate('label').withText(this.fields.allChildren.options.yes));
    });
  },

  async notAllChildren() {
    within(this.fields.allChildren.id, () => {
      I.click(locate('label').withText(this.fields.allChildren.options.no));
    });
  },

  closeCaseFromOrder(closeCase) {
    if (closeCase) {
      I.click(this.fields.closeCase.options.yes);
    } else {
      I.click(this.fields.closeCase.options.no);
    }
  },
};
