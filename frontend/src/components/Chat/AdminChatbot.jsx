import React, { useState, useRef, useEffect } from 'react';
import api from '../../api/axiosConfig';
import { Sparkles, Send, Trash2, BarChart2, FileText, Eye, ThumbsUp, RefreshCw, X } from 'lucide-react';
import {
  ResponsiveContainer,
  LineChart,
  Line,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  Tooltip,
  Legend,
  CartesianGrid
} from 'recharts';

const AdminChatbot = ({ onClose }) => {
  const [messages, setMessages] = useState([
    {
      sender: 'assistant',
      text: 'Welcome Admin! I am your AI Analytics & Content Assistant. Ask me about view counts, performance trends, subscriber growth, category insights, or search your uploaded documents.',
      structuredFiles: [],
      chartData: null,
      timestamp: new Date()
    }
  ]);
  const [input, setInput] = useState('');
  const [loading, setLoading] = useState(false);
  const messagesEndRef = useRef(null);

  const starterPrompts = [
    "Give me analytics on my uploaded content",
    "Plot daily views for the last 7 days",
    "Show views by content category",
    "Which file has the lowest engagement?",
    "How can I improve engagement?"
  ];

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

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
      const response = await api.post('/chat/admin', {
        message: query
      });

      const assistantMsg = {
        sender: 'assistant',
        text: response.data.reply || "Here are your requested admin analytics:",
        structuredFiles: response.data.structuredFiles || [],
        chartData: response.data.chartData || null,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, assistantMsg]);
    } catch (err) {
      console.error('Admin Chatbot error:', err);
      setMessages(prev => [
        ...prev,
        {
          sender: 'assistant',
          text: 'Error retrieving analytics. Make sure your admin session is active.',
          structuredFiles: [],
          chartData: null,
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

  const renderChart = (chartData) => {
    if (!chartData || !chartData.dataPoints || chartData.dataPoints.length === 0) return null;

    const { chartType, title, xAxisKey, seriesKeys, dataPoints } = chartData;

    return (
      <div className="mt-3 bg-white border border-gray-200 rounded-xl p-4 shadow-sm w-full">
        <h4 className="font-bold text-sm text-gray-800 mb-3 flex items-center space-x-2">
          <BarChart2 className="w-4 h-4 text-indigo-600" />
          <span>{title}</span>
        </h4>
        <div className="h-64 w-full text-xs">
          <ResponsiveContainer width="100%" height="100%">
            {chartType === 'line' ? (
              <LineChart data={dataPoints}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey={xAxisKey || 'date'} stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip />
                <Legend />
                {seriesKeys && seriesKeys.includes('views') && (
                  <Line type="monotone" dataKey="views" name="Views" stroke="#2563eb" strokeWidth={2} dot={{ r: 4 }} />
                )}
                {seriesKeys && seriesKeys.includes('likes') && (
                  <Line type="monotone" dataKey="likes" name="Likes" stroke="#ec4899" strokeWidth={2} dot={{ r: 4 }} />
                )}
                {seriesKeys && seriesKeys.includes('subscribers') && (
                  <Line type="monotone" dataKey="subscribers" name="Subscribers" stroke="#10b981" strokeWidth={2} dot={{ r: 4 }} />
                )}
              </LineChart>
            ) : (
              <BarChart data={dataPoints}>
                <CartesianGrid strokeDasharray="3 3" stroke="#f1f5f9" />
                <XAxis dataKey={xAxisKey || 'category'} stroke="#64748b" />
                <YAxis stroke="#64748b" />
                <Tooltip />
                <Legend />
                <Bar dataKey="views" name="Views" fill="#4f46e5" radius={[4, 4, 0, 0]} />
              </BarChart>
            )}
          </ResponsiveContainer>
        </div>
      </div>
    );
  };

  return (
    <div className="bg-white border border-gray-200 rounded-2xl shadow-xl flex flex-col h-[650px] overflow-hidden">
      {/* Top Bar */}
      <div className="bg-gradient-to-r from-indigo-700 to-purple-800 text-white px-5 py-4 flex justify-between items-center shadow-md">
        <div className="flex items-center space-x-3">
          <div className="p-2 bg-white/10 rounded-xl">
            <Sparkles className="w-5 h-5 text-yellow-300 animate-spin-slow" />
          </div>
          <div>
            <h3 className="font-bold text-base">Admin AI Analytics & Insights</h3>
            <p className="text-xs text-indigo-100 opacity-90">Real-time DB Data & Chart Generation</p>
          </div>
        </div>
        <div className="flex items-center space-x-2">
          <button
            onClick={() => setMessages([{ sender: 'assistant', text: 'Chat reset. How can I analyze your content performance now?', timestamp: new Date() }])}
            title="Clear Chat"
            className="p-2 hover:bg-white/20 rounded-lg text-white/80 hover:text-white transition"
          >
            <Trash2 className="w-4 h-4" />
          </button>
          {onClose && (
            <button onClick={onClose} className="p-2 hover:bg-white/20 rounded-lg text-white">
              <X className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>

      {/* Messages Scroll Area */}
      <div className="flex-1 p-5 overflow-y-auto space-y-4 bg-slate-50">
        {messages.map((msg, idx) => (
          <div
            key={idx}
            className={`flex flex-col ${msg.sender === 'user' ? 'items-end' : 'items-start'}`}
          >
            <div
              className={`max-w-[90%] rounded-2xl px-4 py-3 text-sm shadow-sm ${
                msg.sender === 'user'
                  ? 'bg-indigo-600 text-white rounded-br-none'
                  : 'bg-white border border-gray-200 text-gray-800 rounded-bl-none'
              }`}
            >
              <div className="whitespace-pre-wrap leading-relaxed">{msg.text}</div>
            </div>

            {/* Render Interactive Chart if present */}
            {msg.chartData && renderChart(msg.chartData)}

            {/* Render File Result Cards if present */}
            {msg.structuredFiles && msg.structuredFiles.length > 0 && (
              <div className="mt-3 space-y-2 w-full max-w-[95%]">
                <p className="text-xs font-semibold text-gray-500 uppercase tracking-wider px-1">
                  Targeted Admin Content:
                </p>
                {msg.structuredFiles.map((file) => (
                  <div key={file.id} className="bg-white border border-gray-200 p-3 rounded-xl shadow-xs flex justify-between items-center">
                    <div className="flex items-center space-x-3 min-w-0">
                      <FileText className="w-5 h-5 text-indigo-600 flex-shrink-0" />
                      <div className="min-w-0">
                        <h5 className="font-semibold text-gray-900 text-sm truncate">{file.originalFileName}</h5>
                        <div className="flex items-center space-x-3 text-xs text-gray-500 mt-0.5">
                          <span className="bg-indigo-50 text-indigo-700 px-2 py-0.5 rounded font-medium">{file.categoryName}</span>
                          <span className="flex items-center space-x-1"><Eye className="w-3 h-3" /><span>{file.viewCount} views</span></span>
                          <span className="flex items-center space-x-1 text-pink-600"><ThumbsUp className="w-3 h-3" /><span>{file.likesCount} likes</span></span>
                        </div>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        ))}

        {loading && (
          <div className="flex items-center space-x-2 bg-white border border-gray-200 text-gray-600 px-4 py-3 rounded-2xl w-max shadow-sm">
            <RefreshCw className="w-4 h-4 text-indigo-600 animate-spin" />
            <span className="text-xs font-medium">Analyzing database metrics & generating insights...</span>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Starter Prompts */}
      {messages.length === 1 && (
        <div className="px-4 py-2.5 bg-slate-100 border-t border-gray-200 flex flex-wrap gap-2">
          {starterPrompts.map((prompt, idx) => (
            <button
              key={idx}
              onClick={() => handleSend(prompt)}
              className="text-xs bg-white hover:bg-indigo-50 text-gray-700 hover:text-indigo-700 border border-gray-200 rounded-lg px-3 py-1.5 font-medium transition shadow-2xs"
            >
              {prompt}
            </button>
          ))}
        </div>
      )}

      {/* Input Area */}
      <div className="p-4 bg-white border-t border-gray-200 flex items-center space-x-3">
        <textarea
          rows="1"
          value={input}
          onChange={(e) => setInput(e.target.value)}
          onKeyDown={handleKeyDown}
          placeholder="Ask for performance metrics, graphs, or document analysis..."
          className="flex-1 resize-none border border-gray-300 focus:border-indigo-500 focus:ring-1 focus:ring-indigo-500 rounded-xl px-4 py-3 text-sm text-gray-800 focus:outline-none"
        />
        <button
          onClick={() => handleSend()}
          disabled={loading || !input.trim()}
          className="p-3 bg-indigo-600 hover:bg-indigo-700 disabled:bg-gray-300 text-white rounded-xl shadow-md transition"
        >
          <Send className="w-4 h-4" />
        </button>
      </div>
    </div>
  );
};

export default AdminChatbot;
