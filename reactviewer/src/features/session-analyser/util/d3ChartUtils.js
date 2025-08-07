
import * as d3 from 'd3';

export const CHART_CONFIG = {
  margin: { top: 20, right: 200, bottom: 60, left: 80 },
  width: 1200,
  height: 500,
  pointRadius: 8,
  pointHoverRadius: 12,
  strokeWidth: 2,
  hoverStrokeWidth: 3
};

export const processDailyData = (dailyData) => {
  if (!dailyData || !Array.isArray(dailyData)) return { processedData: [], communityData: [] };
  
  // Filter for days with hits and sort by date
  const daysWithHits = dailyData
    .filter(d => d.hitCount && d.hitCount > 0)
    .sort((a, b) => new Date(a.timestamp) - new Date(b.timestamp));
  
  // Process daily data, each item represents one day with total hits
  const processedData = daysWithHits.map(d => ({
      date: new Date(d.timestamp),
      hitCount: d.hitCount,
      dateString: d.date,
      singaporeDate: d.singaporeDate || d.date,
      communityIds: d.communityIds || [],
      communityIdHitCounts: d.communityIdHitCounts || {},
      sids: d.sids || [],
      sampleSessions: d.sampleSessions || []
    }));
  
  // Create community ID data for multi-line chart
  const communityData = processCommunityDataComplete(daysWithHits)
  
  return { processedData, communityData };
};

// Process data to create complete lines for each community ID
export const processCommunityDataComplete = (dailyData) => {
  if (!dailyData || dailyData.length === 0) return [];
  
  // Get all unique dates and community IDs
  const allDates = dailyData.map(day => ({
    date: new Date(day.timestamp),
    dateString: day.date,
    singaporeDate: day.singaporeDate || day.date
  })).sort((a, b) => a.date - b.date);
  
  const allCommunityIds = new Set();
  dailyData.forEach(day => {
    if (day.communityIdHitCounts) {
      Object.keys(day.communityIdHitCounts).forEach(id => allCommunityIds.add(id));
    } else if (day.communityIds) {
      day.communityIds.forEach(id => allCommunityIds.add(id));
    }
  });
  
  // Create a complete line for each community ID with all dates
  const communityLines = [];
  
  allCommunityIds.forEach(communityId => {
    const dataPoints = [];
    
    // For each date, find the hit count for this community ID
    allDates.forEach(dateInfo => {
      // Find the day data for this date
      const dayData = dailyData.find(day => 
        (day.singaporeDate || day.date) === dateInfo.singaporeDate
      );
      
      let communityCount = 0;
      if (dayData) {
        if (dayData.communityIdHitCounts && dayData.communityIdHitCounts[communityId]) {
          communityCount = dayData.communityIdHitCounts[communityId];
        } else if (dayData.communityIds && dayData.communityIds.includes(communityId)) {
          // Fallback: estimate if exact counts not available
          communityCount = Math.ceil(dayData.hitCount / dayData.communityIds.length);
        }
      }
      
      // Add data point for every date (even if count is 0 for continuity)
      dataPoints.push({
        date: dateInfo.date,
        hitCount: communityCount,
        singaporeDate: dateInfo.singaporeDate,
        dateString: dateInfo.dateString
      });
    });
    
    if (dataPoints.some(p => p.hitCount > 0)) { // Only include if there's at least one hit
      communityLines.push({
        communityId: communityId,
        dataPoints: dataPoints,
        color: generateColorForCommunityId(communityId)
      });
    }
  });
  
  return communityLines;
};


export const generateColorForCommunityId = (communityId) => {
  // Use a hash of the community ID to generate consistent colors
  let hash = 0;
  for (let i = 0; i < communityId.length; i++) {
    const char = communityId.charCodeAt(i);
    hash = ((hash << 5) - hash) + char;
    hash = hash & hash; // Convert to 32-bit integer
  }
  
  const colors = [
    '#1f77b4', // blue
    '#ff7f0e', // orange  
    '#2ca02c', // green
    '#d62728', // red
    '#9467bd', // purple
    '#8c564b', // brown
    '#e377c2', // pink
    '#7f7f7f', // gray
    '#bcbd22', // olive
    '#17becf', // cyan
    '#aec7e8', // light blue
    '#ffbb78', // light orange
    '#98df8a', // light green
    '#ff9896', // light red
    '#c5b0d5', // light purple
    '#c49c94', // light brown
    '#f7b6d3', // light pink
    '#c7c7c7', // light gray
    '#dbdb8d', // light olive
    '#9edae5'  // light cyan
  ];
  
  return colors[Math.abs(hash) % colors.length];
};

export const createScales = (communityData, chartWidth, chartHeight) => {
  // Get all dates from all community lines
  const allDates = [];
  const allHitCounts = [];
  
  communityData.forEach(community => {
    community.dataPoints.forEach(point => {
      allDates.push(point.date);
      allHitCounts.push(point.hitCount);
    });
  });
  
  // X-axis: Time scale for dates
  const xScale = d3.scaleTime()
    .domain(d3.extent(allDates))
    .range([0, chartWidth]);

  // Y-axis: Linear scale for hit counts
  const maxHits = d3.max(allHitCounts) || 0;
  const yScale = d3.scaleLinear()
    .domain([0, maxHits * 1.1]) // Add 10% padding at top
    .range([chartHeight, 0]);

  return { xScale, yScale };
};

export const createAxes = (g, xScale, yScale, chartWidth, chartHeight, margin, communityData) => {
  // Get all unique dates for tick generation
  const allDates = [];
  communityData.forEach(community => {
    community.dataPoints.forEach(point => {
      allDates.push(point.date);
    });
  });
  
  const uniqueDates = [...new Set(allDates.map(d => d.getTime()))].map(t => new Date(t));
  const dataPointCount = uniqueDates.length;
  
  // Create smart tick generation for X-axis based on data density
  let xAxisTicks;
  
  if (dataPointCount <= 10) {
    // For sparse data, show all dates
    xAxisTicks = uniqueDates;
  } else if (dataPointCount <= 30) {
    // For medium data, use every other day or smart intervals
    xAxisTicks = d3.timeDay.every(Math.ceil(dataPointCount / 10));
  } else {
    // For dense data, use automatic ticking
    xAxisTicks = d3.timeDay.every(1);
  }

  // X-axis: Date axis with custom tick values for sparse data
  const xAxis = d3.axisBottom(xScale)
    .tickFormat(d3.timeFormat("%Y-%m-%d"));
    
  // Set custom tick values if we have sparse data
  if (dataPointCount <= 10) {
    xAxis.tickValues(xAxisTicks);
  } else {
    xAxis.ticks(xAxisTicks);
  }

  // Y-axis: Hit count axis
  const yAxis = d3.axisLeft(yScale)
    .tickFormat(d3.format("d")) // Integer formatting
    .ticks(10);

  g.append("g")
    .attr("class", "x-axis")
    .attr("transform", `translate(0,${chartHeight})`)
    .call(xAxis)
    .selectAll("text")
    .style("text-anchor", "end")
    .attr("dx", "-.8em")
    .attr("dy", ".15em")
    .attr("transform", "rotate(-45)");

  g.append("g")
    .attr("class", "y-axis")
    .call(yAxis);

  // Add axis labels
  g.append("text")
    .attr("transform", "rotate(-90)")
    .attr("y", 0 - margin.left)
    .attr("x", 0 - (chartHeight / 2))
    .attr("dy", "1em")
    .style("text-anchor", "middle")
    .style("font-size", "14px")
    .style("font-weight", "bold")
    .text("Daily Snort Hit Count");

  g.append("text")
    .attr("transform", `translate(${chartWidth / 2}, ${chartHeight + margin.bottom - 10})`)
    .style("text-anchor", "middle")
    .style("font-size", "14px")
    .style("font-weight", "bold")
    .text("Date");
};

export const createTooltip = () => {
  return d3.select("body").append("div")
    .attr("class", "d3-tooltip")
    .style("position", "absolute")
    .style("visibility", "hidden")
    .style("background", "rgba(0, 0, 0, 0.8)")
    .style("color", "white")
    .style("border-radius", "5px")
    .style("padding", "10px")
    .style("font-size", "12px")
    .style("max-width", "300px")
    .style("z-index", "1000");
};

export const formatTooltipContent = (d, communityId) => {
  // Use the explicit Singapore date instead of converting timestamp
  const dateStr = d.singaporeDate || d.dateString || 'N/A';
  
  return `
    <strong>Date:</strong> ${dateStr}<br/>
    <strong>Community ID:</strong> ${communityId}<br/>
    <strong>Snort Hits:</strong> ${d.hitCount}
  `;
};

// legend positioning
export const createLegend = (g, communityData, margin, chartWidth) => {
  const legend = g.append("g")
    .attr("class", "legend")
    .attr("transform", `translate(${chartWidth + 30}, 20)`);
  
  const legendItems = legend.selectAll(".legend-item")
    .data(communityData)
    .enter()
    .append("g")
    .attr("class", "legend-item")
    .attr("transform", (d, i) => `translate(0, ${i * 18})`);
  
  legendItems.append("circle")
    .attr("cx", 6)
    .attr("cy", 6)
    .attr("r", 4)
    .style("fill", d => d.color);
  
  // Add text label with better formatting
  legendItems.append("text")
    .attr("x", 16)
    .attr("y", 6)
    .attr("dy", "0.35em")
    .style("font-size", "11px")
    .style("font-family", "Arial, sans-serif")
    .text(d => {
      // Show first and last few characters of community ID like "1:9a23...7b24"
      if (d.communityId.length > 20) {
        return d.communityId.substring(0, 8) + '...' + d.communityId.substring(d.communityId.length - 4);
      }
      return d.communityId;
    });
  
  return legend;
};

export const cleanupTooltips = () => {
  d3.selectAll(".d3-tooltip").remove();
};