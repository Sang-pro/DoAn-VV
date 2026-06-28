import React, { useState, useRef, useEffect } from 'react';
import { motion, AnimatePresence } from 'framer-motion';
import { MessageCircle, X, Send, Bot, User, Trash2 } from 'lucide-react';
import { useAuthStore } from '../store/authStore';
import apiClient from '../api/client';

interface Message {
  id: string;
  text: string;
  sender: 'user' | 'bot';
  timestamp: Date;
}

const AIChatbot: React.FC = () => {
  const { isAuthenticated } = useAuthStore();
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState<Message[]>(() => {
    const saved = localStorage.getItem('ai_chat_history');
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        return parsed.map((m: any) => ({
          ...m,
          timestamp: new Date(m.timestamp)
        }));
      } catch (e) {
        console.error('Failed to parse saved chat history:', e);
      }
    }
    return [
      {
        id: '1',
        text: 'Xin chào! Tôi là trợ lý AI của dự án. Tôi có thể giúp gì cho bạn?',
        sender: 'bot',
        timestamp: new Date()
      }
    ];
  });
  const [inputValue, setInputValue] = useState('');
  const [isTyping, setIsTyping] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Save messages to localStorage whenever they change
  useEffect(() => {
    localStorage.setItem('ai_chat_history', JSON.stringify(messages));
  }, [messages]);

  useEffect(() => {
    scrollToBottom();
  }, [messages, isTyping]);

  const handleSendMessage = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!inputValue.trim()) return;

    const newUserMessage: Message = {
      id: Date.now().toString() + '-user',
      text: inputValue,
      sender: 'user',
      timestamp: new Date()
    };

    setMessages((prev) => [...prev, newUserMessage]);
    setInputValue('');
    setIsTyping(true);

    try {
      // Format history array specifically for Ollama: { role: 'user' | 'assistant', content: string }
      const messageHistory = messages.map(msg => ({
        role: msg.sender === 'user' ? 'user' : 'assistant',
        content: msg.text
      }));
      // add the current new message
      messageHistory.push({ role: 'user', content: inputValue });

      // Add a placeholder bot message for real-time streaming
      const botMsgId = Date.now().toString() + '-bot';
      setMessages((prev) => [...prev, {
        id: botMsgId,
        text: '',
        sender: 'bot',
        timestamp: new Date()
      }]);

      const token = localStorage.getItem('accessToken');
      const baseURL = apiClient.defaults.baseURL || 'http://localhost:8080/api';
      
      const response = await fetch(`${baseURL}/ollama/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { 'Authorization': `Bearer ${token}` } : {})
        },
        body: JSON.stringify({
          model: 'deepseek-r1:8b', // User requested deepseek
          messages: messageHistory,
          streaming: true
        })
      });

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`);
      }

      if (!response.body) {
         throw new Error("No response body");
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let isDone = false;
      let fullText = '';
      let buffer = '';

      while (!isDone) {
        const { value, done } = await reader.read();
        if (done) break;
        
        buffer += decoder.decode(value, { stream: true });
        const lines = buffer.split('\n');
        buffer = lines.pop() || ''; // Keep the incomplete line in the buffer
        
        for (const line of lines) {
          if (line.trim().startsWith('data:')) {
            const dataStr = line.replace('data:', '').trim();
            if (!dataStr || dataStr === '[DONE]') continue;
            
            try {
              const data = JSON.parse(dataStr);
              if (data.error) throw new Error(data.error);
              if (data.message && data.message.content) {
                fullText += data.message.content;
                // Update the bot message in real-time
                setMessages((prev) => prev.map(m => m.id === botMsgId ? { ...m, text: fullText } : m));
              }
            } catch (e: any) {
              if (e.message && line.includes('error')) {
                 fullText += '\n\n**Lỗi:** ' + e.message;
                 setMessages((prev) => prev.map(m => m.id === botMsgId ? { ...m, text: fullText } : m));
              }
            }
          }
        }
      }
    } catch (error: any) {
      console.error('Error calling AI API:', error);
      const errorMsg: Message = {
        id: Date.now().toString() + '-error',
        text: 'Xin lỗi, không thể kết nối đến Trợ lý AI (Ollama). Vui lòng kiểm tra lại dịch vụ Backend.',
        sender: 'bot',
        timestamp: new Date()
      };
      // remove empty partial message if it failed immediately
      setMessages((prev) => {
        const filtered = prev.filter(m => m.text !== '');
        return [...filtered, errorMsg];
      });
    } finally {
      setIsTyping(false);
    }
  };

  const clearChat = () => {
    setMessages([
      {
        id: '1',
        text: 'Đã xóa hội thoại. Tôi có thể giúp gì thêm cho bạn?',
        sender: 'bot',
        timestamp: new Date()
      }
    ]);
  };

  if (!isAuthenticated) return null;

  return (
    <>
      {/* Floating Action Button */}
      <motion.button
        className="fixed bottom-6 right-6 p-4 rounded-full bg-gradient-to-r from-indigo-500 to-purple-600 text-white shadow-xl hover:shadow-2xl z-50 focus:outline-none"
        whileHover={{ scale: 1.1 }}
        whileTap={{ scale: 0.9 }}
        onClick={() => setIsOpen(true)}
      >
        <MessageCircle size={28} />
      </motion.button>

      {/* Chat Window Overlay */}
      <AnimatePresence>
        {isOpen && (
          <motion.div
            initial={{ opacity: 0, y: 50, scale: 0.9 }}
            animate={{ opacity: 1, y: 0, scale: 1 }}
            exit={{ opacity: 0, y: 50, scale: 0.9 }}
            transition={{ type: 'spring', damping: 25, stiffness: 200 }}
            className="fixed bottom-24 right-6 w-[350px] sm:w-[400px] h-[600px] max-h-[80vh] bg-white rounded-2xl shadow-2xl flex flex-col overflow-hidden z-50 border border-gray-100"
          >
            {/* Header */}
            <div className="bg-gradient-to-r from-indigo-600 to-purple-600 p-4 text-white flex justify-between items-center z-10 shadow-md">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-white/20 rounded-full flex items-center justify-center backdrop-blur-sm">
                  <Bot size={24} className="text-white" />
                </div>
                <div>
                  <h3 className="font-bold text-lg">Trợ lý AI</h3>
                  <p className="text-xs text-indigo-100 flex items-center gap-1">
                    <span className="w-2 h-2 rounded-full bg-green-400 inline-block animate-pulse"></span>
                    Đang hoạt động (Ollama)
                  </p>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button 
                  onClick={clearChat} 
                  className="p-2 hover:bg-white/20 rounded-full transition-colors"
                  title="Xóa lịch sử chat"
                >
                  <Trash2 size={18} />
                </button>
                <button 
                  onClick={() => setIsOpen(false)} 
                  className="p-2 hover:bg-white/20 rounded-full transition-colors"
                >
                  <X size={20} />
                </button>
              </div>
            </div>

            {/* Chat Area */}
            <div className="flex-1 p-4 overflow-y-auto bg-gray-50 flex flex-col gap-4 scroll-smooth">
              {messages.map((msg) => (
                <div
                  key={msg.id}
                  className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'} w-full`}
                >
                  <div className={`flex max-w-[80%] gap-2 ${msg.sender === 'user' ? 'flex-row-reverse' : 'flex-row'}`}>
                    {/* Avatar */}
                    <div className="flex-shrink-0 mt-auto mb-1">
                      {msg.sender === 'user' ? (
                        <div className="w-8 h-8 rounded-full bg-indigo-100 flex items-center justify-center text-indigo-600">
                          <User size={16} />
                        </div>
                      ) : (
                        <div className="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center text-purple-600">
                          <Bot size={16} />
                        </div>
                      )}
                    </div>
                    
                    {/* Message Bubble */}
                    <div
                      className={`p-3 rounded-2xl ${
                        msg.sender === 'user'
                          ? 'bg-gradient-to-br from-indigo-500 to-purple-500 text-white rounded-br-none'
                          : 'bg-white text-gray-800 shadow-sm border border-gray-100 rounded-bl-none'
                      }`}
                    >
                      <p className="text-sm leading-relaxed whitespace-pre-wrap">{msg.text}</p>
                      <p className={`text-[10px] mt-1 text-right ${msg.sender === 'user' ? 'text-indigo-100' : 'text-gray-400'}`}>
                        {msg.timestamp.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' })}
                      </p>
                    </div>
                  </div>
                </div>
              ))}
              
              {isTyping && (
                <div className="flex justify-start w-full">
                  <div className="flex max-w-[80%] gap-2">
                    <div className="flex-shrink-0 mt-auto mb-1">
                      <div className="w-8 h-8 rounded-full bg-purple-100 flex items-center justify-center text-purple-600">
                        <Bot size={16} />
                      </div>
                    </div>
                    <div className="bg-white p-4 rounded-2xl rounded-bl-none shadow-sm border border-gray-100 flex items-center gap-1">
                      <motion.div className="w-2 h-2 bg-gray-400 rounded-full" animate={{ y: [0, -5, 0] }} transition={{ repeat: Infinity, duration: 0.6, delay: 0 }} />
                      <motion.div className="w-2 h-2 bg-gray-400 rounded-full" animate={{ y: [0, -5, 0] }} transition={{ repeat: Infinity, duration: 0.6, delay: 0.2 }} />
                      <motion.div className="w-2 h-2 bg-gray-400 rounded-full" animate={{ y: [0, -5, 0] }} transition={{ repeat: Infinity, duration: 0.6, delay: 0.4 }} />
                    </div>
                  </div>
                </div>
              )}
              <div ref={messagesEndRef} />
            </div>

            {/* Input Area */}
            <div className="p-4 bg-white border-t border-gray-100">
              <form onSubmit={handleSendMessage} className="flex gap-2">
                <input
                  type="text"
                  value={inputValue}
                  onChange={(e) => setInputValue(e.target.value)}
                  placeholder="Nhập câu hỏi tại đây..."
                  className="flex-1 bg-gray-100 text-gray-800 rounded-full px-5 py-3 focus:outline-none focus:ring-2 focus:ring-indigo-500 focus:bg-white transition-all"
                />
                <button
                  type="submit"
                  disabled={!inputValue.trim() || isTyping}
                  className={`p-3 rounded-full flex items-center justify-center transition-all ${
                    !inputValue.trim() || isTyping
                      ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                      : 'bg-indigo-600 text-white hover:bg-indigo-700 shadow-md hover:shadow-lg focus:ring-2 focus:ring-offset-2 focus:ring-indigo-500'
                  }`}
                >
                  <Send size={20} className={inputValue.trim() && !isTyping ? 'translate-x-0.5' : ''} />
                </button>
              </form>
            </div>
          </motion.div>
        )}
      </AnimatePresence>
    </>
  );
};

export default AIChatbot;
