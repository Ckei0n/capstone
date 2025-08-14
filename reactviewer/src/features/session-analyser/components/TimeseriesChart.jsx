import React, { useEffect, useRef } from 'react';
import * as d3 from 'd3';
import './TimeseriesChart.css';

import { CHART_CONFIG } from '../util/chartConfig';
import { processDailyData } from '../util/dataProcessor';
import { ChartBuilder } from '../util/chartBuilder';
import { InteractionHandler } from '../util/interactionHandler';
import { cleanupTooltips } from '../util/tooltipUtils';

const TimeseriesChart = ({ data, onDayClick }) => {
  const svgRef = useRef();

  useEffect(() => {
    if (!data || !Array.isArray(data) || data.length === 0) return;

    const drawChart = (data) => {
      // Clear previous chart
      const svg = d3.select(svgRef.current);
      svg.selectAll("*").remove();

      // Setup chart dimensions
      const { margin, width, height } = CHART_CONFIG;
      const chartWidth = width - margin.left - margin.right;
      const chartHeight = height - margin.top - margin.bottom;

      // Create main chart group
      const g = svg
        .attr("width", width)
        .attr("height", height)
        .append("g")
        .attr("transform", `translate(${margin.left},${margin.top})`);

      // Process data
      const { communityData } = processDailyData(data);
      
      // Handle empty data
      if (communityData.length === 0) {
        showNoDataMessage(g, chartWidth, chartHeight);
        return;
      }

      // Build chart components
      const chartBuilder = new ChartBuilder(g, chartWidth, chartHeight, margin);
      const scales = chartBuilder.createScales(communityData);
      chartBuilder.createAxes(scales, communityData);
      chartBuilder.createLegend(communityData);
      
      // Add interaction handling
      const interactionHandler = new InteractionHandler(onDayClick, data);
      chartBuilder.drawLines(communityData, scales, interactionHandler);
    };

    const showNoDataMessage = (g, chartWidth, chartHeight) => {
      g.append("text")
        .attr("x", chartWidth / 2)
        .attr("y", chartHeight / 2)
        .attr("text-anchor", "middle")
        .style("font-size", "16px")
        .style("fill", "#666")
        .text("No data found in the selected date range");
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
        Each colored line represents a different Community ID. Click on any data point 
        to view detailed session information. The legend shows which color corresponds 
        to each Community ID.
      </p>
      <svg ref={svgRef}></svg>
    </div>
  );
};

export default TimeseriesChart;