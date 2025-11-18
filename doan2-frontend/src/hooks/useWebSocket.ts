import { useEffect, useRef } from 'react';
import { useAuthStore } from '../store/authStore';

interface WebSocketMessage {
  topic: string;
  message: any;
  timestamp: string;
}

export const useWebSocket = () => {
  const wsRef = useRef<WebSocket | null>(null);
  const messageHandlersRef = useRef<Map<string, ((msg: WebSocketMessage) => void)[]>>(new Map());
  const user = useAuthStore((state) => state.user);

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || !user) return;

    const wsProtocol = globalThis.location.protocol === 'https:' ? 'wss:' : 'ws:';
    const wsUrl = `${wsProtocol}//${globalThis.location.host}/ws`;

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('WebSocket connected');
      ws.send(
        JSON.stringify({
          type: 'CONNECT',
          token: token,
          username: user.username,
        })
      );
    };

    ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        const handlers = messageHandlersRef.current.get(message.topic) || [];
        for (const handler of handlers) {
          handler(message);
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
    };

    ws.onclose = () => {
      console.log('WebSocket disconnected');
    };

    wsRef.current = ws;

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
    };
  }, [user]);

  const subscribe = (topic: string, handler: (msg: WebSocketMessage) => void) => {
    const handlers = messageHandlersRef.current.get(topic) || [];
    handlers.push(handler);
    messageHandlersRef.current.set(topic, handlers);

    return () => {
      const index = handlers.indexOf(handler);
      if (index > -1) {
        handlers.splice(index, 1);
      }
    };
  };

  const send = (destination: string, message: any) => {
    if (wsRef.current?.readyState === WebSocket.OPEN) {
      wsRef.current.send(
        JSON.stringify({
          destination,
          payload: message,
        })
      );
    }
  };

  return { subscribe, send, isConnected: wsRef.current?.readyState === WebSocket.OPEN };
};
