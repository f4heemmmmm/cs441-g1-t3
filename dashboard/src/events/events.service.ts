import { Injectable, OnModuleInit, OnModuleDestroy, Logger } from '@nestjs/common';
import { Subject, Observable } from 'rxjs';
import EventSource = require('eventsource');

@Injectable()
export class EventsService implements OnModuleInit, OnModuleDestroy {
  private readonly logger = new Logger(EventsService.name);
  private eventSubject = new Subject<MessageEvent>();
  private es: EventSource | null = null;
  private reconnectTimer: ReturnType<typeof setTimeout> | null = null;

  onModuleInit() {
    this.connect();
  }

  onModuleDestroy() {
    if (this.reconnectTimer) clearTimeout(this.reconnectTimer);
    this.es?.close();
  }

  private connect() {
    this.logger.log('Connecting to Java SSE backend at http://localhost:8080/events');
    this.es = new EventSource('http://localhost:8080/events');

    this.es.onopen = () => {
      this.logger.log('Connected to Java SSE backend');
    };

    this.es.onmessage = (e: any) => {
      this.eventSubject.next({ data: e.data } as MessageEvent);
    };

    this.es.onerror = () => {
      this.logger.warn('SSE connection lost, reconnecting in 3s...');
      this.es?.close();
      this.reconnectTimer = setTimeout(() => this.connect(), 3000);
    };
  }

  getEvents(): Observable<MessageEvent> {
    return this.eventSubject.asObservable();
  }
}
