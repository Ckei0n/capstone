import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';

import './UploadPage.css';

const UploadPage = () => {
    //// State management
    const [files, setFiles] = useState([]);
    const [progress, setProgress] = useState(0);
    const [message, setMessage] = useState("");
    const [csrfToken, setCsrfToken] = useState(null);

    // Fetch CSRF token on component mount
    useEffect(() => {
        const fetchCsrfToken = async () => {
        try {
            // Request CSRF token from server
            const response = await axios.get('/api/csrf', {
            withCredentials: true //include cookies in request
            });
            setCsrfToken(response.data); // Store token in state for later use
        } catch (error) {
            console.error('Failed to fetch CSRF token:', error);
            setMessage("Failed to initialize security token");
        }
        };

        fetchCsrfToken();
    }, []); // Empty dependency array = run once on mount

    //Triggered when user selects files via file input, Updates component state with selected files
    const handleFileChange = (e) => {
        setFiles(e.target.files); //FileList object
    };

    const handleUpload = async () => {
        if (!csrfToken) {
        setMessage("Security token not available. Please refresh the page.");
        return;
    }
        const formData = new FormData();
        for (let i = 0; i < files.length; i++) { // Add all selected files to form data, Server expects files under "files" field name
            formData.append("files", files[i]);
        }

        try {
            const response = await axios.post("/api/import", formData, {
                headers: {
                    "Content-Type": "multipart/form-data", // Set content type for file uploads
                    [csrfToken.headerName]: csrfToken.token // // Include CSRF token in header, header name comes from server
                },
                withCredentials: true, // Include cookies for session management
                onUploadProgress: (progressEvent) => {
                    const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total); // Calculate percentage: (bytes uploaded / total bytes) * 100
                    setProgress(percent);
                },
            });
            setMessage(response.data);
        } catch (error) {
            setMessage("Upload failed: " + error.message);
        }
    };

    return (
        <>
        <div className="nav-links">
            <Link to="/">← Back to Analyzer</Link>
        </div>
         <div className="file-uploader">
            <h2>Upload GZ Files</h2>
            <label htmlFor="file-input">Choose files:</label>
            <input 
                id="file-input" 
                type="file" 
                multiple 
                onChange={handleFileChange} 
            />
            <button onClick={handleUpload}>Upload</button>
            <progress value={progress} max="100" />
            <p>{progress}%</p>
            {message && <p>{message}</p>}
        </div>
        </>
    );
};

export default UploadPage;