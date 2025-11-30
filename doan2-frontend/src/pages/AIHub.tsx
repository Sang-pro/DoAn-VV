import React, { useState, useRef, useEffect } from 'react';
import { streamOllamaChat, OllamaMessage } from '../api/ollama';

const AIHub: React.FC = () => {
  const [messages, setMessages] = useState<OllamaMessage[]>([]);
  const [input, setInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages, isLoading]);

  const handleSend = async () => {
    if (input.trim() === '' || isLoading) return;

    const userMessage: OllamaMessage = { role: 'user', content: input };
    setMessages((prev) => [...prev, userMessage]);
    setInput('');
    setIsLoading(true);

    const fullHistory = [...messages, userMessage];

    // Add a placeholder for the assistant's response
    setMessages((prev) => [...prev, { role: 'assistant', content: '' }]);

    try {
      await streamOllamaChat(
        {
          model: 'llama2', // Or make this selectable
          messages: fullHistory,
        },
        (chunk, done) => {
          if (!done) {
            setMessages((prev) => {
              const lastMessage = prev[prev.length - 1];
              if (lastMessage && lastMessage.role === 'assistant') {
                // Append the chunk to the last message content
                return [
                  ...prev.slice(0, -1),
                  { ...lastMessage, content: lastMessage.content + chunk },
                ];
              }
              return prev;
            });
          } else {
            setIsLoading(false);
          }
        },
        (error) => {
          console.error('Streaming Error:', error);
          setMessages((prev) => [
            ...prev,
            {
              role: 'assistant',
              content: `Error: ${error.message}`,
            },
          ]);
          setIsLoading(false);
        }
      );
    } catch (error) {
      console.error('Failed to send message:', error);
       setMessages((prev) => [
        ...prev,
        { role: 'assistant', content: 'An unexpected error occurred.' },
      ]);
      setIsLoading(false);
    }
  };

  return (
    <div className="flex flex-col h-[calc(100vh-4rem)] bg-gray-900 text-white">
      <header className="bg-gray-800 p-4 shadow-md">
        <h1 className="text-xl font-bold">AI Analytics Hub</h1>
      </header>
      <main className="flex-1 overflow-y-auto p-4">
        <div className="space-y-4">
          {messages.map((msg, index) => (
            <div
              key={index}
              className={`flex items-start gap-3 ${
                msg.role === 'user' ? 'justify-end' : ''
              }`}
            >
              {msg.role === 'assistant' && (
                <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center font-bold">A</div>
              )}
              <div
                className={`max-w-xl p-3 rounded-lg ${
                  msg.role === 'user'
                    ? 'bg-blue-600'
                    : 'bg-gray-700'
                }`}
              >
                <p style={{ whiteSpace: 'pre-wrap' }}>{msg.content}</p>
              </div>
               {msg.role === 'user' && (
                <div className="w-8 h-8 rounded-full bg-gray-600 flex items-center justify-center font-bold">Y</div>
              )}
            </div>
          ))}
          {isLoading && messages[messages.length-1]?.role !== 'assistant' && (
            <div className="flex items-start gap-3">
              <div className="w-8 h-8 rounded-full bg-blue-500 flex items-center justify-center font-bold">A</div>
              <div className="max-w-xl p-3 rounded-lg bg-gray-700">
                <div className="dot-flashing"></div>
              </div>
            </div>
          )}
          <div ref={messagesEndRef} />
        </div>
      </main>
      <footer className="p-4 bg-gray-800">
        <div className="flex items-center gap-2">
          <input
            type="text"
            value={input}
            onChange={(e) => setInput(e.target.value)}
            onKeyPress={(e) => e.key === 'Enter' && handleSend()}
            placeholder="Ask about your data..."
            className="flex-1 p-2 rounded-lg bg-gray-700 border border-gray-600 focus:outline-none focus:ring-2 focus:ring-blue-500"
            disabled={isLoading}
          />
          <button
            onClick={handleSend}
            disabled={isLoading}
            className="px-4 py-2 rounded-lg bg-blue-600 hover:bg-blue-700 disabled:bg-gray-500"
          >
            {isLoading ? 'Sending...' : 'Send'}
          </button>
        </div>
      </footer>
       <style>{`
        .dot-flashing {
          position: relative;
          width: 10px;
          height: 10px;
          border-radius: 5px;
          background-color: #9880ff;
          color: #9880ff;
          animation: dotFlashing 1s infinite linear alternate;
          animation-delay: .5s;
        }
        .dot-flashing::before, .dot-flashing::after {
          content: '';
          display: inline-block;
          position: absolute;
          top: 0;
        }
        .dot-flashing::before {
          left: -15px;
          width: 10px;
          height: 10px;
          border-radius: 5px;
          background-color: #9880ff;
          color: #9880ff;
          animation: dotFlashing 1s infinite alternate;
          animation-delay: 0s;
        }
        .dot-flashing::after {
          left: 15px;
          width: 10px;
          height: 10px;
          border-radius: 5px;
          background-color: #9880ff;
          color: #9880ff;
          animation: dotFlashing 1s infinite alternate;
          animation-delay: 1s;
        }

        @keyframes dotFlashing {
          0% {
            background-color: #9880ff;
          }
          50%, 100% {
            background-color: #1a1a1a;
          }
        }
      `}</style>
    </div>
  );
};

export default AIHub;
