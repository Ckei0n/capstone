import React from 'react';
import './SessionForm.css';

//Handles all form input rendering and user interactions. 

const SessionForm = ({ 
  startDate, 
  setStartDate, 
  endDate, 
  setEndDate, 
  onSubmit, 
  loading 
}) => {
  return (
    <form onSubmit={onSubmit} className="session-form">
      <div className="form-group">
        <label>Start Date:</label>
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          required
        />
      </div>
      <div className="form-group">
        <label>End Date:</label>
        <input
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          required
        />
      </div>
      <button type="submit" disabled={loading} className="submit-btn">
        {loading ? 'Loading...' : 'Submit'}
      </button>
    </form>
  );
};

export default SessionForm;