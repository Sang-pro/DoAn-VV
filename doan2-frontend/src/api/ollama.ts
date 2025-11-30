import { useAuthStore } from '../store/authStore';

const API_URL = import.meta.env.VITE_API_URL || 'http://localhost:8080';

export interface OllamaMessage {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

export interface OllamaChatRequest {
  model: string;
  messages: OllamaMessage[];
  options?: Record<string, unknown>;
  stream?: boolean;
}

export const streamOllamaChat = async (
  request: OllamaChatRequest,
  onStream: (chunk: string, done: boolean) => void,
  onError: (error: Error) => void
) => {
  const { accessToken } = useAuthStore.getState();

  try {
    const response = await fetch(`${API_URL}/ollama/chat/stream`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        ...request,
        stream: true, // Ensure streaming is enabled
      }),
    });

    if (!response.ok) {
      const errorText = await response.text();
      throw new Error(
        `Failed to start stream: ${response.status} ${response.statusText} - ${errorText}`
      );
    }

    const reader = response.body?.getReader();
    if (!reader) {
      throw new Error('Could not get reader from response body');
    }

    const decoder = new TextDecoder();

    const processStream = async () => {
      while (true) {
        const { done, value } = await reader.read();
        if (done) {
          onStream('', true);
          break;
        }

        const chunk = decoder.decode(value, { stream: true });
        // SSE sends data in "data: {json}\n\n" format. We need to parse it.
        const lines = chunk
          .split('\n')
          .filter((line) => line.trim().startsWith('data:'));

        for (const line of lines) {
          const jsonStr = line.replace('data:', '').trim();
          if (jsonStr === '[DONE]') {
            onStream('', true);
            return;
          }
          try {
            const parsed = JSON.parse(jsonStr);
            if (parsed.done === true) {
              onStream('', true);
              return;
            }
            if(parsed.message && parsed.message.content) {
                onStream(parsed.message.content, false);
            }
          } catch (e) {
            // Might receive partial JSON, just continue accumulating
            // console.error('Failed to parse stream chunk:', jsonStr, e);
          }
        }
      }
    };

    processStream().catch(onError);
  } catch (error) {
    onError(error as Error);
  }
};
