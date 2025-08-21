import React from 'react';
import './SessionForm.css';



// renders the date range input form for querying session data, used to trigger an API call to fetch session analytics for the specified date range.

const SessionForm = ({ 
  startDate, //string value of start date
  setStartDate, //function to update start date state
  endDate, //string value of end date
  setEndDate, //function to update the end date state
  onSubmit, //function to trigger data retrieval after form is submitted
  loading  //checks if an api call is in progress
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