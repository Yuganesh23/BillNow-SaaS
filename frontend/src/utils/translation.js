export function resolveMessage(catalog, locale, key) {
  return catalog[locale]?.[key] || catalog.en?.[key] || key;
}
