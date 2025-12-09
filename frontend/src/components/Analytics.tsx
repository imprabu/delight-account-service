import { useState, useEffect } from 'react';
import { 
  BarChart3,
  LineChart,
  PieChart,
  TrendingUp,
  Filter,
  Download,
  Plus,
  X,
  Crown,
  FileText,
  Calendar,
  Users,
  MessageSquare,
  Clock,
  Target,
  Settings,
  Save,
  Play,
  Brain,
  CheckCircle,
  ChevronRight,
  Sparkles,
  Link2,
  Layers,
  AlertCircle
} from 'lucide-react';
import { DemoBanner } from './DemoBanner';

interface AnalyticsProps {
  currentPlan?: 'free' | 'professional' | 'enterprise';
  onNavigate?: (screen: string) => void;
}

export function Analytics({ currentPlan = 'professional', onNavigate }: AnalyticsProps) {
  const [selectedReport, setSelectedReport] = useState('overview');
  const [showReportBuilder, setShowReportBuilder] = useState(false);
  const [selectedTimeRange, setSelectedTimeRange] = useState('30days');
  
  // Custom report builder state
  const [builderStep, setBuilderStep] = useState(1);
  const [primaryEntity, setPrimaryEntity] = useState('');
  const [joinedEntities, setJoinedEntities] = useState<string[]>([]);
  const [selectedFields, setSelectedFields] = useState<Array<{entityId: string, fieldId: string}>>([]);
  const [selectedMetrics, setSelectedMetrics] = useState<string[]>([]);
  const [selectedGroupBy, setSelectedGroupBy] = useState('');
  const [selectedChartType, setSelectedChartType] = useState('bar');
  const [reportName, setReportName] = useState('');
  const [filters, setFilters] = useState<any[]>([]);
  const [showFilterDropdown, setShowFilterDropdown] = useState(false);
  
  // Saved custom reports
  const [savedReports, setSavedReports] = useState<any[]>([
    {
      id: 'custom-1',
      name: 'VIP Customer Response Analysis',
      primaryEntity: 'conversations',
      joinedEntities: [{ entityId: 'contacts', fromEntity: 'conversations' }],
      fields: [
        { entityId: 'contacts', fieldId: 'name' },
        { entityId: 'contacts', fieldId: 'lifecycle_stage' },
        { entityId: 'conversations', fieldId: 'response_time' },
        { entityId: 'conversations', fieldId: 'customer_satisfaction' },
      ],
      metrics: ['Average', 'Count'],
      groupBy: 'contacts.lifecycle_stage',
      chartType: 'bar',
      filters: [
        { entityId: 'contacts', field: 'lifecycle_stage', operator: 'equals', value: 'VIP', label: 'Contacts - Lifecycle Stage' }
      ],
      createdAt: '2024-12-01',
      lastRun: '2024-12-02'
    },
    {
      id: 'custom-2',
      name: 'Agent Performance by Channel',
      primaryEntity: 'conversations',
      joinedEntities: [{ entityId: 'agents', fromEntity: 'conversations' }],
      fields: [
        { entityId: 'agents', fieldId: 'name' },
        { entityId: 'conversations', fieldId: 'channel' },
        { entityId: 'conversations', fieldId: 'resolution_time' },
        { entityId: 'conversations', fieldId: 'status' },
      ],
      metrics: ['Average', 'Count'],
      groupBy: 'conversations.channel',
      chartType: 'line',
      filters: [],
      createdAt: '2024-11-28',
      lastRun: '2024-12-01'
    }
  ]);
  const [viewingCustomReport, setViewingCustomReport] = useState<string | null>(null);

  // Define join relationships - like foreign keys in a database
  const joinRelationships = [
    // Conversations -> Contacts (via contact_id foreign key)
    {
      from: 'conversations',
      to: 'contacts',
      type: 'many-to-one',
      fromField: 'contact_id',
      toField: 'id',
      description: 'Each conversation belongs to one contact',
      label: 'Conversation → Contact'
    },
    // Contacts -> Conversations (reverse relationship)
    {
      from: 'contacts',
      to: 'conversations',
      type: 'one-to-many',
      fromField: 'id',
      toField: 'contact_id',
      description: 'Each contact can have many conversations',
      label: 'Contact → Conversations'
    },
    // Conversations -> Agents (via assigned_agent_id foreign key)
    {
      from: 'conversations',
      to: 'agents',
      type: 'many-to-one',
      fromField: 'assigned_agent_id',
      toField: 'id',
      description: 'Each conversation is assigned to one agent',
      label: 'Conversation → Agent'
    },
    // Agents -> Conversations (reverse relationship)
    {
      from: 'agents',
      to: 'conversations',
      type: 'one-to-many',
      fromField: 'id',
      toField: 'assigned_agent_id',
      description: 'Each agent can handle many conversations',
      label: 'Agent → Conversations'
    },
    // Conversations -> Intents (via primary_intent_id foreign key)
    {
      from: 'conversations',
      to: 'intents',
      type: 'many-to-one',
      fromField: 'primary_intent_id',
      toField: 'id',
      description: 'Each conversation has one primary intent',
      label: 'Conversation → Intent'
    },
    // Intents -> Conversations (reverse relationship)
    {
      from: 'intents',
      to: 'conversations',
      type: 'one-to-many',
      fromField: 'id',
      toField: 'primary_intent_id',
      description: 'Each intent can be detected in many conversations',
      label: 'Intent → Conversations'
    },
    // Contacts -> Agents (through conversations - requires 2 joins)
    {
      from: 'contacts',
      to: 'agents',
      type: 'many-to-many',
      via: 'conversations',
      description: 'Contacts connect to agents through their conversations',
      label: 'Contact → Agent (via Conversations)',
      requiresIntermediate: true
    },
    // Knowledge Base -> Conversations (via junction table conversation_kb_usage)
    {
      from: 'knowledge_base',
      to: 'conversations',
      type: 'many-to-many',
      via: 'conversation_kb_usage',
      description: 'Knowledge base articles can be used in many conversations',
      label: 'Knowledge Base → Conversations',
      isJunctionTable: true
    },
    // Conversations -> Knowledge Base (reverse)
    {
      from: 'conversations',
      to: 'knowledge_base',
      type: 'many-to-many',
      via: 'conversation_kb_usage',
      description: 'Conversations can reference multiple knowledge base articles',
      label: 'Conversation → Knowledge Base',
      isJunctionTable: true
    },
    // AI Models -> Conversations (via junction table conversation_ai_requests)
    {
      from: 'ai_models',
      to: 'conversations',
      type: 'many-to-many',
      via: 'conversation_ai_requests',
      description: 'AI models can be used in many conversations',
      label: 'AI Model → Conversations',
      isJunctionTable: true
    },
    // Conversations -> AI Models (reverse)
    {
      from: 'conversations',
      to: 'ai_models',
      type: 'many-to-many',
      via: 'conversation_ai_requests',
      description: 'Conversations can use multiple AI models',
      label: 'Conversation → AI Models',
      isJunctionTable: true
    },
  ];

  // Entity definitions with their properties
  const entities = [
    {
      id: 'conversations',
      name: 'Conversations',
      icon: MessageSquare,
      description: 'Analyze chat sessions and messages',
      color: 'blue',
      fields: [
        { id: 'id', name: 'Conversation ID', type: 'text' },
        { id: 'contact_id', name: 'Contact ID (FK)', type: 'text', isForeignKey: true },
        { id: 'assigned_agent_id', name: 'Agent ID (FK)', type: 'text', isForeignKey: true },
        { id: 'primary_intent_id', name: 'Intent ID (FK)', type: 'text', isForeignKey: true },
        { id: 'channel', name: 'Channel', type: 'select', options: ['Web Chat', 'Instagram', 'Facebook', 'WhatsApp', 'Email'] },
        { id: 'status', name: 'Status', type: 'select', options: ['Active', 'Resolved', 'Pending', 'Escalated'] },
        { id: 'created_at', name: 'Created Date', type: 'date' },
        { id: 'resolved_at', name: 'Resolved Date', type: 'date' },
        { id: 'message_count', name: 'Message Count', type: 'number' },
        { id: 'response_time', name: 'Response Time (seconds)', type: 'number' },
        { id: 'resolution_time', name: 'Resolution Time (minutes)', type: 'number' },
        { id: 'customer_satisfaction', name: 'Satisfaction Score', type: 'number' },
        { id: 'resolution_type', name: 'Resolution Type', type: 'select', options: ['Auto-Resolved', 'Human Resolved', 'Unresolved'] },
        { id: 'tags', name: 'Tags', type: 'tags' },
      ]
    },
    {
      id: 'contacts',
      name: 'Contacts',
      icon: Users,
      description: 'Track customer contact records',
      color: 'green',
      fields: [
        { id: 'id', name: 'Contact ID', type: 'text' },
        { id: 'name', name: 'Full Name', type: 'text' },
        { id: 'email', name: 'Email', type: 'text' },
        { id: 'phone', name: 'Phone', type: 'text' },
        { id: 'created_at', name: 'Created Date', type: 'date' },
        { id: 'last_contact', name: 'Last Contact Date', type: 'date' },
        { id: 'conversation_count', name: 'Total Conversations', type: 'number' },
        { id: 'total_messages', name: 'Total Messages', type: 'number' },
        { id: 'avg_satisfaction', name: 'Average Satisfaction', type: 'number' },
        { id: 'status', name: 'Status', type: 'select', options: ['Active', 'Inactive', 'Blocked'] },
        { id: 'tags', name: 'Tags', type: 'tags' },
        { id: 'lifecycle_stage', name: 'Lifecycle Stage', type: 'select', options: ['Lead', 'Customer', 'VIP', 'Churned'] },
      ]
    },
    {
      id: 'agents',
      name: 'Agents',
      icon: Users,
      description: 'Monitor team performance',
      color: 'purple',
      fields: [
        { id: 'id', name: 'Agent ID', type: 'text' },
        { id: 'name', name: 'Agent Name', type: 'text' },
        { id: 'email', name: 'Email', type: 'text' },
        { id: 'role', name: 'Role', type: 'select', options: ['Admin', 'Agent', 'Manager', 'Viewer'] },
        { id: 'status', name: 'Status', type: 'select', options: ['Online', 'Offline', 'Away', 'Busy'] },
        { id: 'conversations_handled', name: 'Conversations Handled', type: 'number' },
        { id: 'avg_response_time', name: 'Avg Response Time', type: 'number' },
        { id: 'avg_resolution_time', name: 'Avg Resolution Time', type: 'number' },
        { id: 'satisfaction_score', name: 'Satisfaction Score', type: 'number' },
        { id: 'active_since', name: 'Active Since', type: 'date' },
        { id: 'last_active', name: 'Last Active', type: 'date' },
      ]
    },
    {
      id: 'intents',
      name: 'Intents',
      icon: Target,
      description: 'Understand customer intent patterns',
      color: 'orange',
      fields: [
        { id: 'id', name: 'Intent ID', type: 'text' },
        { id: 'name', name: 'Intent Name', type: 'text' },
        { id: 'category', name: 'Category', type: 'select', options: ['Support', 'Sales', 'Billing', 'General', 'Technical'] },
        { id: 'conversation_count', name: 'Conversation Count', type: 'number' },
        { id: 'confidence_score', name: 'Avg Confidence Score', type: 'number' },
        { id: 'resolution_rate', name: 'Resolution Rate', type: 'number' },
        { id: 'avg_response_time', name: 'Avg Response Time', type: 'number' },
        { id: 'created_at', name: 'Created Date', type: 'date' },
        { id: 'last_triggered', name: 'Last Triggered', type: 'date' },
        { id: 'is_active', name: 'Is Active', type: 'boolean' },
      ]
    },
    {
      id: 'knowledge_base',
      name: 'Knowledge Base',
      icon: FileText,
      description: 'Measure content effectiveness',
      color: 'indigo',
      fields: [
        { id: 'id', name: 'Document ID', type: 'text' },
        { id: 'title', name: 'Title', type: 'text' },
        { id: 'category', name: 'Category', type: 'text' },
        { id: 'search_count', name: 'Search Count', type: 'number' },
        { id: 'usage_count', name: 'Usage Count', type: 'number' },
        { id: 'success_rate', name: 'Success Rate', type: 'number' },
        { id: 'avg_relevance_score', name: 'Avg Relevance Score', type: 'number' },
        { id: 'created_at', name: 'Created Date', type: 'date' },
        { id: 'updated_at', name: 'Last Updated', type: 'date' },
        { id: 'last_used', name: 'Last Used', type: 'date' },
        { id: 'word_count', name: 'Word Count', type: 'number' },
        { id: 'is_published', name: 'Is Published', type: 'boolean' },
      ]
    },
    {
      id: 'ai_models',
      name: 'AI Models',
      icon: Brain,
      description: 'Track AI model performance',
      color: 'pink',
      fields: [
        { id: 'id', name: 'Model ID', type: 'text' },
        { id: 'name', name: 'Model Name', type: 'text' },
        { id: 'provider', name: 'Provider', type: 'select', options: ['OpenAI', 'Anthropic', 'Google', 'Custom'] },
        { id: 'model_version', name: 'Version', type: 'text' },
        { id: 'total_requests', name: 'Total Requests', type: 'number' },
        { id: 'avg_response_time', name: 'Avg Response Time (ms)', type: 'number' },
        { id: 'success_rate', name: 'Success Rate', type: 'number' },
        { id: 'avg_tokens_used', name: 'Avg Tokens Used', type: 'number' },
        { id: 'total_cost', name: 'Total Cost', type: 'number' },
        { id: 'accuracy_score', name: 'Accuracy Score', type: 'number' },
        { id: 'last_used', name: 'Last Used', type: 'date' },
      ]
    },
  ];

  // Curated reports available for all plans
  const curatedReports = [
    { 
      id: 'overview', 
      name: 'Overview', 
      description: 'Key metrics summary',
      icon: BarChart3,
    },
    { 
      id: 'conversations', 
      name: 'Conversations', 
      description: 'Chat volume and trends',
      icon: MessageSquare,
    },
    { 
      id: 'contacts', 
      name: 'Contacts', 
      description: 'Contact growth and activity',
      icon: Users,
    },
  ];

  const metricOptions = ['Count', 'Sum', 'Average', 'Min', 'Max', 'Percentage', 'Growth Rate'];
  const allTimeRanges = [
    { label: 'Last 7 Days', value: '7days', daysRequired: 7 },
    { label: 'Last 15 Days', value: '15days', daysRequired: 15 },
    { label: 'Last 30 Days', value: '30days', daysRequired: 30 },
    { label: 'Last 90 Days', value: '90days', daysRequired: 90 },
    { label: 'This Year', value: 'year', daysRequired: 365 },
    { label: 'Custom Range', value: 'custom', daysRequired: 365 },
  ];

  const canAccessCustomReports = currentPlan === 'professional' || currentPlan === 'enterprise';
  
  // Tier-based limits:
  // Free: Only curated reports, no custom reports, export limited to 15 days
  // Professional: 10 custom reports, export up to 90 days
  // Enterprise: 50 custom reports, export up to 90 days
  const getCustomReportLimit = () => {
    if (currentPlan === 'free') return 0;
    if (currentPlan === 'professional') return 10;
    return 50; // enterprise
  };
  
  const getExportDaysLimit = () => {
    if (currentPlan === 'free') return 15;
    return 90; // professional and enterprise
  };
  
  const customReportLimit = getCustomReportLimit();
  const exportDaysLimit = getExportDaysLimit();
  const hasReachedCustomReportLimit = savedReports.length >= customReportLimit;
  
  // Filter time ranges based on plan
  const timeRanges = allTimeRanges.filter(range => range.daysRequired <= exportDaysLimit);
  
  // Ensure selected time range is valid for the current plan
  useEffect(() => {
    const isValidRange = timeRanges.some(range => range.value === selectedTimeRange);
    if (!isValidRange && timeRanges.length > 0) {
      // Default to the longest available range
      const longestRange = timeRanges[timeRanges.length - 1];
      setSelectedTimeRange(longestRange.value);
    }
  }, [currentPlan, exportDaysLimit]);

  const getPrimaryEntity = () => entities.find(e => e.id === primaryEntity);
  const getAllSelectedEntities = () => {
    const allIds = [primaryEntity, ...joinedEntities.map(j => j.entityId)].filter(Boolean);
    return entities.filter(e => allIds.includes(e.id));
  };

  // Get available entities that can be joined with the primary entity
  const getJoinableEntities = () => {
    if (!primaryEntity) return [];
    
    // Find all possible joins from the primary entity
    const possibleJoins = joinRelationships.filter(rel => rel.from === primaryEntity);
    
    // Filter out already joined entities
    const alreadyJoined = joinedEntities.map(j => j.entityId);
    
    return possibleJoins
      .filter(rel => !alreadyJoined.includes(rel.to))
      .map(rel => {
        const entity = entities.find(e => e.id === rel.to);
        return {
          ...entity,
          joinInfo: rel
        };
      })
      .filter(Boolean);
  };

  // Get join relationships for selected entities
  const getActiveJoins = () => {
    return joinedEntities.map(join => {
      const relationship = joinRelationships.find(
        rel => rel.from === join.fromEntity && rel.to === join.entityId
      );
      const entity = entities.find(e => e.id === join.entityId);
      return {
        entity,
        relationship
      };
    });
  };

  // Get all fields from selected entities for filtering
  const getAllFieldsForFilters = () => {
    const allEntities = getAllSelectedEntities();
    const allFields: any[] = [];
    allEntities.forEach(entity => {
      entity.fields.forEach(field => {
        allFields.push({
          ...field,
          entityId: entity.id,
          entityName: entity.name,
          displayName: `${entity.name} - ${field.name}`
        });
      });
    });
    return allFields;
  };

  // Get filterable fields (select, date, boolean, number types)
  const getFilterableFields = () => {
    return getAllFieldsForFilters().filter(f => 
      ['select', 'date', 'boolean', 'number'].includes(f.type)
    );
  };

  const addFilter = (field: any) => {
    const newFilter = {
      id: Date.now(),
      entityId: field.entityId,
      field: field.id,
      label: field.displayName,
      operator: field.type === 'date' ? 'after' : field.type === 'number' ? 'greater_than' : 'equals',
      value: '',
      type: field.type,
      options: field.options,
    };
    setFilters([...filters, newFilter]);
    setShowFilterDropdown(false);
  };

  const removeFilter = (id: number) => {
    setFilters(filters.filter(f => f.id !== id));
  };

  const updateFilter = (id: number, key: string, value: any) => {
    setFilters(filters.map(f => f.id === id ? { ...f, [key]: value } : f));
  };

  const toggleJoinEntity = (entityId: string, joinInfo: any) => {
    const existingJoin = joinedEntities.find(j => j.entityId === entityId);
    
    if (existingJoin) {
      setJoinedEntities(joinedEntities.filter(j => j.entityId !== entityId));
      // Remove fields from this entity
      setSelectedFields(selectedFields.filter(sf => sf.entityId !== entityId));
      // Remove filters from this entity
      setFilters(filters.filter(f => f.entityId !== entityId));
    } else {
      const newJoin = {
        entityId: entityId,
        fromEntity: primaryEntity,
        relationship: joinInfo
      };
      setJoinedEntities([...joinedEntities, newJoin]);
    }
  };

  const resetBuilder = () => {
    setBuilderStep(1);
    setPrimaryEntity('');
    setJoinedEntities([]);
    setSelectedFields([]);
    setSelectedMetrics([]);
    setSelectedGroupBy('');
    setSelectedChartType('bar');
    setReportName('');
    setFilters([]);
  };

  const isEntityJoined = (entityId: string) => {
    return joinedEntities.some(j => j.entityId === entityId);
  };

  const generateReport = () => {
    const newReport = {
      id: `custom-${Date.now()}`,
      name: reportName || `Untitled Report ${savedReports.length + 1}`,
      primaryEntity,
      joinedEntities,
      fields: selectedFields,
      metrics: selectedMetrics,
      groupBy: selectedGroupBy,
      chartType: selectedChartType,
      filters,
      createdAt: new Date().toISOString().split('T')[0],
      lastRun: new Date().toISOString().split('T')[0]
    };
    
    setSavedReports([newReport, ...savedReports]);
    setViewingCustomReport(newReport.id);
    setShowReportBuilder(false);
    setSelectedReport('');
  };

  const deleteCustomReport = (reportId: string) => {
    setSavedReports(savedReports.filter(r => r.id !== reportId));
    if (viewingCustomReport === reportId) {
      setViewingCustomReport(null);
      setSelectedReport('overview');
    }
  };

  const editCustomReport = (report: any) => {
    setReportName(report.name);
    setPrimaryEntity(report.primaryEntity);
    setJoinedEntities(report.joinedEntities);
    setSelectedFields(report.fields);
    setSelectedMetrics(report.metrics);
    setSelectedGroupBy(report.groupBy);
    setSelectedChartType(report.chartType);
    setFilters(report.filters);
    setShowReportBuilder(true);
    setBuilderStep(1);
    
    // Remove the report since we'll recreate it
    setSavedReports(savedReports.filter(r => r.id !== report.id));
  };

  const getCurrentReport = () => {
    if (viewingCustomReport) {
      return savedReports.find(r => r.id === viewingCustomReport);
    }
    return null;
  };

  const toggleField = (entityId: string, fieldId: string) => {
    const exists = selectedFields.some(sf => sf.entityId === entityId && sf.fieldId === fieldId);
    if (exists) {
      setSelectedFields(selectedFields.filter(sf => !(sf.entityId === entityId && sf.fieldId === fieldId)));
    } else {
      setSelectedFields([...selectedFields, { entityId, fieldId }]);
    }
  };

  const isFieldSelected = (entityId: string, fieldId: string) => {
    return selectedFields.some(sf => sf.entityId === entityId && sf.fieldId === fieldId);
  };

  // Sample data for charts
  const overviewData = {
    conversationsByChannel: [
      { channel: 'Web Chat', count: 542, color: 'bg-blue-500' },
      { channel: 'Instagram', count: 324, color: 'bg-pink-500' },
      { channel: 'Facebook', count: 245, color: 'bg-indigo-500' },
      { channel: 'WhatsApp', count: 123, color: 'bg-green-500' },
    ],
    responseTimeByDay: [
      { day: 'Mon', time: 1.5 },
      { day: 'Tue', time: 1.8 },
      { day: 'Wed', time: 1.6 },
      { day: 'Thu', time: 2.1 },
      { day: 'Fri', time: 1.9 },
      { day: 'Sat', time: 1.4 },
      { day: 'Sun', time: 1.3 },
    ],
    resolutionRate: [
      { status: 'Auto-Resolved', percentage: 72, color: 'bg-green-500' },
      { status: 'Human Resolved', percentage: 15, color: 'bg-blue-500' },
      { status: 'Pending', percentage: 10, color: 'bg-yellow-500' },
      { status: 'Escalated', percentage: 3, color: 'bg-red-500' },
    ]
  };

  return (
    <div className="p-4 md:p-6 lg:p-8">
      <DemoBanner />
      {/* Header */}
      <div className="mb-6 md:mb-8">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4 mb-6">
          <div>
            <h1 className="text-gray-900 mb-2">Analytics</h1>
            <p className="text-gray-600 text-sm md:text-base">Explore insights with curated reports or build your own</p>
          </div>
          {!showReportBuilder && (
            <div className="flex flex-wrap items-center gap-2 md:gap-3">
              {/* Time Range */}
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

              {/* Export */}
              <div className="relative group">
                <button className="px-3 md:px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2">
                  <Download className="w-4 h-4 text-gray-600" />
                  <span className="text-gray-700 hidden sm:inline">Export</span>
                </button>
                {/* Export limit tooltip */}
                <div className="absolute top-full right-0 mt-2 w-64 bg-gray-900 text-white text-xs rounded-lg p-3 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all z-10">
                  <div className="flex items-center gap-2 mb-1">
                    <Calendar className="w-3 h-3" />
                    <span className="font-medium">Export Limit: {exportDaysLimit} days</span>
                  </div>
                  <p className="text-gray-300">
                    {currentPlan === 'free' 
                      ? 'Upgrade to Professional for 90-day exports'
                      : 'Export data from the last 90 days'}
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Plan Limitations Notice */}
        {!showReportBuilder && currentPlan === 'free' && (
          <div className="mb-4 bg-amber-50 border border-amber-200 rounded-lg p-4 flex items-start gap-3">
            <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
            <div className="flex-1">
              <p className="text-sm text-amber-900">
                <span className="font-medium">Free Plan Limitations:</span> Custom reports are not available. Exports limited to last {exportDaysLimit} days.
              </p>
              <button
                onClick={() => onNavigate && onNavigate('billing')}
                className="mt-2 text-sm text-amber-700 hover:text-amber-800 font-medium underline"
              >
                Upgrade to unlock all features
              </button>
            </div>
          </div>
        )}

        {/* Report Type Selection */}
        {!showReportBuilder && (
          <div className="space-y-4 mb-6">
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {/* Curated Reports Card */}
              <div className="bg-gradient-to-br from-blue-50 to-indigo-50 border-2 border-blue-200 rounded-xl p-4 md:p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-blue-600 rounded-lg flex items-center justify-center">
                      <BarChart3 className="w-6 h-6 text-white" />
                    </div>
                    <div>
                      <h3 className="text-gray-900 font-medium">Curated Reports</h3>
                      <p className="text-sm text-gray-600">Pre-built insights ready to view</p>
                    </div>
                  </div>
                  <div className="px-2 py-1 bg-blue-100 text-blue-700 rounded text-xs font-medium">
                    All Plans
                  </div>
                </div>
                <div className="flex flex-wrap gap-2">
                  {curatedReports.map((report) => {
                    const Icon = report.icon;
                    return (
                      <button
                        key={report.id}
                        onClick={() => {
                          setSelectedReport(report.id);
                          setViewingCustomReport(null);
                          setShowReportBuilder(false);
                        }}
                        className={`px-3 py-2 rounded-lg transition-colors flex items-center gap-2 text-sm ${
                          selectedReport === report.id && !viewingCustomReport
                            ? 'bg-blue-600 text-white'
                            : 'bg-white text-gray-700 hover:bg-blue-100 border border-blue-200'
                        }`}
                      >
                        <Icon className="w-4 h-4" />
                        {report.name}
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Custom Reports Card */}
              <div className="bg-gradient-to-br from-purple-50 to-pink-50 border-2 border-purple-200 rounded-xl p-4 md:p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <div className="w-12 h-12 bg-gradient-to-br from-purple-600 to-pink-600 rounded-lg flex items-center justify-center">
                      <Sparkles className="w-6 h-6 text-white" />
                    </div>
                    <div>
                      <h3 className="text-gray-900 font-medium">Custom Reports</h3>
                      <p className="text-sm text-gray-600">
                        {canAccessCustomReports 
                          ? `${savedReports.length}/${customReportLimit} reports used`
                          : 'Build reports your way'}
                      </p>
                    </div>
                  </div>
                  {!canAccessCustomReports ? (
                    <div className="px-2 py-1 bg-purple-100 text-purple-700 rounded text-xs font-medium flex items-center gap-1">
                      <Crown className="w-3 h-3" />
                      Pro+
                    </div>
                  ) : (
                    <div className="px-2 py-1 bg-green-100 text-green-700 rounded text-xs font-medium flex items-center gap-1">
                      <CheckCircle className="w-3 h-3" />
                      Active
                    </div>
                  )}
                </div>
                {canAccessCustomReports ? (
                  <div className="space-y-3">
                    {/* Progress bar for report usage */}
                    <div className="space-y-1">
                      <div className="h-2 bg-gray-200 rounded-full overflow-hidden">
                        <div 
                          className={`h-full transition-all duration-300 ${
                            hasReachedCustomReportLimit 
                              ? 'bg-red-500' 
                              : savedReports.length / customReportLimit >= 0.8
                              ? 'bg-amber-500'
                              : 'bg-gradient-to-r from-purple-500 to-pink-500'
                          }`}
                          style={{ width: `${Math.min((savedReports.length / customReportLimit) * 100, 100)}%` }}
                        />
                      </div>
                    </div>
                    <button
                      onClick={() => {
                        if (!hasReachedCustomReportLimit) {
                          setShowReportBuilder(true);
                          setViewingCustomReport(null);
                          setSelectedReport('');
                          resetBuilder();
                        }
                      }}
                      disabled={hasReachedCustomReportLimit}
                      className={`w-full px-4 py-2 rounded-lg transition-colors flex items-center justify-center gap-2 ${
                        hasReachedCustomReportLimit
                          ? 'bg-gray-300 text-gray-500 cursor-not-allowed'
                          : 'bg-gradient-to-r from-purple-600 to-pink-600 text-white hover:from-purple-700 hover:to-pink-700'
                      }`}
                    >
                      <Plus className="w-4 h-4" />
                      Create Custom Report
                    </button>
                    {hasReachedCustomReportLimit && currentPlan !== 'enterprise' && (
                      <button
                        onClick={() => onNavigate && onNavigate('billing')}
                        className="w-full px-3 py-2 bg-white border border-purple-300 text-purple-700 rounded-lg hover:bg-purple-50 transition-colors flex items-center justify-center gap-2 text-sm"
                      >
                        <Crown className="w-4 h-4" />
                        Upgrade for More
                      </button>
                    )}
                  </div>
                ) : (
                  <div className="space-y-2">
                    <button
                      onClick={() => onNavigate && onNavigate('billing')}
                      className="w-full px-4 py-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-colors flex items-center justify-center gap-2"
                    >
                      <Crown className="w-4 h-4" />
                      Upgrade to Unlock
                    </button>
                    <p className="text-xs text-center text-gray-500">
                      Professional: 10 reports • Enterprise: 50 reports
                    </p>
                  </div>
                )}
              </div>
            </div>

            {/* Report Limit Warning for Professional users near limit */}
            {canAccessCustomReports && hasReachedCustomReportLimit && currentPlan === 'professional' && (
              <div className="bg-purple-50 border border-purple-200 rounded-lg p-4 flex items-start gap-3">
                <AlertCircle className="w-5 h-5 text-purple-600 flex-shrink-0 mt-0.5" />
                <div className="flex-1">
                  <p className="text-sm text-purple-900">
                    <span className="font-medium">Custom Report Limit Reached:</span> You've used all {customReportLimit} custom reports. 
                  </p>
                  <button
                    onClick={() => onNavigate && onNavigate('billing')}
                    className="mt-2 text-sm text-purple-700 hover:text-purple-800 font-medium underline"
                  >
                    Upgrade to Enterprise for 50 custom reports
                  </button>
                </div>
              </div>
            )}

            {/* My Custom Reports List */}
            {canAccessCustomReports && savedReports.length > 0 && (
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-4">
                  <div className="flex items-center gap-3">
                    <h3 className="text-gray-900 font-medium">My Custom Reports</h3>
                    <span className={`px-2 py-1 rounded text-xs font-medium ${
                      hasReachedCustomReportLimit 
                        ? 'bg-red-100 text-red-700' 
                        : savedReports.length / customReportLimit >= 0.8
                        ? 'bg-amber-100 text-amber-700'
                        : 'bg-gray-100 text-gray-600'
                    }`}>
                      {savedReports.length}/{customReportLimit}
                    </span>
                  </div>
                </div>
                <div className="grid grid-cols-2 gap-3">
                  {savedReports.map((report) => {
                    const entity = entities.find(e => e.id === report.primaryEntity);
                    const Icon = entity?.icon || FileText;
                    const isActive = viewingCustomReport === report.id;
                    
                    return (
                      <div
                        key={report.id}
                        className={`border-2 rounded-lg p-4 transition-all ${
                          isActive
                            ? 'border-purple-400 bg-purple-50'
                            : 'border-gray-200 hover:border-purple-300 bg-white'
                        }`}
                      >
                        <div className="flex items-start justify-between mb-3">
                          <div className="flex items-center gap-3 flex-1 min-w-0">
                            <div className={`w-10 h-10 bg-${entity?.color || 'gray'}-100 rounded-lg flex items-center justify-center flex-shrink-0`}>
                              <Icon className={`w-5 h-5 text-${entity?.color || 'gray'}-600`} />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="text-gray-900 font-medium truncate">{report.name}</h4>
                              <p className="text-xs text-gray-500">
                                {entity?.name}
                                {report.joinedEntities.length > 0 && ` +${report.joinedEntities.length}`}
                              </p>
                            </div>
                          </div>
                        </div>
                        
                        <div className="space-y-2 mb-3">
                          <div className="flex items-center gap-2 text-xs text-gray-600">
                            <BarChart3 className="w-3 h-3" />
                            <span>{report.fields.length} fields, {report.metrics.length} metrics</span>
                          </div>
                          {report.filters.length > 0 && (
                            <div className="flex items-center gap-2 text-xs text-gray-600">
                              <Filter className="w-3 h-3" />
                              <span>{report.filters.length} filter{report.filters.length > 1 ? 's' : ''}</span>
                            </div>
                          )}
                          <div className="flex items-center gap-2 text-xs text-gray-500">
                            <Clock className="w-3 h-3" />
                            <span>Last run: {report.lastRun}</span>
                          </div>
                        </div>

                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              setViewingCustomReport(report.id);
                              setSelectedReport('');
                            }}
                            className={`flex-1 px-3 py-1.5 rounded text-sm transition-colors ${
                              isActive
                                ? 'bg-purple-600 text-white'
                                : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
                            }`}
                          >
                            View Report
                          </button>
                          <button
                            onClick={() => editCustomReport(report)}
                            className="p-1.5 hover:bg-gray-100 rounded transition-colors"
                            title="Edit report"
                          >
                            <Settings className="w-4 h-4 text-gray-600" />
                          </button>
                          <button
                            onClick={() => deleteCustomReport(report.id)}
                            className="p-1.5 hover:bg-red-50 rounded transition-colors"
                            title="Delete report"
                          >
                            <X className="w-4 h-4 text-red-600" />
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>
              </div>
            )}
          </div>
        )}
      </div>

      {/* Custom Report Builder */}
      {showReportBuilder && canAccessCustomReports && (
        <div className="mb-6">
          {/* Builder Header */}
          <div className="bg-white rounded-xl border border-gray-200 p-6 mb-6">
            <div className="flex items-center justify-between mb-6">
              <div className="flex items-center gap-3">
                <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
                  <Settings className="w-5 h-5 text-purple-600" />
                </div>
                <div>
                  <h3 className="text-gray-900">Custom Report Builder</h3>
                  <p className="text-sm text-gray-600">Build advanced reports with multiple data sources</p>
                </div>
              </div>
              <button
                onClick={() => setShowReportBuilder(false)}
                className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
              >
                <X className="w-5 h-5 text-gray-500" />
              </button>
            </div>

            {/* Progress Steps */}
            <div className="flex items-center gap-1 md:gap-2 overflow-x-auto">
              {[
                { num: 1, label: 'Choose Data' },
                { num: 2, label: 'Select Fields' },
                { num: 3, label: 'Configure' },
                { num: 4, label: 'Visualize' },
              ].map((step, idx) => (
                <div key={step.num} className="flex items-center flex-1 min-w-0">
                  <div className="flex items-center gap-1 md:gap-2 flex-1 min-w-0">
                    <div
                      className={`w-7 h-7 md:w-8 md:h-8 rounded-full flex items-center justify-center text-xs md:text-sm font-medium transition-colors flex-shrink-0 ${
                        builderStep >= step.num
                          ? 'bg-purple-600 text-white'
                          : 'bg-gray-200 text-gray-500'
                      }`}
                    >
                      {builderStep > step.num ? (
                        <CheckCircle className="w-4 h-4 md:w-5 md:h-5" />
                      ) : (
                        step.num
                      )}
                    </div>
                    <span
                      className={`text-xs md:text-sm font-medium truncate ${
                        builderStep >= step.num ? 'text-gray-900' : 'text-gray-500'
                      }`}
                    >
                      {step.label}
                    </span>
                  </div>
                  {idx < 3 && (
                    <ChevronRight
                      className={`w-4 h-4 mx-2 ${
                        builderStep > step.num ? 'text-purple-600' : 'text-gray-300'
                      }`}
                    />
                  )}
                </div>
              ))}
            </div>
          </div>

          {/* Step 1: Choose Primary Entity and Join Others */}
          {builderStep === 1 && (
            <div className="space-y-6">
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h4 className="text-gray-900 mb-2">Step 1: Choose your primary data source</h4>
                <p className="text-sm text-gray-600 mb-4">Select the main entity you want to analyze</p>
                <div className="grid grid-cols-3 gap-4">
                  {entities.map((entity) => {
                    const Icon = entity.icon;
                    return (
                      <button
                        key={entity.id}
                        onClick={() => {
                          setPrimaryEntity(entity.id);
                          setJoinedEntities([]);
                          setSelectedFields([]);
                          setFilters([]);
                        }}
                        className={`p-5 border-2 rounded-xl hover:border-purple-400 hover:bg-purple-50 transition-all text-left ${
                          primaryEntity === entity.id
                            ? 'border-purple-400 bg-purple-50'
                            : 'border-gray-300'
                        }`}
                      >
                        <div className={`w-12 h-12 bg-${entity.color}-100 rounded-lg flex items-center justify-center mb-3`}>
                          <Icon className={`w-6 h-6 text-${entity.color}-600`} />
                        </div>
                        <div className="text-gray-900 font-medium mb-1">{entity.name}</div>
                        <div className="text-sm text-gray-500 mb-3">{entity.description}</div>
                        <div className="text-xs text-gray-500 flex items-center gap-1">
                          <FileText className="w-3 h-3" />
                          {entity.fields.length} fields available
                        </div>
                      </button>
                    );
                  })}
                </div>
              </div>

              {/* Join Additional Entities */}
              {primaryEntity && getJoinableEntities().length > 0 && (
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <div className="flex items-start gap-3 mb-4">
                    <div className="w-10 h-10 bg-indigo-100 rounded-lg flex items-center justify-center flex-shrink-0">
                      <Link2 className="w-5 h-5 text-indigo-600" />
                    </div>
                    <div className="flex-1">
                      <h4 className="text-gray-900 mb-1">Join additional data sources (Optional)</h4>
                      <p className="text-sm text-gray-600">
                        Combine related data to create richer reports. Join relationships are based on your database structure.
                      </p>
                    </div>
                  </div>
                  
                  <div className="space-y-3">
                    {getJoinableEntities().map((item) => {
                      if (!item) return null;
                      const Icon = item.icon;
                      const isJoined = isEntityJoined(item.id);
                      const joinInfo = item.joinInfo;
                      
                      return (
                        <button
                          key={item.id}
                          onClick={() => toggleJoinEntity(item.id, joinInfo)}
                          className={`w-full p-4 border-2 rounded-lg transition-all text-left ${
                            isJoined
                              ? 'border-indigo-400 bg-indigo-50'
                              : 'border-gray-300 hover:border-indigo-300 hover:bg-gray-50'
                          }`}
                        >
                          <div className="flex items-start justify-between mb-2">
                            <div className="flex items-center gap-3">
                              <div className={`w-10 h-10 bg-${item.color}-100 rounded-lg flex items-center justify-center`}>
                                <Icon className={`w-5 h-5 text-${item.color}-600`} />
                              </div>
                              <div>
                                <div className="text-gray-900 font-medium">{item.name}</div>
                                <div className="text-xs text-gray-500">{item.fields.length} fields available</div>
                              </div>
                            </div>
                            {isJoined && (
                              <CheckCircle className="w-5 h-5 text-indigo-600" />
                            )}
                          </div>
                          
                          <div className="ml-13 space-y-1">
                            <div className="flex items-center gap-2 text-xs text-gray-600">
                              <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                              <span className="font-medium">Relationship:</span>
                              <span>{joinInfo.description}</span>
                            </div>
                            {joinInfo.type === 'many-to-one' && (
                              <div className="flex items-center gap-2 text-xs text-gray-600">
                                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                                <span className="font-mono bg-gray-100 px-2 py-0.5 rounded">
                                  {getPrimaryEntity()?.name}.{joinInfo.fromField} = {item.name}.{joinInfo.toField}
                                </span>
                              </div>
                            )}
                            {joinInfo.type === 'one-to-many' && (
                              <div className="flex items-center gap-2 text-xs text-gray-600">
                                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                                <span className="font-mono bg-gray-100 px-2 py-0.5 rounded">
                                  {getPrimaryEntity()?.name}.{joinInfo.fromField} = {item.name}.{joinInfo.toField}
                                </span>
                              </div>
                            )}
                            {joinInfo.type === 'many-to-many' && (
                              <div className="flex items-center gap-2 text-xs text-gray-600">
                                <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                                <span className="font-mono bg-gray-100 px-2 py-0.5 rounded">
                                  via {joinInfo.via} table
                                </span>
                              </div>
                            )}
                            <div className="flex items-center gap-2 text-xs">
                              <div className="w-1 h-1 bg-gray-400 rounded-full"></div>
                              <span className={`px-2 py-0.5 rounded ${
                                joinInfo.type === 'many-to-one' ? 'bg-blue-100 text-blue-700' :
                                joinInfo.type === 'one-to-many' ? 'bg-green-100 text-green-700' :
                                'bg-purple-100 text-purple-700'
                              }`}>
                                {joinInfo.type}
                              </span>
                            </div>
                          </div>
                        </button>
                      );
                    })}
                  </div>

                  {joinedEntities.length > 0 && (
                    <div className="mt-4 p-4 bg-indigo-50 border border-indigo-200 rounded-lg">
                      <div className="flex items-start gap-2 text-sm text-indigo-900">
                        <Layers className="w-5 h-5 flex-shrink-0 mt-0.5" />
                        <div>
                          <div className="font-medium mb-1">
                            {joinedEntities.length + 1} data source{joinedEntities.length > 0 ? 's' : ''} selected
                          </div>
                          <div className="text-xs text-indigo-700 space-y-0.5">
                            {getActiveJoins().map((join, idx) => (
                              <div key={idx} className="flex items-center gap-1">
                                <ChevronRight className="w-3 h-3" />
                                {join.relationship?.label || `${getPrimaryEntity()?.name} → ${join.entity?.name}`}
                              </div>
                            ))}
                          </div>
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              )}

              <div className="flex justify-end">
                <button
                  onClick={() => primaryEntity && setBuilderStep(2)}
                  disabled={!primaryEntity}
                  className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  Continue
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* Step 2: Select Fields from All Entities */}
          {builderStep === 2 && primaryEntity && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <div className="mb-4">
                <h4 className="text-gray-900 mb-2">Step 2: Select fields from your data sources</h4>
                <div className="bg-blue-50 border border-blue-200 rounded-lg p-3 mb-4">
                  <div className="flex items-start gap-2">
                    <div className="w-5 h-5 bg-blue-600 rounded flex items-center justify-center flex-shrink-0 mt-0.5">
                      <span className="text-white text-xs">💡</span>
                    </div>
                    <div className="text-sm text-gray-700">
                      <strong>What are fields?</strong> Fields are the data columns that will appear in your report. 
                      Select fields from any of your chosen data sources.
                    </div>
                  </div>
                </div>
                <div className="flex items-center justify-between">
                  <p className="text-sm text-gray-600">
                    {selectedFields.length} field{selectedFields.length !== 1 ? 's' : ''} selected
                  </p>
                  {selectedFields.length > 0 && (
                    <button
                      onClick={() => setSelectedFields([])}
                      className="text-sm text-purple-600 hover:text-purple-700 font-medium"
                    >
                      Clear all
                    </button>
                  )}
                </div>
              </div>

              {/* Fields grouped by entity */}
              <div className="space-y-6">
                {getAllSelectedEntities().map((entity) => {
                  const Icon = entity.icon;
                  const entityFieldCount = selectedFields.filter(sf => sf.entityId === entity.id).length;
                  
                  return (
                    <div key={entity.id} className="border border-gray-200 rounded-lg p-4">
                      <div className="flex items-center gap-3 mb-4">
                        <div className={`w-8 h-8 bg-${entity.color}-100 rounded-lg flex items-center justify-center`}>
                          <Icon className={`w-4 h-4 text-${entity.color}-600`} />
                        </div>
                        <div className="flex-1">
                          <h5 className="text-gray-900 font-medium">{entity.name}</h5>
                          <p className="text-xs text-gray-500">{entityFieldCount} of {entity.fields.length} fields selected</p>
                        </div>
                      </div>
                      <div className="grid grid-cols-4 gap-2">
                        {entity.fields.map((field) => {
                          const selected = isFieldSelected(entity.id, field.id);
                          return (
                            <button
                              key={field.id}
                              onClick={() => toggleField(entity.id, field.id)}
                              className={`px-3 py-2 border-2 rounded-lg transition-all text-sm text-left ${
                                selected
                                  ? 'border-purple-400 bg-purple-50 text-purple-900'
                                  : 'border-gray-300 text-gray-700 hover:border-purple-300'
                              }`}
                            >
                              <div className="font-medium truncate">{field.name}</div>
                              <div className="text-xs text-gray-500 mt-0.5">{field.type}</div>
                            </button>
                          );
                        })}
                      </div>
                    </div>
                  );
                })}
              </div>

              <div className="flex justify-between mt-6">
                <button
                  onClick={() => setBuilderStep(1)}
                  className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-gray-700"
                >
                  Back
                </button>
                <button
                  onClick={() => selectedFields.length > 0 && setBuilderStep(3)}
                  disabled={selectedFields.length === 0}
                  className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  Continue
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* Step 3: Configure Metrics and Filters */}
          {builderStep === 3 && primaryEntity && (
            <div className="space-y-6">
              {/* Selected Fields Summary */}
              <div className="bg-blue-50 border border-blue-200 rounded-xl p-4">
                <div className="flex items-start gap-3">
                  <div className="w-8 h-8 bg-blue-600 rounded-lg flex items-center justify-center flex-shrink-0">
                    <CheckCircle className="w-5 h-5 text-white" />
                  </div>
                  <div className="flex-1">
                    <h4 className="text-gray-900 font-medium mb-1">Selected Fields ({selectedFields.length})</h4>
                    <p className="text-sm text-gray-600 mb-3">These fields will be the data columns in your report</p>
                    <div className="flex flex-wrap gap-2">
                      {selectedFields.map(sf => {
                        const entity = entities.find(e => e.id === sf.entityId);
                        const field = entity?.fields.find(f => f.id === sf.fieldId);
                        return field && entity ? (
                          <div key={`${sf.entityId}-${sf.fieldId}`} className="px-3 py-1 bg-white border border-blue-300 rounded-lg text-sm text-gray-700 flex items-center gap-2">
                            <span className="text-xs text-gray-500">{entity.name}:</span>
                            <span>{field.name}</span>
                          </div>
                        ) : null;
                      })}
                    </div>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h4 className="text-gray-900 mb-4">Step 3: Configure your report</h4>
                
                {/* Report Name */}
                <div className="mb-6">
                  <label className="block text-gray-700 mb-2 text-sm font-medium">Report Name</label>
                  <input
                    type="text"
                    value={reportName}
                    onChange={(e) => setReportName(e.target.value)}
                    placeholder={`e.g., ${getPrimaryEntity()?.name} Analysis Report`}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent"
                  />
                </div>

                {/* Metrics */}
                <div className="mb-6">
                  <label className="block text-gray-700 mb-3 text-sm font-medium">Metrics to Calculate</label>
                  <div className="flex flex-wrap gap-2">
                    {metricOptions.map((metric) => (
                      <button
                        key={metric}
                        onClick={() => {
                          if (selectedMetrics.includes(metric)) {
                            setSelectedMetrics(selectedMetrics.filter(m => m !== metric));
                          } else {
                            setSelectedMetrics([...selectedMetrics, metric]);
                          }
                        }}
                        className={`px-3 py-2 border-2 rounded-lg transition-colors text-sm ${
                          selectedMetrics.includes(metric)
                            ? 'border-purple-400 bg-purple-50 text-purple-900'
                            : 'border-gray-300 hover:border-purple-300'
                        }`}
                      >
                        {metric}
                      </button>
                    ))}
                  </div>
                </div>

                {/* Group By */}
                <div className="mb-6">
                  <label className="block text-gray-700 mb-2 text-sm font-medium">Group Results By</label>
                  <select
                    value={selectedGroupBy}
                    onChange={(e) => setSelectedGroupBy(e.target.value)}
                    className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-purple-500 focus:border-transparent bg-white"
                  >
                    <option value="">No grouping</option>
                    <option value="day">Day</option>
                    <option value="week">Week</option>
                    <option value="month">Month</option>
                    {selectedFields.map(sf => {
                      const entity = entities.find(e => e.id === sf.entityId);
                      const field = entity?.fields.find(f => f.id === sf.fieldId);
                      if (field && ['select', 'text'].includes(field.type)) {
                        return (
                          <option key={`${sf.entityId}-${sf.fieldId}`} value={`${sf.entityId}.${sf.fieldId}`}>
                            {entity?.name} - {field.name}
                          </option>
                        );
                      }
                      return null;
                    })}
                  </select>
                </div>

                {/* Filters */}
                <div>
                  <div className="flex items-center justify-between mb-3">
                    <label className="block text-gray-700 text-sm font-medium">
                      Filters (Optional)
                    </label>
                    <div className="relative">
                      <button
                        onClick={() => setShowFilterDropdown(!showFilterDropdown)}
                        className="px-3 py-1.5 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2 text-sm"
                      >
                        <Plus className="w-4 h-4" />
                        Add Filter
                      </button>

                      {showFilterDropdown && (
                        <>
                          <div 
                            className="fixed inset-0 z-10" 
                            onClick={() => setShowFilterDropdown(false)}
                          />
                          <div className="absolute right-0 mt-2 w-72 bg-white rounded-lg border border-gray-200 shadow-lg z-20 max-h-96 overflow-y-auto">
                            <div className="p-2">
                              <div className="px-3 py-2 text-xs font-medium text-gray-500 uppercase">
                                Select field to filter
                              </div>
                              {getFilterableFields().map((field) => (
                                <button
                                  key={`${field.entityId}-${field.id}`}
                                  onClick={() => addFilter(field)}
                                  className="w-full text-left px-3 py-2 hover:bg-purple-50 rounded-lg transition-colors text-sm"
                                >
                                  <div className="font-medium text-gray-900">{field.displayName}</div>
                                  <div className="text-xs text-gray-500">{field.type}</div>
                                </button>
                              ))}
                            </div>
                          </div>
                        </>
                      )}
                    </div>
                  </div>

                  {filters.length > 0 ? (
                    <div className="space-y-2 bg-gray-50 rounded-lg p-3">
                      {filters.map((filter) => (
                        <div key={filter.id} className="flex items-center gap-2 bg-white p-3 rounded-lg border border-gray-200">
                          <span className="text-sm text-gray-700 min-w-[140px] font-medium truncate" title={filter.label}>
                            {filter.label}
                          </span>
                          
                          <select
                            value={filter.operator}
                            onChange={(e) => updateFilter(filter.id, 'operator', e.target.value)}
                            className="px-2 py-1 border border-gray-300 rounded text-sm bg-white"
                          >
                            {filter.type === 'number' ? (
                              <>
                                <option value="equals">equals</option>
                                <option value="not_equals">not equals</option>
                                <option value="greater_than">greater than</option>
                                <option value="less_than">less than</option>
                              </>
                            ) : filter.type === 'date' ? (
                              <>
                                <option value="equals">on</option>
                                <option value="between">between</option>
                                <option value="after">after</option>
                                <option value="before">before</option>
                              </>
                            ) : (
                              <>
                                <option value="equals">equals</option>
                                <option value="not_equals">not equals</option>
                              </>
                            )}
                          </select>

                          {filter.type === 'select' ? (
                            <select
                              value={filter.value}
                              onChange={(e) => updateFilter(filter.id, 'value', e.target.value)}
                              className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm bg-white"
                            >
                              <option value="">Select...</option>
                              {filter.options?.map((opt: string) => (
                                <option key={opt} value={opt}>{opt}</option>
                              ))}
                            </select>
                          ) : filter.type === 'boolean' ? (
                            <select
                              value={filter.value}
                              onChange={(e) => updateFilter(filter.id, 'value', e.target.value)}
                              className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm bg-white"
                            >
                              <option value="">Select...</option>
                              <option value="true">Yes</option>
                              <option value="false">No</option>
                            </select>
                          ) : filter.type === 'date' ? (
                            <div className="flex-1 flex items-center gap-2">
                              <input
                                type="date"
                                value={filter.value}
                                onChange={(e) => updateFilter(filter.id, 'value', e.target.value)}
                                className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm"
                              />
                              {filter.operator === 'between' && (
                                <>
                                  <span className="text-gray-500 text-sm">to</span>
                                  <input
                                    type="date"
                                    value={filter.valueTo || ''}
                                    onChange={(e) => updateFilter(filter.id, 'valueTo', e.target.value)}
                                    className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm"
                                  />
                                </>
                              )}
                            </div>
                          ) : filter.type === 'number' ? (
                            <input
                              type="number"
                              value={filter.value}
                              onChange={(e) => updateFilter(filter.id, 'value', e.target.value)}
                              placeholder="Enter number..."
                              className="flex-1 px-2 py-1 border border-gray-300 rounded text-sm"
                            />
                          ) : null}

                          <button
                            onClick={() => removeFilter(filter.id)}
                            className="p-1 hover:bg-gray-100 rounded transition-colors"
                          >
                            <X className="w-4 h-4 text-gray-500" />
                          </button>
                        </div>
                      ))}
                    </div>
                  ) : (
                    <p className="text-sm text-gray-500 bg-gray-50 rounded-lg p-3">
                      No filters applied. Add filters to narrow down your results based on any field from your selected data sources.
                    </p>
                  )}
                </div>
              </div>

              <div className="flex justify-between">
                <button
                  onClick={() => setBuilderStep(2)}
                  className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-gray-700"
                >
                  Back
                </button>
                <button
                  onClick={() => setBuilderStep(4)}
                  className="px-6 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors flex items-center gap-2"
                >
                  Continue
                  <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}

          {/* Step 4: Visualization */}
          {builderStep === 4 && primaryEntity && (
            <div className="space-y-6">
              {/* Report Summary */}
              <div className="bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200 rounded-xl p-6">
                <h4 className="text-gray-900 font-medium mb-4 flex items-center gap-2">
                  <Sparkles className="w-5 h-5 text-purple-600" />
                  Your Report Summary
                </h4>
                <div className="grid grid-cols-2 gap-6">
                  <div className="col-span-2">
                    <div className="text-sm text-gray-600 mb-2">Data Sources & Joins</div>
                    <div className="space-y-1">
                      <div className="text-gray-900 font-medium">
                        {getPrimaryEntity()?.name} (Primary)
                      </div>
                      {getActiveJoins().map((join, idx) => (
                        <div key={idx} className="text-sm text-gray-700 flex items-center gap-2">
                          <ChevronRight className="w-3 h-3 text-gray-400" />
                          {join.entity?.name}
                          <span className="text-xs text-gray-500">
                            ({join.relationship?.type})
                          </span>
                        </div>
                      ))}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-2">Report Name</div>
                    <div className="text-gray-900 font-medium">{reportName || 'Untitled Report'}</div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-2">Data Columns ({selectedFields.length})</div>
                    <div className="flex flex-wrap gap-1">
                      {selectedFields.slice(0, 3).map(sf => {
                        const entity = entities.find(e => e.id === sf.entityId);
                        const field = entity?.fields.find(f => f.id === sf.fieldId);
                        return field ? (
                          <span key={`${sf.entityId}-${sf.fieldId}`} className="text-xs px-2 py-1 bg-white border border-purple-200 rounded text-gray-700">
                            {field.name}
                          </span>
                        ) : null;
                      })}
                      {selectedFields.length > 3 && (
                        <span className="text-xs px-2 py-1 bg-white border border-purple-200 rounded text-gray-600">
                          +{selectedFields.length - 3} more
                        </span>
                      )}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-2">Metrics ({selectedMetrics.length})</div>
                    <div className="flex flex-wrap gap-1">
                      {selectedMetrics.length > 0 ? (
                        selectedMetrics.slice(0, 3).map(metric => (
                          <span key={metric} className="text-xs px-2 py-1 bg-white border border-purple-200 rounded text-gray-700">
                            {metric}
                          </span>
                        ))
                      ) : (
                        <span className="text-xs text-gray-500">None selected</span>
                      )}
                      {selectedMetrics.length > 3 && (
                        <span className="text-xs px-2 py-1 bg-white border border-purple-200 rounded text-gray-600">
                          +{selectedMetrics.length - 3} more
                        </span>
                      )}
                    </div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-2">Group By</div>
                    <div className="text-gray-900 font-medium">{selectedGroupBy || 'None'}</div>
                  </div>
                  <div>
                    <div className="text-sm text-gray-600 mb-2">Filters</div>
                    <div className="text-gray-900 font-medium">{filters.length} active</div>
                  </div>
                </div>
              </div>

              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h4 className="text-gray-900 mb-2">Step 4: Choose visualization type</h4>
                <p className="text-sm text-gray-600 mb-4">Select how you want to display your {selectedFields.length} data columns</p>
                <div className="grid grid-cols-3 gap-4 mb-6">
                  {[
                    { type: 'bar', icon: BarChart3, label: 'Bar Chart', description: 'Compare values across categories' },
                    { type: 'line', icon: LineChart, label: 'Line Chart', description: 'Show trends over time' },
                    { type: 'pie', icon: PieChart, label: 'Pie Chart', description: 'Display proportions' }
                  ].map((chart) => {
                    const Icon = chart.icon;
                    return (
                      <button
                        key={chart.type}
                        onClick={() => setSelectedChartType(chart.type)}
                        className={`p-5 border-2 rounded-xl transition-all text-center ${
                          selectedChartType === chart.type
                            ? 'border-purple-400 bg-purple-50'
                            : 'border-gray-300 hover:border-purple-300'
                        }`}
                      >
                        <Icon className="w-8 h-8 text-gray-600 mx-auto mb-3" />
                        <div className="text-gray-900 font-medium mb-1">{chart.label}</div>
                        <div className="text-xs text-gray-500">{chart.description}</div>
                      </button>
                    );
                  })}
                </div>

                {/* Report Preview */}
                <div className="bg-gray-50 rounded-lg p-4 border border-gray-200">
                  <div className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
                    <FileText className="w-4 h-4" />
                    Report Preview
                  </div>
                  <div className="bg-white rounded border border-gray-300 p-4">
                    <div className="text-xs text-gray-500 mb-2">Your report will include:</div>
                    <div className="space-y-1 text-sm">
                      <div className="flex items-start gap-2">
                        <div className="w-1.5 h-1.5 bg-purple-600 rounded-full mt-1.5 flex-shrink-0"></div>
                        <span className="text-gray-700">
                          <strong>{selectedFields.length} columns</strong> from {getAllSelectedEntities().length} data source{getAllSelectedEntities().length > 1 ? 's' : ''}
                        </span>
                      </div>
                      {selectedMetrics.length > 0 && (
                        <div className="flex items-start gap-2">
                          <div className="w-1.5 h-1.5 bg-purple-600 rounded-full mt-1.5 flex-shrink-0"></div>
                          <span className="text-gray-700"><strong>{selectedMetrics.length} calculations:</strong> {selectedMetrics.join(', ')}</span>
                        </div>
                      )}
                      {selectedGroupBy && (
                        <div className="flex items-start gap-2">
                          <div className="w-1.5 h-1.5 bg-purple-600 rounded-full mt-1.5 flex-shrink-0"></div>
                          <span className="text-gray-700"><strong>Grouped by:</strong> {selectedGroupBy}</span>
                        </div>
                      )}
                      {filters.length > 0 && (
                        <div className="flex items-start gap-2">
                          <div className="w-1.5 h-1.5 bg-purple-600 rounded-full mt-1.5 flex-shrink-0"></div>
                          <span className="text-gray-700"><strong>Filtered by:</strong> {filters.length} condition{filters.length > 1 ? 's' : ''} across all data sources</span>
                        </div>
                      )}
                      <div className="flex items-start gap-2">
                        <div className="w-1.5 h-1.5 bg-purple-600 rounded-full mt-1.5 flex-shrink-0"></div>
                        <span className="text-gray-700"><strong>Visualization:</strong> {selectedChartType === 'bar' ? 'Bar Chart' : selectedChartType === 'line' ? 'Line Chart' : 'Pie Chart'}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>

              <div className="flex justify-between">
                <button
                  onClick={() => setBuilderStep(3)}
                  className="px-6 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-gray-700"
                >
                  Back
                </button>
                <div className="flex items-center gap-3">
                  <button 
                    onClick={generateReport}
                    className="px-6 py-2 bg-gradient-to-r from-purple-600 to-pink-600 text-white rounded-lg hover:from-purple-700 hover:to-pink-700 transition-colors flex items-center gap-2"
                  >
                    <Play className="w-4 h-4" />
                    Generate & Save Report
                  </button>
                </div>
              </div>
            </div>
          )}
        </div>
      )}

      {/* Report Content */}
      {!showReportBuilder && (
        <div className="space-y-6">
          {/* Custom Report View */}
          {viewingCustomReport && getCurrentReport() && (
            <>
              {/* Custom Report Header */}
              <div className="bg-gradient-to-br from-purple-50 to-pink-50 border border-purple-200 rounded-xl p-6">
                <div className="flex items-start justify-between mb-4">
                  <div className="flex items-center gap-3 flex-1">
                    <div className="w-12 h-12 bg-gradient-to-br from-purple-600 to-pink-600 rounded-lg flex items-center justify-center">
                      <Sparkles className="w-6 h-6 text-white" />
                    </div>
                    <div>
                      <h3 className="text-gray-900 font-medium mb-1">{getCurrentReport()?.name}</h3>
                      <p className="text-sm text-gray-600">Custom Report</p>
                    </div>
                  </div>
                  <button
                    onClick={() => editCustomReport(getCurrentReport()!)}
                    className="px-4 py-2 bg-white border border-purple-300 rounded-lg hover:bg-purple-50 transition-colors flex items-center gap-2 text-sm text-gray-700"
                  >
                    <Settings className="w-4 h-4" />
                    Edit Report
                  </button>
                </div>

                {/* Report Configuration Summary */}
                <div className="grid grid-cols-4 gap-4">
                  <div className="bg-white rounded-lg p-3">
                    <div className="text-xs text-gray-600 mb-1">Data Sources</div>
                    <div className="text-sm text-gray-900 font-medium">
                      {getCurrentReport()?.joinedEntities.length! + 1} tables
                    </div>
                  </div>
                  <div className="bg-white rounded-lg p-3">
                    <div className="text-xs text-gray-600 mb-1">Fields</div>
                    <div className="text-sm text-gray-900 font-medium">
                      {getCurrentReport()?.fields.length} columns
                    </div>
                  </div>
                  <div className="bg-white rounded-lg p-3">
                    <div className="text-xs text-gray-600 mb-1">Metrics</div>
                    <div className="text-sm text-gray-900 font-medium">
                      {getCurrentReport()?.metrics.length || 0} calculations
                    </div>
                  </div>
                  <div className="bg-white rounded-lg p-3">
                    <div className="text-xs text-gray-600 mb-1">Filters</div>
                    <div className="text-sm text-gray-900 font-medium">
                      {getCurrentReport()?.filters.length} active
                    </div>
                  </div>
                </div>
              </div>

              {/* Custom Report Results */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <div className="flex items-center justify-between mb-4">
                  <h3 className="text-gray-900">Report Results</h3>
                  <button className="px-3 py-1.5 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2 text-sm">
                    <Download className="w-4 h-4" />
                    Export
                  </button>
                </div>

                {/* Mock visualization based on chart type */}
                {getCurrentReport()?.chartType === 'bar' && (
                  <div className="space-y-4">
                    <div className="h-64 flex items-end justify-between gap-3">
                      {[
                        { label: 'VIP', value: 85, color: 'bg-purple-500' },
                        { label: 'Customer', value: 92, color: 'bg-blue-500' },
                        { label: 'Lead', value: 78, color: 'bg-green-500' },
                        { label: 'Churned', value: 45, color: 'bg-red-500' },
                      ].map((item, idx) => {
                        const height = (item.value / 100) * 100;
                        return (
                          <div key={idx} className="flex-1 flex flex-col items-center gap-2 group">
                            <div 
                              className={`w-full ${item.color} rounded-t group-hover:opacity-80 transition-all cursor-pointer relative`}
                              style={{ height: `${height}%` }}
                            >
                              <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-900 text-white px-2 py-1 rounded text-xs opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                                {item.value}% avg
                              </div>
                            </div>
                            <span className="text-gray-700 text-sm font-medium">{item.label}</span>
                          </div>
                        );
                      })}
                    </div>
                  </div>
                )}

                {getCurrentReport()?.chartType === 'line' && (
                  <div className="h-64 flex items-end justify-between gap-2">
                    {['Mon', 'Tue', 'Wed', 'Thu', 'Fri', 'Sat', 'Sun'].map((day, idx) => {
                      const heights = [65, 72, 68, 78, 85, 80, 75];
                      return (
                        <div key={idx} className="flex-1 flex flex-col items-center gap-2">
                          <div className="w-full relative" style={{ height: '100%' }}>
                            <div 
                              className="absolute bottom-0 w-full bg-gradient-to-t from-purple-500 to-blue-500 rounded-t"
                              style={{ height: `${heights[idx]}%` }}
                            />
                          </div>
                          <span className="text-gray-500 text-xs">{day}</span>
                        </div>
                      );
                    })}
                  </div>
                )}

                {getCurrentReport()?.chartType === 'pie' && (
                  <div className="flex items-center justify-center py-8">
                    <div className="text-center">
                      <div className="w-48 h-48 mx-auto mb-6 rounded-full bg-gradient-to-br from-purple-500 via-blue-500 to-pink-500 flex items-center justify-center">
                        <div className="w-32 h-32 bg-white rounded-full flex items-center justify-center">
                          <div className="text-center">
                            <div className="text-3xl text-gray-900 mb-1">87%</div>
                            <div className="text-xs text-gray-600">Total</div>
                          </div>
                        </div>
                      </div>
                      <div className="flex items-center justify-center gap-4 text-sm">
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 bg-purple-500 rounded"></div>
                          <span className="text-gray-700">Category A</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 bg-blue-500 rounded"></div>
                          <span className="text-gray-700">Category B</span>
                        </div>
                        <div className="flex items-center gap-2">
                          <div className="w-3 h-3 bg-pink-500 rounded"></div>
                          <span className="text-gray-700">Category C</span>
                        </div>
                      </div>
                    </div>
                  </div>
                )}

                {/* Data Table Preview */}
                <div className="mt-6 border-t border-gray-200 pt-6">
                  <h4 className="text-gray-900 font-medium mb-3">Data Preview</h4>
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b border-gray-200">
                          {getCurrentReport()?.fields.slice(0, 4).map((field: any, idx: number) => {
                            const entity = entities.find(e => e.id === field.entityId);
                            const fieldDef = entity?.fields.find(f => f.id === field.fieldId);
                            return (
                              <th key={idx} className="text-left py-2 px-3 text-gray-700 font-medium">
                                {fieldDef?.name}
                              </th>
                            );
                          })}
                        </tr>
                      </thead>
                      <tbody>
                        {[1, 2, 3, 4, 5].map((row) => (
                          <tr key={row} className="border-b border-gray-100 hover:bg-gray-50">
                            {getCurrentReport()?.fields.slice(0, 4).map((field: any, idx: number) => (
                              <td key={idx} className="py-2 px-3 text-gray-600">
                                {idx === 0 ? `Sample ${row}` : idx === 1 ? `Value ${row}` : idx === 2 ? `${80 + row}%` : `Data ${row}`}
                              </td>
                            ))}
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Curated Reports */}
          {selectedReport === 'overview' && !viewingCustomReport && (
            <>
              {/* Conversations by Channel */}
              <div className="bg-white rounded-xl border border-gray-200 p-6">
                <h3 className="text-gray-900 mb-4">Conversations by Channel</h3>
                <div className="space-y-3">
                  {overviewData.conversationsByChannel.map((item, idx) => (
                    <div key={idx}>
                      <div className="flex items-center justify-between text-sm mb-2">
                        <span className="text-gray-700">{item.channel}</span>
                        <span className="text-gray-900 font-medium">{item.count} conversations</span>
                      </div>
                      <div className="w-full bg-gray-200 rounded-full h-3">
                        <div 
                          className={`${item.color} h-3 rounded-full transition-all duration-500`}
                          style={{ width: `${(item.count / 542) * 100}%` }}
                        />
                      </div>
                    </div>
                  ))}
                </div>
              </div>

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Response Time Trend */}
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-gray-900 mb-4">Response Time by Day</h3>
                  <div className="h-64 flex items-end justify-between gap-2">
                    {overviewData.responseTimeByDay.map((item, idx) => {
                      const maxTime = Math.max(...overviewData.responseTimeByDay.map(d => d.time));
                      const height = (item.time / maxTime) * 100;
                      return (
                        <div key={idx} className="flex-1 flex flex-col items-center gap-2 group">
                          <div 
                            className="w-full bg-gradient-to-t from-purple-500 to-blue-500 rounded-t group-hover:from-purple-600 group-hover:to-blue-600 transition-all cursor-pointer relative" 
                            style={{ height: `${height}%` }}
                          >
                            <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-gray-900 text-white px-2 py-1 rounded text-xs opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap">
                              {item.time}s avg
                            </div>
                          </div>
                          <span className="text-gray-500 text-xs">{item.day}</span>
                        </div>
                      );
                    })}
                  </div>
                </div>

                {/* Resolution Status */}
                <div className="bg-white rounded-xl border border-gray-200 p-6">
                  <h3 className="text-gray-900 mb-4">Resolution Status</h3>
                  <div className="space-y-3 mb-6">
                    {overviewData.resolutionRate.map((item, idx) => (
                      <div key={idx}>
                        <div className="flex items-center justify-between text-sm mb-2">
                          <span className="text-gray-700">{item.status}</span>
                          <span className="text-gray-900 font-medium">{item.percentage}%</span>
                        </div>
                        <div className="w-full bg-gray-200 rounded-full h-2">
                          <div 
                            className={`${item.color} h-2 rounded-full transition-all duration-500`}
                            style={{ width: `${item.percentage}%` }}
                          />
                        </div>
                      </div>
                    ))}
                  </div>
                  <div className="pt-4 border-t border-gray-200 text-center">
                    <div className="text-3xl text-gray-900 mb-1">87%</div>
                    <div className="text-sm text-gray-600">Overall Resolution Rate</div>
                  </div>
                </div>
              </div>
            </>
          )}

          {/* Conversations Report */}
          {selectedReport === 'conversations' && !viewingCustomReport && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h3 className="text-gray-900 mb-4">Conversation Metrics</h3>
              <div className="grid grid-cols-3 gap-6 mb-6">
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <div className="text-3xl text-blue-600 mb-2">1,234</div>
                  <div className="text-sm text-gray-600">Total Conversations</div>
                </div>
                <div className="text-center p-4 bg-purple-50 rounded-lg">
                  <div className="text-3xl text-purple-600 mb-2">4.2</div>
                  <div className="text-sm text-gray-600">Avg Messages/Conv</div>
                </div>
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <div className="text-3xl text-green-600 mb-2">1.8s</div>
                  <div className="text-sm text-gray-600">Avg Response Time</div>
                </div>
              </div>
              <p className="text-gray-500 text-sm">More detailed conversation analytics coming soon...</p>
            </div>
          )}

          {/* Contacts Report */}
          {selectedReport === 'contacts' && !viewingCustomReport && (
            <div className="bg-white rounded-xl border border-gray-200 p-6">
              <h3 className="text-gray-900 mb-4">Contact Metrics</h3>
              <div className="grid grid-cols-3 gap-6 mb-6">
                <div className="text-center p-4 bg-green-50 rounded-lg">
                  <div className="text-3xl text-green-600 mb-2">856</div>
                  <div className="text-sm text-gray-600">Total Contacts</div>
                </div>
                <div className="text-center p-4 bg-blue-50 rounded-lg">
                  <div className="text-3xl text-blue-600 mb-2">+68</div>
                  <div className="text-sm text-gray-600">New This Period</div>
                </div>
                <div className="text-center p-4 bg-purple-50 rounded-lg">
                  <div className="text-3xl text-purple-600 mb-2">542</div>
                  <div className="text-sm text-gray-600">Active Contacts</div>
                </div>
              </div>
              <p className="text-gray-500 text-sm">More detailed contact analytics coming soon...</p>
            </div>
          )}
        </div>
      )}
    </div>
  );
}
