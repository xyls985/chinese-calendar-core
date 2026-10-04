export const MIN_YEAR: 1813;
export const MAX_YEAR: 2100;

export interface SolarDateTime {
  readonly year: number;
  readonly month: number;
  readonly day: number;
  readonly hour: number;
  readonly minute: number;
}
export interface LunarDate {
  readonly year: number;
  readonly month: number;
  readonly day: number;
  readonly isLeapMonth: boolean;
}
export interface FourPillars {
  readonly year: string;
  readonly month: string;
  readonly day: string;
  readonly hour: string;
}
export interface SolarTerm {
  readonly name: string;
  /** Approximate time, rounded to HH:MM in UTC+08:00. */
  readonly time: string;
}
export interface CalendarResult {
  readonly solar: SolarDateTime;
  readonly lunar: LunarDate;
  readonly pillars: FourPillars;
  readonly solarTerm: SolarTerm | null;
}

export function getLunarDate(year: number, month: number, day: number): LunarDate;
export function getCalendar(year: number, month: number, day: number, hour?: number, minute?: number): CalendarResult;
export function formatCalendar(year: number, month: number, day: number, hour?: number, minute?: number): string;

declare const calendar: {
  MIN_YEAR: typeof MIN_YEAR;
  MAX_YEAR: typeof MAX_YEAR;
  getCalendar: typeof getCalendar;
  getLunarDate: typeof getLunarDate;
  formatCalendar: typeof formatCalendar;
};
export default calendar;
