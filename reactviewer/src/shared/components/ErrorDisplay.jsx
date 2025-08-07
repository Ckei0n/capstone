import React from 'react';

//Displays error messages to users in a consistent format.

const ErrorDisplay = ({ error }) => {
  if (!error) return null;

  return (
    <p className="error">{error}</p>
  );
};

export default ErrorDisplay;