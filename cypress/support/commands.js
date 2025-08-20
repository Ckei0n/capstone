Cypress.Commands.add('login', () => {
  cy.session('userSession', () => {
    
    cy.visit('/')
    
    cy.url().should('include', '/realms/', { timeout: 30000 })

    cy.get('#kc-form-login', { timeout: 20000 }).should('be.visible')
    cy.get('#username', { timeout: 10000 }).should('be.visible')
    cy.get('#password', { timeout: 10000 }).should('be.visible')
    cy.get('#kc-login', { timeout: 10000 }).should('be.visible').and('not.be.disabled')
    
    cy.get('#username').clear().type(Cypress.env('TEST_USERNAME'))
    cy.get('#password').clear().type(Cypress.env('TEST_PASSWORD'))
    
    cy.get('#username').should('have.value', Cypress.env('TEST_USERNAME'))
    cy.get('#password').should('have.value', Cypress.env('TEST_PASSWORD'))
    
    cy.get('#kc-login').click()
    
    
    cy.url().should('not.include', '/realms/', { timeout: 60000 })
    cy.url().should('not.include', '/auth/', { timeout: 10000 })
    
    cy.get('h1', { timeout: 30000 }).should('contain', 'Session Analyzer')
    
    Cypress.config('pageLoadTimeout', 60000)
  }, {
    validate() {
      cy.visit('/')
      cy.url().should('not.include', '/realms/')
      cy.get('h1').should('contain', 'Session Analyzer')
    }
  })
})

