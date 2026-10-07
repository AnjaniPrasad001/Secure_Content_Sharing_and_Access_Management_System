import React, { useState, useRef, useEffect } from 'react';
import axios from 'axios';
import { X, Send, Trash2, Sparkles, FileText, Eye, ThumbsUp, ArrowRight } from 'lucide-react';

const PublicChatbot = ({ onSelectFile }) => {
  const [isOpen, setIsOpen] = useState(false);
  const [messages, setMessages] = useState([
    {
      sender: 'assistant',
      text: 'Hello! I am your FileVault Discovery Assistant. Ask me to search public files, summarize documents, find recent uploads, or recommend learning resources!',
      structuredFiles: [],
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const starterQuestions = [
    "Show recent public uploads",
    "Find files about Java and Spring Boot",
    "Recommend beginner programming materials",
    "What are the top viewed files?"
  ];

  useEffect(() => {
    if (isOpen) {
      scrollToBottom();
    }
  }, [messages, isOpen]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSend = async (textToSend) => {
    const query = (textToSend || input).trim();
    if (!query || loading) return;

    const userMsg = { sender: 'user', text: query, timestamp: new Date() };
    setMessages(prev => [...prev, userMsg]);
    if (!textToSend) setInput('');
    setLoading(true);

    try {
      const response = await axios.post('http://localhost:8080/api/chat/public', {
        message: query
      });

      const assistantMsg = {
        sender: 'assistant',
        text: response.data.reply || "Here is what I found for you:",
        structuredFiles: response.data.structuredFiles || [],
        llmActive: response.data.llmActive,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, assistantMsg]);
    } catch (err) {
      console.error('Chatbot request failed:', err);
      setMessages(prev => [
        ...prev,
        {
          sender: 'assistant',
          text: 'Sorry, I encountered an issue reaching the search service. Please try again.',
          structuredFiles: [],
          timestamp: new Date()
        }
      ]);
    } finally {
      setLoading(false);
    }
  };

  const handleKeyDown = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const clearChat = () => {
    setMessages([
      {
        sender: 'assistant',
        text: 'Chat history cleared. How else can I help you discover content today?',
        structuredFiles: [],
        timestamp: new Date()
      }
    ]);
  };

  return (
    <div className="fixed bottom-6 right-6 z-50">
      {/* Floating Toggle Button */}
      {!isOpen && (
        <button
          onClick={() => setIsOpen(true)}
          className="flex items-center space-x-2 bg-blue-600 hover:bg-blue-700 text-white font-medium px-5 py-3.5 rounded-full shadow-2xl transition-all duration-300 transform hover:scale-105"
        >
          <Sparkles className="w-5 h-5 animate-pulse text-yellow-300" />
          <span>Ask AI Assistant</span>
        </button>
      )}

      {/* Chat Window Panel */}
      {isOpen && (
        <div className="bg-white border border-gray-200 w-[92vw] sm:w-[420px] h-[580px] rounded-2xl shadow-2xl flex flex-col overflow-hidden transition-all duration-300">
          {/* Header */}
          <div className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white px-4 py-3.5 flex justify-between items-center shadow-md">
            <div className="flex items-center space-x-2.5">
              <div className="p-2 bg-white/10 rounded-lg">
                <Sparkles className="w-5 h-5 text-yellow-300" />
              </div>
              <div>
                <h3 className="font-bold text-base leading-tight">Content Discovery AI</h3>
                <p className="text-xs text-blue-100 opacity-90">RAG Powered Search & Recommendations</p>
              </div>
            </div>
            <div className="flex items-center space-x-1">
              <button
                onClick={clearChat}
                title="Clear Chat"
                className="p-1.5 hover:bg-white/20 rounded-lg text-white/80 hover:text-white transition"
              >
                <Trash2 className="w-4 h-4" />
              </button>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1.5 hover:bg-white/20 rounded-lg text-white transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>
          </div>

          {/* Messages Body */}
          <div className="flex-1 p-4 overflow-y-auto space-y-4 bg-slate-50">
            {messages.map((msg, idx) => (
              <div
                key={idx}
                className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
              >
                <div
                  className={`max-w-[88%] rounded-2xl px-4 py-3 text-sm shadow-sm ${
                    msg.sender === 'user'
                      ? 'bg-blue-600 text-white rounded-br-none'
                      : 'bg-white border border-gray-200 text-gray-800 rounded-bl-none'
                  }`}
                >
                  <p className="whitespace-pre-wrap leading-relaxed">{msg.text}</p>
                </div>

                {/* Render Structured Content File Cards */}
                {msg.structuredFiles && msg.structuredFiles.length > 0 && (
                  <div className="mt-3 space-y-2 w-full max-w-[95%]">
                    <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider px-1">
                      Matching Catalog Files ({msg.structuredFiles.length}):
                    </p>
                    {msg.structuredFiles.map((file) => (
                      <div
                        key={file.id}
                        className="bg-white border border-gray-200 hover:border-blue-400 p-3 rounded-xl shadow-xs transition duration-200 flex flex-col justify-between"
                      >
                        <div className="flex items-start space-x-2">
                          <FileText className="w-4 h-4 text-blue-600 mt-0.5 flex-shrink-0" />
                          <div className="flex-1 min-w-0">
                            <h4 className="font-semibold text-gray-900 text-sm truncate">
                              {file.originalFileName}
                            </h4>
                            <p className="text-xs text-gray-500 line-clamp-1">
                              {file.description || 'No description available'}
                            </p>
                            <div className="flex items-center space-x-3 mt-1.5 text-xs text-gray-500">
                              <span className="bg-blue-50 text-blue-700 px-2 py-0.5 rounded font-medium text-[11px]">
                                {file.categoryName || 'General'}
                              </span>
                              <span className="flex items-center space-x-1">
                                <Eye className="w-3 h-3" />
                                <span>{file.viewCount}</span>
                              </span>
                              {file.likesCount > 0 && (
                                <span className="flex items-center space-x-1 text-pink-600">
                                  <ThumbsUp className="w-3 h-3" />
                                  <span>{file.likesCount}</span>
                                </span>
                              )}
                            </div>
                          </div>
                        </div>
                        {onSelectFile && (
                          <button
                            onClick={() => onSelectFile(file)}
                            className="mt-2.5 w-full flex items-center justify-center space-x-1 text-xs bg-blue-50 hover:bg-blue-100 text-blue-700 py-1.5 rounded-lg font-medium transition"
                          >
                            <span>View Content Details</span>
                            <ArrowRight className="w-3.5 h-3.5" />
                          </button>
                        )}
                      </div>
                    ))}
                  </div>
                )}
              </div>
            ))}

            {loading && (
              <div className="flex items-center space-x-2 bg-white border border-gray-200 text-gray-500 px-4 py-2.5 rounded-2xl w-max shadow-sm">
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce"></div>
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce [animation-delay:0.2s]"></div>
                <div className="w-2 h-2 bg-blue-600 rounded-full animate-bounce [animation-delay:0.4s]"></div>
                <span className="text-xs ml-1 font-medium text-gray-600">Searching content...</span>
              </div>
            )}

            <div ref={messagesEndRef} />
          </div>

          {/* Starter Prompts */}
          {messages.length === 1 && (
            <div className="px-3 py-2 bg-slate-100 border-t border-gray-200 flex flex-wrap gap-1.5">
              {starterQuestions.map((q, idx) => (
                <button
                  key={idx}
                  onClick={() => handleSend(q)}
                  className="text-xs bg-white hover:bg-blue-50 text-gray-700 hover:text-blue-700 border border-gray-200 rounded-full px-3 py-1 font-medium transition"
                >
                  {q}
                </button>
              ))}
            </div>
          )}

          {/* Input Footer */}
          <div className="p-3 bg-white border-t border-gray-200 flex items-center space-x-2">
            <textarea
              rows="1"
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onKeyDown={handleKeyDown}
              placeholder="Ask about files, topics, or recommendations..."
              className="flex-1 resize-none border border-gray-300 focus:border-blue-500 focus:ring-1 focus:ring-blue-500 rounded-xl px-3.5 py-2.5 text-sm text-gray-800 focus:outline-none"
            />
            <button
              onClick={() => handleSend()}
              disabled={loading || !input.trim()}
              className="p-2.5 bg-blue-600 hover:bg-blue-700 disabled:bg-gray-300 text-white rounded-xl shadow-md transition"
            >
              <Send className="w-4 h-4" />
            </button>
          </div>
        </div>
      )}
    </div>
  );
};

export default PublicChatbot;
