import { Injectable, signal } from "@angular/core";

@Injectable({ providedIn: 'root' })
export class ToastService {

    public message = signal<string | null>(null);
    private timer: ReturnType<typeof setTimeout> = 0;
  
  showError(message : string) {
    this.message.set(message);
    clearTimeout(this.timer);
    this.timer = setTimeout(() => {
        this.message.set(null);
    }, 6000);
  }
}