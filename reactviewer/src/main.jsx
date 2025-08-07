import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
import SessionAnalyzer from './features/session-analyser/SessionAnalyzer'
import { BrowserRouter, Routes, Route } from 'react-router-dom';
import UploadPage from './UploadPage';

createRoot(document.getElementById('root')).render(
  <StrictMode>
    <BrowserRouter>
      <Routes>
        <Route path="/" element={<SessionAnalyzer />} />
        <Route path="/api/import" element={<UploadPage />} />
      </Routes>
    </BrowserRouter>
  </StrictMode>
)
