import React from 'react';
import { useSessionData } from '../hooks/useSessionData';
import SessionTableHeader from './SessionTableHeader';
import SessionTableBody from './SessionTableBody';
import './SessionDetailsTable.css';


//used to display detailed session data. renders when user clicks on a data point in the chart.
const SessionDetailsTable = ({ 
  selectedDay, // Object containing the clicked day's data
  onClose, // Function to close the detail view and return to chart
  dateRange // Object with startDate/endDate for API calls
}) => {
  // Handle session data loading
  const {
    allAvailableSessions,
    loading,
    error,
    showingAll,
    loadAllSessions
  } = useSessionData(selectedDay, dateRange);

  // return if no selected day
  if (!selectedDay) return null;

  const totalSessions = selectedDay.hitCount || 0;

  return (
    <div className="session-details-container" data-cy="session-details-container">
      <SessionTableHeader
        selectedDate={selectedDay.singaporeDate}
        sessionCount={allAvailableSessions.length}
        totalSessions={totalSessions}
        showingAll={showingAll}
        hasMoreSessions={selectedDay.hasMoreSessions}
        loading={loading}
        onLoadAll={loadAllSessions}
        onClose={onClose}
        selectedCommunityId={selectedDay.selectedCommunityId}
      />

      {error && <div className="error" data-cy="session-error">{error}</div>}

      <SessionTableBody
        sessions={allAvailableSessions}
        totalAvailable={allAvailableSessions.length}
      />
    </div>
  );
};

export default SessionDetailsTable;