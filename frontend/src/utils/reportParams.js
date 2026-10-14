export const compactReportParams = (values = {}) => Object.fromEntries(
  Object.entries(values).filter(([, value]) => value !== '' && value !== undefined && value !== null),
);
