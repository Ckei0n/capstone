import React, { useState, useEffect } from 'react';
import axios from 'axios';
import { Link } from 'react-router-dom';

import './UploadPage.css';

const UploadPage = () => {
    const [files, setFiles] = useState([]);
    const [progress, setProgress] = useState(0);
    const [message, setMessage] = useState("");
    const [csrfToken, setCsrfToken] = useState(null);

    // Fetch CSRF token on component mount
    useEffect(() => {
        const fetchCsrfToken = async () => {
        try {
            const response = await axios.get('/api/csrf', {
            withCredentials: true
            });
            setCsrfToken(response.data);
        } catch (error) {
            console.error('Failed to fetch CSRF token:', error);
            setMessage("Failed to initialize security token");
        }
        };

        fetchCsrfToken();
    }, []);

    const handleFileChange = (e) => {
        setFiles(e.target.files);
    };

    const handleUpload = async () => {
        if (!csrfToken) {
        setMessage("Security token not available. Please refresh the page.");
        return;
    }
        const formData = new FormData();
        for (let i = 0; i < files.length; i++) {
            formData.append("files", files[i]);
        }

        try {
            const response = await axios.post("/api/import", formData, {
                headers: {
                    "Content-Type": "multipart/form-data",
                    [csrfToken.headerName]: csrfToken.token
                },
                withCredentials: true, // Important for cookies
                onUploadProgress: (progressEvent) => {
                    const percent = Math.round((progressEvent.loaded * 100) / progressEvent.total);
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