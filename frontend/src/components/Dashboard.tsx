import { useState } from 'react';
import { 
  MessageSquare, 
  Users, 
  TrendingUp, 
  Clock, 
  Brain, 
  Calendar,
  Download,
  ArrowUpRight,
  ArrowDownRight
} from 'lucide-react';
import { DemoBanner } from './DemoBanner';

export function Dashboard() {
  const [selectedTimeRange, setSelectedTimeRange] = useState('7days');

  // Simple key metrics
  const stats = [
    { label: 'Total Conversations', value: '1,234', change: '+12%', trend: 'up', icon: MessageSquare, color: 'blue' },
    { label: 'Total Contacts', value: '856', change: '+8%', trend: 'up', icon: Users, color: 'green' },
    { label: 'Avg Response Time', value: '1.8s', change: '-12%', trend: 'down', icon: Clock, color: 'purple' },
    { label: 'Resolution Rate', value: '87%', change: '+5%', trend: 'up', icon: TrendingUp, color: 'orange' },
  ];

  const timeRanges = [
    { label: 'Last 7 Days', value: '7days' },
    { label: 'Last 30 Days', value: '30days' },
    { label: 'Last 90 Days', value: '90days' },
  ];

  const topQuestions = [
    { name: 'Technical Support', count: 324, percentage: 35, color: 'bg-blue-500' },
    { name: 'Product Questions', count: 278, percentage: 30, color: 'bg-purple-500' },
    { name: 'Order Status', count: 185, percentage: 20, color: 'bg-orange-500' },
    { name: 'Billing', count: 139, percentage: 15, color: 'bg-green-500' },
  ];

  const conversationTrends = [45, 52, 48, 58, 65, 72, 68];

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <DemoBanner />
      {/* Header */}
      <div className="mb-6 md:mb-8">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4">
          <div>
            <h1 className="text-gray-900 mb-2">Dashboard</h1>
            <p className="text-gray-600 text-sm md:text-base">Overview of your AI assistant performance</p>
          </div>
          <div className="flex items-center gap-2 md:gap-3 flex-wrap">
            {/* Time Range Selector */}
            <div className="relative">
              <select
                value={selectedTimeRange}
                onChange={(e) => setSelectedTimeRange(e.target.value)}
                className="px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent appearance-none pr-10 bg-white"
              >
                {timeRanges.map((range) => (
                  <option key={range.value} value={range.value}>
                    {range.label}
                  </option>
                ))}
              </select>
              <Calendar className="w-4 h-4 text-gray-400 absolute right-3 top-1/2 -translate-y-1/2 pointer-events-none" />
            </div>

            {/* Export Button */}
            <button className="px-3 md:px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2">
              <Download className="w-4 h-4 text-gray-600" />
              <span className="text-gray-700 hidden sm:inline">Export</span>
            </button>
          </div>
        </div>
      </div>

      {/* Stats Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6 mb-8">
        {stats.map((stat) => {
          const Icon = stat.icon;
          const isResponseTime = stat.label.includes('Response Time');
          const isPositive = isResponseTime ? stat.trend === 'down' : stat.trend === 'up';
          
          return (
            <div key={stat.label} className="bg-white rounded-xl border border-gray-200 p-6 hover:shadow-lg transition-shadow">
              <div className="flex items-center justify-between mb-4">
                <div className={`w-12 h-12 rounded-lg flex items-center justify-center bg-${stat.color}-50`}>
                  <Icon className={`w-6 h-6 text-${stat.color}-600`} />
                </div>
                <div className={`flex items-center gap-1 text-sm font-medium ${isPositive ? 'text-green-600' : 'text-red-600'}`}>
                  {isPositive ? (
                    <ArrowUpRight className="w-4 h-4" />
                  ) : (
                    <ArrowDownRight className="w-4 h-4" />
                  )}
                  {stat.change}
                </div>
              </div>
              <div className="text-3xl text-gray-900 mb-1">{stat.value}</div>
              <div className="text-gray-600 text-sm">{stat.label}</div>
            </div>
          );
        })}
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6">
        {/* AI Status */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-gray-900 mb-4 flex items-center gap-2">
            <Brain className="w-5 h-5 text-blue-600" />
            AI Assistant Status
          </h2>
          
          <div className="flex items-center justify-between p-4 bg-gradient-to-r from-green-50 to-blue-50 rounded-lg border border-green-200 mb-6">
            <div className="flex items-center gap-3">
              <div className="w-3 h-3 bg-green-500 rounded-full animate-pulse" />
              <div>
                <span className="text-gray-900">AI Assistant is Active</span>
                <div className="text-sm text-gray-600 mt-0.5">Responding to customer messages</div>
              </div>
            </div>
            <span className="text-green-700 text-sm px-3 py-1 bg-green-100 rounded-full font-medium">Online</span>
          </div>

          <div className="grid grid-cols-2 gap-4">
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl text-gray-900 mb-1">1,234</div>
              <div className="text-gray-600 text-sm">Conversations</div>
              <div className="text-gray-500 text-xs mt-1">This month</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl text-gray-900 mb-1">4.2</div>
              <div className="text-gray-600 text-sm">Msgs per Chat</div>
              <div className="text-gray-500 text-xs mt-1">Average</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl text-gray-900 mb-1">247</div>
              <div className="text-gray-600 text-sm">Knowledge Docs</div>
              <div className="text-gray-500 text-xs mt-1">In database</div>
            </div>
            <div className="p-4 bg-gray-50 rounded-lg">
              <div className="text-2xl text-gray-900 mb-1">87%</div>
              <div className="text-gray-600 text-sm">Auto-Resolved</div>
              <div className="text-gray-500 text-xs mt-1">No human needed</div>
            </div>
          </div>
        </div>

        {/* Top Questions */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <h2 className="text-gray-900 mb-4">Top Questions</h2>
          <div className="space-y-4">
            {topQuestions.map((question, idx) => (
              <div key={idx}>
                <div className="flex items-center justify-between text-sm mb-2">
                  <span className="text-gray-700">{question.name}</span>
                  <span className="text-gray-900 font-medium">{question.count}</span>
                </div>
                <div className="w-full bg-gray-200 rounded-full h-2">
                  <div 
                    className={`${question.color} h-2 rounded-full transition-all duration-500`}
                    style={{ width: `${question.percentage}%` }}
                  />
                </div>
              </div>
            ))}
          </div>
          <div className="mt-6 pt-4 border-t border-gray-200 text-center">
            <div className="text-2xl text-gray-900 mb-1">926</div>
            <div className="text-sm text-gray-600">Total Questions</div>
          </div>
        </div>
      </div>

      {/* Conversation Trends */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-gray-900">Conversation Volume</h2>
          <div className="flex items-center gap-2 text-sm text-gray-600">
            <TrendingUp className="w-4 h-4 text-green-600" />
            <span className="text-green-600 font-medium">+15%</span>
            <span>vs last period</span>
          </div>
        </div>
        <div className="h-64 flex items-end justify-between gap-3">
          {conversationTrends.map((height, index) => (
            <div key={index} className="flex-1 flex flex-col items-center gap-3 group">
              <div 
                className="w-full bg-gradient-to-t from-blue-500 to-purple-500 rounded-t group-hover:from-blue-600 group-hover:to-purple-600 transition-all cursor-pointer relative" 
                style={{ height: `${height}%` }}
              >
                <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-900 text-white px-2 py-1 rounded text-xs opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                  {Math.round(height * 10)} convs
                </div>
              </div>
              <span className="text-gray-500 text-xs">
                {selectedTimeRange === '7days' ? 
                  ['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'][index] : 
                  `Day ${index + 1}`
                }
              </span>
            </div>
          ))}
        </div>
        <div className="mt-6 pt-4 border-t border-gray-200 flex items-center justify-between text-sm">
          <div>
            <span className="text-gray-600">Peak: </span>
            <span className="text-gray-900 font-medium">720 conversations</span>
          </div>
          <div>
            <span className="text-gray-600">Average: </span>
            <span className="text-gray-900 font-medium">176 per day</span>
          </div>
          <div>
            <span className="text-gray-600">Total: </span>
            <span className="text-gray-900 font-medium">1,234 this period</span>
          </div>
        </div>
      </div>
    </div>
  );
}
