import React from 'react';
import './SessionTableHeader.css';

const SessionTableHeader = ({
  selectedDate,
  sessionCount,
  totalAvailable,
  totalSessions,
  showingAll,
  hasMoreSessions,
  loading,
  onLoadAll,
  onClose,
  selectedCommunityId
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
          Showing {sessionCount} of {totalAvailable} sessions
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
                ? `Load All Sessions for Community ID` 
                : `Load All ${totalSessions} Sessions`
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