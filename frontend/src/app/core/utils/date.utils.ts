export function toLocalIso(d: Date): string {
  return [
    d.getFullYear(),
    String(d.getMonth() + 1).padStart(2, '0'),
    String(d.getDate()).padStart(2, '0'),
  ].join('-');
}

export function yearAgo(): string {
  const d = new Date();
  d.setFullYear(d.getFullYear() - 1);
  return toLocalIso(d);
}

export function today(): string {
  return toLocalIso(new Date());
}
