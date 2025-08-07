import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';
import './TimeseriesChart.css';

import {
  CHART_CONFIG,
  processDailyData,
  createScales,
  createAxes,
  createTooltip,
  formatTooltipContent,
  createLegend,
  cleanupTooltips
} from '../util/d3ChartUtils';

const TimeseriesChart = ({ data, onDayClick }) => {
  const svgRef = useRef();

  useEffect(() => {
    if (!data || !Array.isArray(data) || data.length === 0) return;

    const drawChart = (data) => {
      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();

      const { margin, width, height } = CHART_CONFIG;
      const chartWidth = width - margin.left - margin.right;
      const chartHeight = height - margin.top - margin.bottom;

      const g = svg
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", `translate(${margin.left},${margin.top})`);

      const { communityData } = processDailyData(data);
      
      // Show message if no data at all
      if (communityData.length === 0) {
        g.append("text")
          .attr("x", chartWidth / 2)
          .attr("y", chartHeight / 2)
          .attr("text-anchor", "middle")
          .style("font-size", "16px")
          .style("fill", "#666")
          .text("No data found in the selected date range");
        return;
      }

      const { xScale, yScale } = createScales(communityData, chartWidth, chartHeight);
      createAxes(g, xScale, yScale, chartWidth, chartHeight, margin, communityData);
      createLegend(g, communityData, margin, chartWidth);
      
      const tooltip = createTooltip();

      // Create line generator with better curve
      const line = d3.line()
        .x(d => xScale(d.date))
        .y(d => yScale(d.hitCount))
        .curve(d3.curveCardinal.tension(0.5)) // use to smooth the curve, lower is smoother
        .defined(d => d.hitCount !== null); // Handle missing data points

      // Draw lines for each community ID
      const communityLines = g.selectAll(".community-line")
        .data(communityData)
        .enter()
        .append("g")
        .attr("class", "community-line");

      // Add the line paths with better styling
      communityLines.append("path")
        .datum(d => d.dataPoints)
        .attr("class", "line")
        .attr("d", line)
        .style("fill", "none")
        .style("stroke", (d, i, nodes) => {
          const parentData = d3.select(nodes[i].parentNode).datum();
          return parentData.color;
        })
        .style("stroke-width", 2)
        .style("opacity", 0.8)
        .style("stroke-linejoin", "round")
        .style("stroke-linecap", "round");

      communityLines.each(function(communityLineData) {
        const communityGroup = d3.select(this);
        
        communityGroup.selectAll(".data-point")
          .data(communityLineData.dataPoints.filter(d => d.hitCount > 0)) // Only show points with data
          .enter()
          .append("circle")
          .attr("class", "data-point")
          .attr("cx", d => xScale(d.date))
          .attr("cy", d => yScale(d.hitCount))
          .attr("r", 3) //change the size of each data point
          .attr("fill", communityLineData.color)
          .attr("stroke", "white")
          .attr("stroke-width", 1.5)
          .style("cursor", "pointer")
          .style("opacity", 0.9)
          .on("mouseover", function(event, d) {
            handleMouseOver(this, event, d, communityLineData.communityId, tooltip, 5, 2);
          })
          .on("mousemove", function(event) {
            handleMouseMove(event, tooltip);
          })
          .on("mouseout", function() {
            handleMouseOut(this, tooltip, 3, 1.5);
          })
          .on("click", function(event, d) {
            handleDayClick(d, communityLineData.communityId);
          });
      });
    };

    const handleMouseOver = (element, event, d, communityId, tooltip, hoverRadius, hoverStrokeWidth) => {
      d3.select(element)
        .attr("r", hoverRadius)
        .attr("stroke-width", hoverStrokeWidth)
        .style("opacity", 1);

      tooltip
        .style("visibility", "visible")
        .html(formatTooltipContent(d, communityId))
        .style("left", (event.pageX + 10) + "px")
        .style("top", (event.pageY - 10) + "px");
    };

    const handleMouseMove = (event, tooltip) => {
      tooltip
        .style("left", (event.pageX + 10) + "px")
        .style("top", (event.pageY - 10) + "px");
    };

    const handleMouseOut = (element, tooltip, normalRadius, normalStrokeWidth) => {
      d3.select(element)
        .attr("r", normalRadius)
        .attr("stroke-width", normalStrokeWidth)
        .style("opacity", 0.9);

      tooltip.style("visibility", "hidden");
    };

    const handleDayClick = (dayData, communityId) => {
      if (onDayClick && dayData.hitCount > 0) {
        // For multi-line chart, we need to get all sessions for the day but filtered by community ID
        const originalDayData = data.find(d => 
          (d.singaporeDate || d.date) === dayData.singaporeDate || 
          (d.singaporeDate || d.date) === dayData.dateString
        );
        
        if (originalDayData) {
          // Filter sample sessions by the clicked community ID
          const filteredSampleSessions = (originalDayData.sampleSessions || []).filter(session => 
            session.communityId === communityId
          );
          
          // Use the original day data but with filtered sessions and community-specific info
          const serializedDayData = {
            ...originalDayData,
            date: originalDayData.singaporeDate || originalDayData.date,
            hitCount: dayData.hitCount, // Use the community-specific hit count
            sampleSessions: filteredSampleSessions,
            selectedCommunityId: communityId, // Add the selected community ID for reference
            hasMoreSessions: originalDayData.hasMoreSessions // Keep the original flag
          };
          
          console.log(`Sending to onDayClick:`, serializedDayData);
          onDayClick(serializedDayData);
        } else {
          // Fallback: create day data from the community point
          const serializedDayData = {
            date: dayData.singaporeDate || dayData.dateString,
            hitCount: dayData.hitCount,
            singaporeDate: dayData.singaporeDate || dayData.dateString,
            sampleSessions: [],
            selectedCommunityId: communityId,
            hasMoreSessions: true
          };
          onDayClick(serializedDayData);
        }
      }
    };

    
    drawChart(data);
    return cleanupTooltips;
  }, [data, onDayClick]);

  if (!data || !Array.isArray(data) || data.length === 0) {
    return (
      <div className="timeseries-container">
        <h3>Daily Snort Hits Visualization</h3>
        <p>No data available for visualization.</p>
      </div>
    );
  }

  return (
    <div className="timeseries-container">
      <h3>Daily Snort Hits by Community ID Over Time</h3>
      <p className="chart-instruction">
        Each colored line represents a different Community ID. Click on any data point to view detailed session information. 
        The legend shows which color corresponds to each Community ID.
      </p>
      <svg ref={svgRef}></svg>
    </div>
  );
};

export default TimeseriesChart;