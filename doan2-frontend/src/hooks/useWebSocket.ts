import { useEffect, useRef, useState } from 'react';
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
  const [isConnected, setIsConnected] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [currentUrl, setCurrentUrl] = useState<string>('');

  useEffect(() => {
    const token = localStorage.getItem('accessToken');
    if (!token || !user) {
      setIsConnected(false);
      setErrorMsg("Chưa đăng nhập hoặc thiếu token");
      return;
    }

    const apiUrl = import.meta.env.VITE_API_URL || '';
    let wsUrl = '';
    if (apiUrl.startsWith('http://') || apiUrl.startsWith('https://')) {
      const url = new URL(apiUrl);
      const wsProtocol = url.protocol === 'https:' ? 'wss:' : 'ws:';
      wsUrl = `${wsProtocol}//${url.host}/ws-plain`;
    } else {
      const wsProtocol = globalThis.location.protocol === 'https:' ? 'wss:' : 'ws:';
      wsUrl = `${wsProtocol}//${globalThis.location.host}/ws-plain`;
    }

    setCurrentUrl(wsUrl);
    setErrorMsg(null);

    const ws = new WebSocket(wsUrl);

    ws.onopen = () => {
      console.log('WebSocket connected');
      setIsConnected(true);
      setErrorMsg(null);
    };

    ws.onmessage = (event) => {
      try {
        const message: WebSocketMessage = JSON.parse(event.data);
        for (const [subscribedTopic, handlers] of messageHandlersRef.current.entries()) {
          if (message.topic === subscribedTopic || message.topic.startsWith(subscribedTopic + '/')) {
            for (const handler of handlers) {
              handler(message);
            }
          }
        }
      } catch (error) {
        console.error('Error parsing WebSocket message:', error);
      }
    };

    ws.onerror = (error) => {
      console.error('WebSocket error:', error);
      setIsConnected(false);
      setErrorMsg("Lỗi kết nối Socket (Kiểm tra CORS hoặc trạng thái Server)");
    };

    ws.onclose = (event) => {
      console.log('WebSocket disconnected');
      setIsConnected(false);
      if (!event.wasClean) {
        setErrorMsg(`Mất kết nối đột ngột (Mã lỗi: ${event.code})`);
      } else {
        setErrorMsg("Kết nối bị đóng bình thường");
      }
    };

    wsRef.current = ws;

    return () => {
      if (ws.readyState === WebSocket.OPEN) {
        ws.close();
      }
      setIsConnected(false);
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

  return { subscribe, send, isConnected, errorMsg, currentUrl };
};
