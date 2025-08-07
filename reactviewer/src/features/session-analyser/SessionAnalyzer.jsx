import React, { useState } from 'react';
import NavigationHeader from '../../shared/components/NavigationHeader';
import SessionForm from './components/SessionForm';
import ErrorDisplay from '../../shared/components/ErrorDisplay';
import ResultsSummary from './components/ResultsSummary';
import SessionDetailsTable from './components/tables/SessionDetailsTable';
import TimeseriesChart from './components/TimeseriesChart';
import { fetchSessionData } from './services/sessionApiService';
import '../../styles/index.css';

// Manages global state, handles business logic,
function SessionAnalyzer() {
  const [startDate, setStartDate] = useState('');
  const [endDate, setEndDate] = useState('');
  const [results, setResults] = useState(null);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [selectedDay, setSelectedDay] = useState(null);

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setResults(null);
    setSelectedDay(null); // Clear any selected day when new search
    setLoading(true);

    try {
      const data = await fetchSessionData(startDate, endDate);
      setResults(data);
    } catch (err) {
      console.error('Fetch error:', err);
      setError(err.message || 'Failed to fetch data.');
    } finally {
      setLoading(false);
    }
  };

  const handleDayClick = (dayData) => {
    setSelectedDay(dayData);
  };

  const handleCloseTable = () => {
    setSelectedDay(null);
  };

  return (
    <div className="container">
      <NavigationHeader />
      
      <h1>Session Analyzer</h1>
      
      <SessionForm
        startDate={startDate}
        setStartDate={setStartDate}
        endDate={endDate}
        setEndDate={setEndDate}
        onSubmit={handleSubmit}
        loading={loading}
      />

      <ErrorDisplay error={error} />

      {results && (
        <div className="results card-white">
          <ResultsSummary results={results} />
          <TimeseriesChart 
            data={results.timeseriesData} 
            onDayClick={handleDayClick}
          />
          
          {selectedDay && (
            <SessionDetailsTable
              selectedDay={selectedDay}
              onClose={handleCloseTable}
              dateRange={{ startDate, endDate }}
            />
          )}
        </div>
      )}
    </div>
  );
}

export default SessionAnalyzer;