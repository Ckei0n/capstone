import React from 'react';
import './SessionTableBody.css';

// Displays the actual session data in a table format. Each row represents one network session that triggered a Snort alert.

// sessions: Array of session object to display, totalAvailable: total number of session available, used if the specific day has no sessions.
const SessionTableBody = ({ sessions, totalAvailable }) => {
  const formatTimestamp = (timestamp) => {
    return new Date(timestamp).toLocaleString();
  };

  // Formats array fields for display in table cells, converts arrays to comma-separated strings, used for fields like SIDs and Snort messages that can be arrays
  const formatArrayField = (field) => {
    if (Array.isArray(field)) {
      return field.join(', ');
    }
    return field || 'N/A';
  };

  if (totalAvailable === 0) {
    return (
      <div className="no-sessions">
        No session details available for this day.
      </div>
    );
  }

  return (
    <div className="table-container" data-cy="table-container">
      <table className="session-details-table" data-cy="session-details-table">
        <thead>
          <tr>
            <th>Timestamp</th>
            <th>Index</th>
            <th>Community ID</th>
            <th>SID</th>
            <th>Source</th>
            <th>Destination</th>
            <th>Message</th>
          </tr>
        </thead>
        <tbody data-cy="session-table-body">
          {sessions.map((session, index) => (
            <tr key={session.documentId || index} data-cy={`session-row-${index}`}>
              <td>{formatTimestamp(session.timestamp)}</td>
              <td className="index-cell">{session.indexName}</td>
              <td className="community-id-cell">{session.communityId || 'N/A'}</td>
              <td>{formatArrayField(session.sid)}</td>
              <td className="endpoint-cell">
                {session.sourceIp}:{session.sourcePort}
              </td>
              <td className="endpoint-cell">
                {session.destIp}:{session.destPort}
              </td>
              <td className="message-cell">
                {formatArrayField(session.snortMessage)}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
};

export default SessionTableBody;