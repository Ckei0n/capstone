import React from 'react';
import './SessionTableHeader.css';


//header section for the session table. 
const SessionTableHeader = ({
  selectedDate, //string value of date
  sessionCount, // number of network sessions being displayed on table
  totalSessions, //total number of sessions in that day
  showingAll, //indicate if all sessions have been shown
  hasMoreSessions, //indicate if there are more sessions that can be loaded (used as a flag for btn)
  loading, // boolean for loading state of the load all button
  onLoadAll, //function to trigger loading all sessions
  onClose, //function to close the table
  selectedCommunityId //show community id of specific data point
}) => {
  const formatCommunityId = (communityId) => {
    if (!communityId) return '';
    // Show first and last few characters like "1:9a23...7b24"
    if (communityId.length > 20) {
      return communityId.substring(0, 8) + '...' + communityId.substring(communityId.length - 4);
    }
    return communityId;
  };

  return (
    // Main title showing the date and optional Community ID filter
    <div className="session-details-header" data-cy="session-details-header">
      <h3>
        Session Details for {selectedDate}
        {selectedCommunityId && (
          <span style={{ fontSize: '0.8em', color: '#666', marginLeft: '10px' }}>
            (Community ID: {formatCommunityId(selectedCommunityId)})
          </span>
        )}
      </h3>
      <div className="session-details-controls">
        <span className="session-count">
          Showing {sessionCount} of {totalSessions} sessions
          {selectedCommunityId && ' for selected Community ID'}
        </span>
        {!showingAll && hasMoreSessions && (
          <button 
            onClick={onLoadAll} 
            disabled={loading}
            className="load-all-btn btn-sm"
            data-cy="load-all-btn"
          >
            {loading ? 'Loading...' : 
              selectedCommunityId 
                ? `Load All Sessions for Community ID` // When filtering by Community ID
                : `Load All ${totalSessions} Sessions` // When showing all sessions for the day
            }
          </button>
        )}
        <button 
          onClick={onClose} 
          className="close-btn"
          aria-label="Close session details"
          data-cy="close-btn"
        >
          ×
        </button>
      </div>
    </div>
  );
};

export default SessionTableHeader;