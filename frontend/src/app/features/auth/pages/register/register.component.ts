import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Stub - Member 1 hoàn thiện sau.
 */
@Component({
  selector: 'app-register',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterLink],
  template: `
    <div class="text-center">
      <h2 class="text-xl font-semibold mb-4">Đăng ký tài khoản</h2>
      <p class="text-gray-500">Module sẽ được Member 1 hoàn thiện.</p>
      <a routerLink="/auth/login" class="text-primary mt-4 inline-block">
        Quay lại đăng nhập
      </a>
    </div>
  `,
})
export class RegisterComponent {}
