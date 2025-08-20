import React from 'react';
import { useSessionData } from '../hooks/useSessionData';
import { useSessionFilters } from '../hooks/useSessionFilters';
import SessionTableHeader from './SessionTableHeader';
import SessionTableBody from './SessionTableBody';
import './SessionDetailsTable.css';

const SessionDetailsTable = ({ 
  selectedDay, 
  onClose, 
  dateRange 
}) => {
  // Handle session data loading
  const {
    allAvailableSessions,
    loading,
    error,
    showingAll,
    loadAllSessions
  } = useSessionData(selectedDay, dateRange);

  // No filtering - just pass through the sessions
  const sessions = useSessionFilters(allAvailableSessions);

  // return if no selected day
  if (!selectedDay) return null;

  const totalSessions = selectedDay.hitCount || 0;

  return (
    <div className="session-details-container" data-cy="session-details-container">
      <SessionTableHeader
        selectedDate={selectedDay.date}
        sessionCount={sessions.length}
        totalAvailable={allAvailableSessions.length}
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
        sessions={sessions}
        totalAvailable={allAvailableSessions.length}
      />
    </div>
  );
};

export default SessionDetailsTable;