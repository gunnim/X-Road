
module.exports = {
  tags: ['ss', 'clients'],
  'Security server clients list': browser => {
    const frontPage = browser.page.ssFrontPage();
    const mainPage = browser.page.ssMainPage();
    const clientsTab = mainPage.section.clientsTab

    // Open SUT and check that page is loaded
    frontPage
      .navigate();
    browser.waitForElementVisible('#app');

    // Enter valid credentials
    frontPage
      .clearUsername()
      .clearPassword()
      .enterUsername(browser.globals.login_usr)
      .enterPassword(browser.globals.login_pwd)
      .signin();

    // Verify successful login and default tab
    browser.waitForElementVisible(clientsTab);

    // Verify default sorting and list items
    browser
      .useXpath()
      .waitForElementVisible('//th[span[contains(text(),"Name")] and contains(@class, "active") and contains(@aria-sort, "ascending")]')
      .waitForElementVisible('(//tbody/tr)[1]//span[contains(text(),"VRK (Owner)")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"TestService")]')
      .waitForElementVisible('(//tbody/tr)[3]//div[contains(text(),"SAVED")]')
      .useCss();

    // Re-sort by name and verify re-sorted list item positions
    clientsTab.clickNameHeader();
    browser.useXpath()
      .waitForElementVisible('//th[span[contains(text(),"Name")] and contains(@class, "active") and contains(@aria-sort, "descending")]')
       .waitForElementVisible('//th[span[contains(text(),"ID")] and contains(@aria-sort, "none")]')
      .waitForElementVisible('(//tbody/tr)[1]//div[contains(text(),"SAVED")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"VRK (Owner)")]')
      .waitForElementVisible('(//tbody/tr)[3]//span[contains(text(),"TestService")]')

      .useCss();

    // Sort by ID and verify new sorting and list items
    clientsTab.clickIDHeader();
    browser.useXpath()
      .waitForElementVisible('//th[span[contains(text(),"ID")] and contains(@class, "active") and contains(@aria-sort, "ascending")]')
      .waitForElementVisible('//th[contains(@aria-sort, "none") and span[contains(text(),"Name")]]')
      .waitForElementVisible('(//tbody/tr)[1]//span[contains(text(),"VRK (Owner)")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"TestService")]')
      .waitForElementVisible('(//tbody/tr)[3]//div[contains(text(),"SAVED")]')
      .useCss();
    
    // Re-sort by ID and verify list items 
    clientsTab.clickIDHeader();
    browser.useXpath()
      .waitForElementVisible('//th[span[contains(text(),"ID")] and contains(@class, "active") and contains(@aria-sort, "descending")]')
      .waitForElementVisible('(//tbody/tr)[1]//div[contains(text(),"SAVED")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"TestService")]')
      .waitForElementVisible('(//tbody/tr)[3]//span[contains(text(),"VRK (Owner)")]')
      .useCss();

    // Sort by Status and verify items
    clientsTab.clickStatusHeader();
    browser.useXpath()
      .waitForElementVisible('//th[span[contains(text(),"Status")] and contains(@class, "active") and contains(@aria-sort, "ascending")]')
      .waitForElementVisible('(//tbody/tr)[1]//span[contains(text(),"VRK (Owner)")]')
      .waitForElementVisible('(//tbody/tr)[1]//div[contains(text(),"REGISTERED")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"TestService")]')
      .waitForElementVisible('(//tbody/tr)[3]//div[contains(text(),"SAVED")]')
      .useCss();

    // Re-sort by Status and verify list items
    clientsTab.clickStatusHeader();
    browser.useXpath()
      .waitForElementVisible('//th[span[contains(text(),"Status")] and contains(@class, "active") and contains(@aria-sort, "descending")]')
      .waitForElementVisible('(//tbody/tr)[1]//div[contains(text(),"SAVED")]')
      .waitForElementVisible('(//tbody/tr)[2]//span[contains(text(),"VRK (Owner)")]')
      .waitForElementVisible('(//tbody/tr)[3]//span[contains(text(),"TestService")]')
      .useCss();

    // Verify sorting by 
    browser
      .end()
  }
};
