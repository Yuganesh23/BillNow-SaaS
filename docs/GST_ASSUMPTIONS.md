# GST assumptions for CA review

This document records calculation decisions; it is not tax advice. A practising CA must approve these rules before production filing.

## Reporting additions

- GSTR-style reports are reconciliation aids and are not described as filing-ready.
- B2C-large classification uses the configurable `REPORTS_GST_B2CL_THRESHOLD`; its current legal value and applicability require CA review.
- Existing document notes store a gross amount rather than itemised tax lines, so report reductions are apportioned using the original invoice's taxable and tax ratios.
- Historical purchases stored only aggregate tax. V4 splits it as CGST/SGST for same-state suppliers or IGST for other-state suppliers; historical cess cannot be reconstructed and remains zero.

- GSTIN validation checks the official 15-character structure, state/UT prefix, embedded PAN shape and base-36 check digit. State code `97` is retained as “Other Territory”.
- Place of supply currently uses the buyer state code (derived from a valid GSTIN, otherwise selected manually). A missing unregistered-customer state falls back to the seller state.
- Equal seller and place-of-supply codes produce CGST and SGST in equal halves. Different codes produce IGST.
- Composition and unregistered sellers produce a Bill of Supply with zero output tax. Eligibility and composition turnover thresholds are deliberately not encoded.
- Tax is calculated and rounded per line to two decimals using `HALF_UP`. Line discount is removed first. The invoice discount is then allocated proportionally across taxable line values; the final line receives the residual so the allocation is exact.
- Inclusive price extraction divides the discounted gross line value by `100 + GST rate + percentage cess rate`. Exclusive pricing applies GST and percentage cess to the discounted taxable value.
- The final payable amount is rounded to the nearest rupee using `HALF_UP`; the difference is stored and printed as a separate round-off line.
- HSN/SAC length is configurable per shop as 4, 6 or 8 digits. BillNow does not infer a required length from turnover.
- Invoice numbers use a per-shop, per-financial-year (April–March) transactional counter and are limited to letters, digits, `/` and `-`, up to 16 characters.
- Export, SEZ, deemed-export, reverse-charge tax liability, fixed-amount cess and special place-of-supply rules are not automated yet. Their flags must remain informational until a CA-approved rule set is implemented.
- Historical invoices retain their original displayed number. Migration V2 only backfills their financial year; new documents use the FY series.
- Legacy shops with no valid GSTIN/state code are backfilled as state code `97` (Other Territory) to keep the additive migration deployable. Owners must select their actual state before issuing new tax documents.

References to verify before launch: GST portal state/UT code list, GST invoice rules, and the business’s CA-approved treatment for composition, unregistered purchases, cess, exports and SEZ supplies.
