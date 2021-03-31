function tabFieldSelector(pathToField) {
  let path = [].concat(pathToField);
  let fieldName = path.splice(-1, 1)[0];
  let selector = '//mat-tab-body';

  // if it is a simple case field then it will not have a complex-panel-[title|simple-field] class
  if (path.length === 0) {
    return `${selector}//tr[.//th/div[text()="${fieldName}"]]`;
  }

  path.forEach(step => {
    selector = `${selector}//*[@class="complex-panel" and .//*[@class="complex-panel-title" and .//*[text()="${step}"]]]`;
  }, this);

  return `${selector}//*[contains(@class,"complex-panel-simple-field") and .//th/span[text()="${fieldName}"]]`;
}

function seeInTab(I, pathToField, fieldValue) {
  const fieldSelector = tabFieldSelector(pathToField);

  if (Array.isArray(fieldValue)) {
    fieldValue.forEach((value, index) => {
      I.seeElement(locate(`${fieldSelector}//tr[${index + 1}]`).withText(value));
    });
  } else {
    I.seeElement(locate(fieldSelector).withText(fieldValue));
  }
}

function organisationTabFieldSelector(pathToField) {
  let path = [].concat(pathToField);
  let fieldName = path.splice(-1, 1)[0];
  let selector = '//mat-tab-body';

  path.forEach(step => {
    selector = `${selector}//*[@class="complex-panel" and .//*[@class="complex-panel-title" and .//*[text()="${step}"]]]`;
  }, this);

  return `${selector}//*[contains(@class,"complex-panel-compound-field") and ..//*[text()="${fieldName}:"]]`;
}

function seeOrganisationInTab(I, pathToField, fieldValue) {
  const fieldSelector = this.organisationTabFieldSelector(pathToField);

  if (Array.isArray(fieldValue)) {
    fieldValue.forEach((value) => {
      I.seeElement(locate(`${fieldSelector}//tr[1]`).withText(value));
    });
  } else {
    I.seeElement(locate(fieldSelector).withText(fieldValue));
  }
}

module.exports = {
  tabFieldSelector,
  seeInTab,
  seeOrganisationInTab,
  organisationTabFieldSelector,
};
