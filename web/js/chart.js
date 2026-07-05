async function renderRateChart(from, to) {
  const canvas = document.getElementById("rate-chart");
  const emptyEl = document.getElementById("chart-empty");
  const labelEl = document.getElementById("chart-pair-label");
  const ctx = canvas.getContext("2d");

  labelEl.textContent = `${from} → ${to}`;
  ctx.clearRect(0, 0, canvas.width, canvas.height);

  let series;
  try {
    const history = await getRatesHistory();
    series = computeHistorySeries(history, from, to);
  } catch {
    series = [];
  }

  if (series.length === 0) {
    emptyEl.hidden = false;
    return;
  }
  emptyEl.hidden = true;

  drawLineChart(ctx, canvas.width, canvas.height, series);
}

function drawLineChart(ctx, width, height, series) {
  const padding = { top: 20, right: 20, bottom: 30, left: 55 };
  const plotWidth = width - padding.left - padding.right;
  const plotHeight = height - padding.top - padding.bottom;

  const values = series.map((p) => p.rate);
  const min = Math.min(...values);
  const max = Math.max(...values);
  const range = max - min || 1;

  const styles = getComputedStyle(document.body);
  const textColor = styles.getPropertyValue("--text-muted").trim() || "#888";
  const lineColor = styles.getPropertyValue("--accent").trim() || "#2f6fed";
  const borderColor = styles.getPropertyValue("--border").trim() || "#ddd";

  ctx.strokeStyle = borderColor;
  ctx.lineWidth = 1;
  ctx.beginPath();
  ctx.moveTo(padding.left, padding.top);
  ctx.lineTo(padding.left, height - padding.bottom);
  ctx.lineTo(width - padding.right, height - padding.bottom);
  ctx.stroke();

  ctx.fillStyle = textColor;
  ctx.font = "11px sans-serif";
  ctx.textAlign = "right";
  ctx.fillText(formatNumber(max), padding.left - 8, padding.top + 4);
  ctx.fillText(formatNumber(min), padding.left - 8, height - padding.bottom);

  const stepX = series.length > 1 ? plotWidth / (series.length - 1) : 0;
  const points = series.map((p, i) => ({
    x: padding.left + stepX * i,
    y: padding.top + plotHeight - ((p.rate - min) / range) * plotHeight,
  }));

  ctx.strokeStyle = lineColor;
  ctx.lineWidth = 2;
  ctx.beginPath();
  points.forEach((pt, i) => {
    if (i === 0) ctx.moveTo(pt.x, pt.y);
    else ctx.lineTo(pt.x, pt.y);
  });
  ctx.stroke();

  ctx.fillStyle = lineColor;
  points.forEach((pt) => {
    ctx.beginPath();
    ctx.arc(pt.x, pt.y, 3, 0, Math.PI * 2);
    ctx.fill();
  });

  ctx.fillStyle = textColor;
  ctx.textAlign = "center";
  series.forEach((p, i) => {
    const label = p.date.slice(5);
    ctx.fillText(label, points[i].x, height - padding.bottom + 15);
  });
}

function onConverterPairChanged(from, to) {
  renderRateChart(from, to);
}
