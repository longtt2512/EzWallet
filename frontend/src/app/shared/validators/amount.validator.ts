import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export function amountValidator(min = 1000, max = 500_000_000): ValidatorFn {
  return (control: AbstractControl): ValidationErrors | null => {
    const value = Number(control.value);
    if (isNaN(value) || value < min) return { minAmount: { min, actual: value } };
    if (value > max) return { maxAmount: { max, actual: value } };
    return null;
  };
}
