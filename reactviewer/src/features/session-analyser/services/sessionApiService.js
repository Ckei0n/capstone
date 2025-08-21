// This file handles all API communication.

// Base URL for all API endpoints, relative to current domain
const API_BASE_URL = '/api';

export const fetchSessionData = async (startDate, endDate) => {
    const url = new URL(`${API_BASE_URL}/sessions`, window.location.origin);
    url.searchParams.append('start', startDate);
    url.searchParams.append('end', endDate);
    
    const response = await fetch(url.toString());
    if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
    }
    
    const data = await response.json();
    if (data.error) {
        throw new Error(data.error);
    }
    
    return data;
};


// Fetches detailed session data for a specific day. Triggered when users click "Load All Sessions" in the session details table. It retrieves all session information for a single day
export const fetchDailySessionDetails = async (startDate, endDate, date) => {
    const url = new URL(`${API_BASE_URL}/sessions/daily-details`, window.location.origin);
    url.searchParams.append('start', startDate);
    url.searchParams.append('end', endDate);
    url.searchParams.append('date', date);
    
    const response = await fetch(url.toString());
    if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
    }
    
    const data = await response.json();
    if (data.error) {
        throw new Error(data.error);
    }
    
    return data;
};

// Uploads files to the server for data import
export const uploadFiles = async (files) => {
    // Create FormData object for file upload
    const formData = new FormData();
    files.forEach(file => {
        formData.append('files', file);
    });
    const response = await fetch(`${API_BASE_URL}/import`, {
        method: 'POST',
        body: formData
    });
    
    if (!response.ok) {
        throw new Error(`HTTP error! Status: ${response.status}`);
    }
    
    return response.text();
};