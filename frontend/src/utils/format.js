// 바이트 값을 사람이 읽기 좋은 단위(KB/MB/GB)로 변환한다.
export function formatBytes(bytes) {
  const value = Number(bytes) || 0;
  if (value <= 0) return "0 B";
  const units = ["B", "KB", "MB", "GB", "TB"];
  const exponent = Math.min(Math.floor(Math.log(value) / Math.log(1024)), units.length - 1);
  const scaled = value / 1024 ** exponent;
  return `${scaled >= 100 ? Math.round(scaled) : Math.round(scaled * 10) / 10} ${units[exponent]}`;
}

// 요청 수처럼 큰 정수를 1.2천/3.4만 형태로 축약한다.
export function formatCount(count) {
  const value = Number(count) || 0;
  if (value < 1000) return String(Math.round(value));
  if (value < 10000) return `${Math.round(value / 100) / 10}천`;
  return `${Math.round(value / 1000)}천`;
}
