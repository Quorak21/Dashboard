export class NotificationService {
    private notificationSound: HTMLAudioElement;

    constructor() {
        this.notificationSound = new Audio();
        this.notificationSound.src = 'assets/sounds/notif.mp3';
    }

    playNotification() {
        this.notificationSound.currentTime = 0;
        this.notificationSound.play();
    }
}