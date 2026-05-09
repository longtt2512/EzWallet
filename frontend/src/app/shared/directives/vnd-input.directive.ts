import { Directive, ElementRef, forwardRef, inject } from '@angular/core';
import {
  ControlValueAccessor,
  NG_VALUE_ACCESSOR,
} from '@angular/forms';

@Directive({
  selector: 'input[vndInput]',
  standalone: true,
  providers: [
    {
      provide: NG_VALUE_ACCESSOR,
      useExisting: forwardRef(() => VndInputDirective),
      multi: true,
    },
  ],
  host: {
    '(input)': 'onInput($event)',
    '(blur)': 'onTouched()',
    'inputmode': 'numeric',
    'autocomplete': 'off',
  },
})
export class VndInputDirective implements ControlValueAccessor {
  private el = inject(ElementRef<HTMLInputElement>);

  private _onChange: (v: number | null) => void = () => {};
  onTouched: () => void = () => {};

  writeValue(value: number | null): void {
    this.el.nativeElement.value = value != null ? this.fmt(value) : '';
  }

  registerOnChange(fn: (v: number | null) => void): void {
    this._onChange = fn;
  }

  registerOnTouched(fn: () => void): void {
    this.onTouched = fn;
  }

  setDisabledState(isDisabled: boolean): void {
    this.el.nativeElement.disabled = isDisabled;
  }

  onInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const digits = input.value.replace(/[^0-9]/g, '');
    const num = digits ? parseInt(digits, 10) : null;
    input.value = num != null ? this.fmt(num) : '';
    const end = input.value.length;
    input.setSelectionRange(end, end);
    this._onChange(num);
  }

  private fmt(n: number): string {
    return new Intl.NumberFormat('vi-VN').format(n);
  }
}
