describe('Upload Gzip files flow', () => {
  beforeEach(() => {

    cy.login()
  })

  it('should be able to upload gzip files to opensearch', () => {
    
    cy.visit('/');

    cy.get('h1').should('contain', 'Session Analyzer');
    cy.get('.nav-links').should('be.visible');
    cy.get('.session-form').should('be.visible');

    cy.intercept('GET', '/api/csrf').as('getCsrfToken');

    cy.get('[data-cy=upload-link]').click();
    cy.url().should('include', '/api/import');
    cy.get('.file-uploader').should('be.visible');
    cy.wait('@getCsrfToken');

    cy.intercept('POST', '/api/import', (req) => {
        console.log('Upload request:', req.body);
    }).as('uploadFile');
  
    cy.get('[data-cy=file-input]').attachFile({
        filePath: 'fake_trend_sessions_utc_time.gz',
        encoding: 'binary', // Explicitly specify binary encoding
    });
    
    cy.get('[data-cy=upload-btn]').click();

    cy.contains('Successfully imported', { timeout: 10000 })
    .should('be.visible')
    .and('contain', 'documents from 1 file(s)')
    .and('contain', 'indices');

  })
})