// Transforms raw API response data into structured format required for D3.js.

import { generateColorForCommunityId } from './colorUtils';


// Responsible for transforming API response data into chart-ready format.
export const processDailyData = (dailyData) => {
  if (!dailyData || !Array.isArray(dailyData)) {
    return { processedData: [], communityData: [] };
  }
  
  // Filter for days with snort hits and sort by date
  const daysWithHits = dailyData
    .filter(d => d.hitCount && d.hitCount > 0)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
  
  // Transform daily data into standardized format
  const processedData = daysWithHits.map(d => ({
    date: new Date(d.timestamp), // Convert to Date object for D3
    hitCount: d.hitCount, // Total hits for the day
    singaporeDate: d.singaporeDate, // Singapore timezone date
    communityIds: d.communityIds || [], // List of Community IDs active this day
    communityIdHitCounts: d.communityIdHitCounts || {}, // // Hit counts per Community ID
    sids: d.sids || [], //  Snort sids 
    sampleSessions: d.sampleSessions || [] // Sample session data for quick access
  }));
  
  // Create multi-line chart data grouped by Community ID
  const communityData = processCommunityDataComplete(daysWithHits);
  
  return { processedData, communityData };
};

// creates separate lines for each Community ID, ensures that each line has data points for all dates in the range (filling in zeros where no snorts occurred). 
export const processCommunityDataComplete = (dailyData) => {
  if (!dailyData || dailyData.length === 0) return [];
  
  // Get date range and all Community IDs
  const allDates = getAllDates(dailyData);
  const allCommunityIds = getAllCommunityIds(dailyData);
  
  // / Create lines for each Community ID
  return createCommunityLines(allDates, allCommunityIds, dailyData);
};

// Extracts and sorts all unique dates from the daily data
const getAllDates = (dailyData) => {
  return dailyData
    .map(day => ({
      date: new Date(day.timestamp),
      singaporeDate: day.singaporeDate || day.date
    }))
    .sort((a, b) => a.date - b.date);
};

// Extracts all unique Community IDs from the dataset
const getAllCommunityIds = (dailyData) => {
  const allCommunityIds = new Set();
  
  dailyData.forEach(day => {
    if (day.communityIdHitCounts) {
      Object.keys(day.communityIdHitCounts).forEach(id => allCommunityIds.add(id));
    }
  });
  
  return allCommunityIds;
};


// Creates individual lines for each Community ID
const createCommunityLines = (allDates, allCommunityIds, dailyData) => {
  const communityLines = [];
  
  allCommunityIds.forEach(communityId => {
    // Create data points for this Community ID across all dates
    const dataPoints = createDataPoints(allDates, communityId, dailyData);
    
    // // Only include Community IDs that have at least 1 sid
    if (dataPoints.some(p => p.hitCount > 0)) {
      communityLines.push({
        communityId: communityId,
        dataPoints: dataPoints,  // Time series data points
        color: generateColorForCommunityId(communityId) // color for this line
      });
    }
  });
  
  return communityLines;
};

// Creates data points for a specific Community ID across all dates
const createDataPoints = (allDates, communityId, dailyData) => {
  return allDates.map(dateInfo => {
    // Find the day's data that matches this date
    const dayData = dailyData.find(day => 
      day.singaporeDate === dateInfo.singaporeDate
    );
    
    let communityCount = 0;
    if (dayData) {
      communityCount = getCommunityCount(dayData, communityId);
    }
    
    // Create standardized data point
    return {
      date: dateInfo.date,
      hitCount: communityCount, // Number of hits for this Community ID
      singaporeDate: dateInfo.singaporeDate,
    };
  });
};

// Extracts hit count for a specific Community ID from day data
const getCommunityCount = (dayData, communityId) => {
  if (dayData.communityIdHitCounts && dayData.communityIdHitCounts[communityId]) {
    return dayData.communityIdHitCounts[communityId];
  } 

  return 0;
};