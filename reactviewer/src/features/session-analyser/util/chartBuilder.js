// Builds chart components (scales, axes, legends, lines)

import * as d3 from 'd3';

export class ChartBuilder {
  constructor(g, chartWidth, chartHeight, margin) {
    this.g = g;
    this.chartWidth = chartWidth;
    this.chartHeight = chartHeight;
    this.margin = margin;
  }

  createScales(communityData) {
    const { allDates, allHitCounts } = this.extractScaleData(communityData);
    
    const xScale = d3.scaleTime()
      .domain(d3.extent(allDates))
      .range([0, this.chartWidth]);

    const maxHits = d3.max(allHitCounts) || 0;
    const yScale = d3.scaleLinear()
      .domain([0, maxHits * 1.1])
      .range([this.chartHeight, 0]);

    return { xScale, yScale };
  }

  extractScaleData(communityData) {
    const allDates = [];
    const allHitCounts = [];
    
    communityData.forEach(community => {
      community.dataPoints.forEach(point => {
        allDates.push(point.date);
        allHitCounts.push(point.hitCount);
      });
    });
    
    return { allDates, allHitCounts };
  }

  createAxes(scales, communityData) {
    const { xScale, yScale } = scales;
    
    const xAxis = this.createXAxis(xScale, communityData);
    const yAxis = this.createYAxis(yScale);
    
    this.renderXAxis(xAxis);
    this.renderYAxis(yAxis);
    this.addAxisLabels();
  }

  createXAxis(xScale, communityData) {
    const uniqueDates = this.getUniqueDates(communityData);
    const dataPointCount = uniqueDates.length;
    
    const xAxis = d3.axisBottom(xScale)
      .tickFormat(d3.timeFormat("%Y-%m-%d"));
      
    if (dataPointCount <= 10) {
      xAxis.tickValues(uniqueDates);
    } else if (dataPointCount <= 30) {
      xAxis.ticks(d3.timeDay.every(Math.ceil(dataPointCount / 10)));
    } else {
      xAxis.ticks(d3.timeDay.every(1));
    }
    
    return xAxis;
  }

  createYAxis(yScale) {
    return d3.axisLeft(yScale)
      .tickFormat(d3.format("d"))
      .ticks(10);
  }

  getUniqueDates(communityData) {
    const allDates = [];
    communityData.forEach(community => {
      community.dataPoints.forEach(point => {
        allDates.push(point.date);
      });
    });
    
    return [...new Set(allDates.map(d => d.getTime()))].map(t => new Date(t));
  }

  renderXAxis(xAxis) {
    this.g.append("g")
      .attr("class", "x-axis")
      .attr("transform", `translate(0,${this.chartHeight})`)
      .call(xAxis)
      .selectAll("text")
      .style("text-anchor", "end")
      .attr("dx", "-.8em")
      .attr("dy", ".15em")
      .attr("transform", "rotate(-45)");
  }

  renderYAxis(yAxis) {
    this.g.append("g")
      .attr("class", "y-axis")
      .call(yAxis);
  }

  addAxisLabels() {
    // Y-axis label
    this.g.append("text")
      .attr("transform", "rotate(-90)")
      .attr("y", 0 - this.margin.left)
      .attr("x", 0 - (this.chartHeight / 2))
      .attr("dy", "1em")
      .style("text-anchor", "middle")
      .style("font-size", "14px")
      .style("font-weight", "bold")
      .text("Daily Snort Hit Count");

    // X-axis label
    this.g.append("text")
      .attr("transform", `translate(${this.chartWidth / 2}, ${this.chartHeight + this.margin.bottom - 10})`)
      .style("text-anchor", "middle")
      .style("font-size", "14px")
      .style("font-weight", "bold")
      .text("Date");
  }

  createLegend(communityData) {
    const legend = this.g.append("g")
      .attr("class", "legend")
      .attr("transform", `translate(${this.chartWidth + 30}, 20)`);
    
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
    
    legendItems.append("text")
      .attr("x", 16)
      .attr("y", 6)
      .attr("dy", "0.35em")
      .style("font-size", "11px")
      .style("font-family", "Arial, sans-serif")
      .text(d => this.formatCommunityId(d.communityId));
    
    return legend;
  }

  formatCommunityId(communityId) {
    if (communityId.length > 20) {
      return communityId.substring(0, 8) + '...' + communityId.substring(communityId.length - 4);
    }
    return communityId;
  }

  drawLines(communityData, scales, interactionHandler) {
    const { xScale, yScale } = scales;
    
    const line = d3.line()
      .x(d => xScale(d.date))
      .y(d => yScale(d.hitCount))
      .curve(d3.curveCardinal.tension(0.5))
      .defined(d => d.hitCount !== null);

    const communityLines = this.g.selectAll(".community-line")
      .data(communityData)
      .enter()
      .append("g")
      .attr("class", "community-line");

    this.addLinePaths(communityLines, line);
    this.addDataPoints(communityLines, xScale, yScale, interactionHandler);
  }

  addLinePaths(communityLines, line) {
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
  }

  addDataPoints(communityLines, xScale, yScale, interactionHandler) {
    communityLines.each(function(communityLineData) {
      const communityGroup = d3.select(this);
      
      communityGroup.selectAll(".data-point")
        .data(communityLineData.dataPoints.filter(d => d.hitCount > 0))
        .enter()
        .append("circle")
        .attr("class", "data-point")
        .attr("cx", d => xScale(d.date))
        .attr("cy", d => yScale(d.hitCount))
        .attr("r", 3)
        .attr("fill", communityLineData.color)
        .attr("stroke", "white")
        .attr("stroke-width", 1.5)
        .style("cursor", "pointer")
        .style("opacity", 0.9)
        .on("mouseover", function(event, d) {
          interactionHandler.handleMouseOver(this, event, d, communityLineData.communityId);
        })
        .on("mousemove", function(event) {
          interactionHandler.handleMouseMove(event);
        })
        .on("mouseout", function() {
          interactionHandler.handleMouseOut(this);
        })
        .on("click", function(event, d) {
          interactionHandler.handleDayClick(d, communityLineData.communityId);
        });
    });
  }
}