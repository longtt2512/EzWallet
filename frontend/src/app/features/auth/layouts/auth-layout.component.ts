import { ChangeDetectionStrategy, Component } from '@angular/core';
import { RouterOutlet } from '@angular/router';

@Component({
  selector: 'app-auth-layout',
  standalone: true,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [RouterOutlet],
  template: `
    <div class="min-h-screen flex items-center justify-center bg-gray-100">
      <div class="w-full max-w-md p-8 bg-white rounded-lg shadow-md">
        <h1 class="text-2xl font-bold text-center mb-6 text-primary">
          EzWallet
        </h1>
        <router-outlet />
      </div>
    </div>
  `,
})
export class AuthLayoutComponent {}
