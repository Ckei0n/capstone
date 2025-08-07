import React from 'react';
import { Link } from 'react-router-dom';
import './NavigationHeader.css';

// Purpose: Renders navigation links for the application. 
const NavigationHeader = () => {
  return (
    <div className="nav-links">
      <Link to="/api/import">Go to File Uploader</Link>
    </div>
  );
};

export default NavigationHeader;