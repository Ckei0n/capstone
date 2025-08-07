import { useState, useMemo } from 'react';
import { fetchDailySessionDetails } from '../../services/sessionApiService';


export const useSessionData = (selectedDay, dateRange) => {
  const [allSessions, setAllSessions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [showingAll, setShowingAll] = useState(false);

  // Get all available sessions (samples or all loaded sessions)
  const allAvailableSessions = useMemo(() => {
    
    const sessions = showingAll ? allSessions : (selectedDay?.sampleSessions || []);
    
    return sessions;
  }, [showingAll, allSessions, selectedDay]);

  const loadAllSessions = async () => {
    if (!selectedDay || !dateRange) {
      const missingData = [];
      if (!selectedDay) missingData.push('selectedDay');
      if (!dateRange) missingData.push('dateRange');
      
      const errorMsg = `Missing required data for loading sessions: ${missingData.join(', ')}`;
      setError(errorMsg);
      return;
    }

    setLoading(true);
    setError('');
    
    try {
      const data = await fetchDailySessionDetails(
        dateRange.startDate, 
        dateRange.endDate, 
        selectedDay.date
      );
      
      console.log("Fetched session details:", data);
      
      if (data && data.sessions) {
        let sessionsToUse = data.sessions;
        
        // Filter by community ID if one was selected
        if (selectedDay.selectedCommunityId) {
          sessionsToUse = data.sessions.filter(session => 
            session.communityId === selectedDay.selectedCommunityId
          );
          console.log(`Filtered ${data.sessions.length} sessions to ${sessionsToUse.length} for community ID: ${selectedDay.selectedCommunityId}`);
        }
        
        setAllSessions(sessionsToUse);
        setShowingAll(true);
      } else {
        setError("No session data returned from server");
      }
    } catch (err) {
      setError(err.message || 'Failed to load all sessions');
    } finally {
      setLoading(false);
    }
  };

  const resetData = () => {
    setAllSessions([]);
    setShowingAll(false);
    setError('');
    setLoading(false);
  };

  return {
    // Data state
    allAvailableSessions,
    loading,
    error,
    showingAll,
    
    // Data handlers
    loadAllSessions,
    resetData
  };
};