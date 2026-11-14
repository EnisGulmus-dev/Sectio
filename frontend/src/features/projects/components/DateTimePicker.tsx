type Props = { value: string; onChange: (value: string) => void };

function localValue(date: Date) {
  const offset = date.getTimezoneOffset() * 60_000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function DateTimePicker({ value, onChange }: Props) {
  const [date = "", time = ""] = value.split("T");

  function update(nextDate: string, nextTime: string) {
    onChange(nextDate ? `${nextDate}T${nextTime || "17:00"}` : "");
  }

  function quickDate(days: number) {
    const next = new Date();
    next.setDate(next.getDate() + days);
    next.setHours(17, 0, 0, 0);
    onChange(localValue(next));
  }

  return <fieldset className="date-time-picker">
    <legend>Son tarih</legend>
    <div className="date-time-fields">
      <label><span>Tarih</span><input type="date" value={date} onChange={(event) => update(event.target.value, time)} /></label>
      <label><span>Saat</span><input type="time" step="900" value={time} disabled={!date} onChange={(event) => update(date, event.target.value)} /></label>
    </div>
    <div className="quick-date-actions" aria-label="Hızlı tarih seçimi">
      <button type="button" onClick={() => quickDate(0)}>Bugün</button>
      <button type="button" onClick={() => quickDate(1)}>Yarın</button>
      <button type="button" onClick={() => quickDate(7)}>+1 hafta</button>
      {value && <button type="button" onClick={() => onChange("")}>Temizle</button>}
    </div>
  </fieldset>;
}
