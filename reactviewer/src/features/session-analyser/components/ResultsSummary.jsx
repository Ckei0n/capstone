import React from 'react';
import './ResultsSummary.css';

// Displays summary statistics from API responses.

// Conditionally renders summary statistics in formatted layout
const ResultsSummary = ({ results }) => {
  if (!results) return null;

  return (
    <div className="results-summary">
      <h2>Results Summary</h2>
      <p><strong>Total Number of Unique Sessions:</strong> {results.totalUniqueSessions}</p>
      <p><strong>Snort SID Hits:</strong> {results.totalSnortHits}</p>
    </div>
  );
};

export default ResultsSummary;