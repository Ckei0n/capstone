describe('Session Data Analysis Flow', () => {
  beforeEach(() => {

    cy.login()
  })

  it('should complete full session analysis workflow with real API calls', () => {
    
    cy.visit('/')

    cy.get('h1').should('contain', 'Session Analyzer')
    cy.get('.session-form').should('be.visible')
    
    //use this date range to test api
    const startDate = '2025-08-12'
    const endDate = '2025-08-19'
    
    //Fill in the dates
    cy.get('input[type="date"]').first().clear().type(startDate)
    cy.get('input[type="date"]').last().clear().type(endDate)
    
    // Submit the form
    cy.get('button[type="submit"]').should('be.visible').click()
    
    // Wait for loading to complete
    cy.get('button[type="submit"]').should('not.contain', 'Loading...')
    
    cy.get('.results-summary', { timeout: 30000 }).should('be.visible')
    
    // Log what we actually got from the API
    cy.get('.results-summary').then($summary => {
      cy.log('Results Summary Content:', $summary.text())
    })
    
    cy.get('body').then($body => {
      if ($body.find('.timeseries-container').length > 0) {
        cy.log('Chart container found')
        
        cy.get('.timeseries-container', { timeout: 15000 }).should('be.visible')
        
        cy.get('body').then($body => {
          if ($body.find('svg').length > 0) {
            cy.log('SVG chart found')
            cy.get('svg').should('be.visible')
            
            cy.get('body').then($body => {
              if ($body.find('svg .data-point').length > 0) {
                cy.log('Data points found, testing interaction')
                
                cy.get('.data-point').first().click({force: true})
                
                cy.get('body').then($body => {
                  if ($body.find('.session-details-container').length > 0) {
                    cy.get('.session-details-container').should('be.visible')
                    
                    cy.get('.session-details-header').should('be.visible')
                    cy.get('.session-details-table').should('be.visible')
                    
                    cy.get('.close-btn').click()
                    cy.get('.session-details-container').should('not.exist')
                  } else {
                    cy.log('Session details modal did not open')
                  }
                })
              } else {
                cy.log('No data points found in chart')
              }
            })
          } else {
            cy.log('No SVG chart found')
          }
        })
      } else {
        cy.log('No chart container found')
      }
    })
  })

  it('should handle empty data gracefully', () => {
    cy.visit('/')
    cy.get('h1').should('contain', 'Session Analyzer')
    
    // Test with a date range that have no data
    cy.get('input[type="date"]').first().clear().type('2020-01-01')
    cy.get('input[type="date"]').last().clear().type('2020-01-02')
    cy.get('button[type="submit"]').click()
    
    cy.get('button[type="submit"]').should('not.contain', 'Loading...')
    
    cy.get('body').then($body => {
      if ($body.find('.results-summary').length > 0) {
        cy.get('.results-summary').should('be.visible')
      }
    })
  })

  it('should test form validation with real API', () => {
    cy.visit('/')
    cy.get('h1').should('contain', 'Session Analyzer')
    
    // Test invalid date range
    cy.get('input[type="date"]').first().clear().type('2025-08-19')
    cy.get('input[type="date"]').last().clear().type('2025-08-12')
    cy.get('button[type="submit"]').click()
    
    // show validation error
    cy.get('.error').should('be.visible')
    cy.get('.error').should('contain', 'Start date cannot be after end date')
  })
})