import { AbstractControl, ValidationErrors } from '@angular/forms';

export const VND_MIN_TOPUP    = 10_000;
export const VND_MIN_WITHDRAW = 50_000;
export const VND_MIN_TRANSFER = 10_000;

/**
 * Rejects values that are not multiples of 1,000 ₫.
 * VND's smallest practical denomination in banking is 1,000 ₫.
 */
export function vndMultiple(control: AbstractControl): ValidationErrors | null {
  const v: number | null = control.value;
  if (v == null) return null;
  return v % 1_000 === 0 ? null : { vndMultiple: true };
}
