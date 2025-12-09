import { useState } from 'react';
import { X, Filter, Plus, Trash2 } from 'lucide-react';
import { FilterGroup } from './AdvancedFilterPanel';

interface CreateConversationViewModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSave: (viewData: { name: string; description: string; filters: FilterGroup }) => void;
  editingView?: { name: string; description: string; filters: FilterGroup } | null;
  availableChannels: string[];
  availableTags: string[];
}

interface FilterCondition {
  id: string;
  field: string;
  operator: string;
  value: string;
}

export function CreateConversationViewModal({ 
  isOpen, 
  onClose, 
  onSave, 
  editingView,
  availableChannels,
  availableTags
}: CreateConversationViewModalProps) {
  const [name, setName] = useState(editingView?.name || '');
  const [description, setDescription] = useState(editingView?.description || '');
  const [filters, setFilters] = useState<FilterGroup>(
    editingView?.filters || { logic: 'AND', conditions: [] }
  );

  if (!isOpen) return null;

  const addFilter = () => {
    const newCondition: FilterCondition = {
      id: `condition-${Date.now()}`,
      field: 'status',
      operator: 'equals',
      value: 'needs_help',
    };
    setFilters({
      ...filters,
      conditions: [...filters.conditions, newCondition],
    });
  };

  const removeFilter = (conditionId: string) => {
    setFilters({
      ...filters,
      conditions: filters.conditions.filter((c) => c.id !== conditionId),
    });
  };

  const updateFilter = (conditionId: string, updates: Partial<FilterCondition>) => {
    setFilters({
      ...filters,
      conditions: filters.conditions.map((c) =>
        c.id === conditionId ? { ...c, ...updates } : c
      ),
    });
  };

  const handleSave = () => {
    if (name.trim()) {
      onSave({ 
        name: name.trim(), 
        description: description.trim(),
        filters 
      });
      setName('');
      setDescription('');
      setFilters({ logic: 'AND', conditions: [] });
      onClose();
    }
  };

  const getFieldLabel = (field: string) => {
    const labels: Record<string, string> = {
      status: 'Status',
      channel: 'Channel',
      tags: 'Tags',
      unread: 'Read Status',
      assignedTo: 'Assigned To',
    };
    return labels[field] || field;
  };

  const getOperatorOptions = (field: string) => {
    if (field === 'tags') {
      return [
        { value: 'contains', label: 'Contains' },
        { value: 'not_contains', label: 'Does Not Contain' },
      ];
    }
    return [
      { value: 'equals', label: 'Equals' },
      { value: 'not_equals', label: 'Does Not Equal' },
    ];
  };

  const getValueOptions = (field: string) => {
    switch (field) {
      case 'status':
        return [
          { value: 'needs_help', label: 'Needs Help' },
          { value: 'resolved', label: 'Resolved' },
        ];
      case 'channel':
        return availableChannels.map(ch => ({ value: ch, label: ch.charAt(0).toUpperCase() + ch.slice(1) }));
      case 'tags':
        return availableTags.map(tag => ({ value: tag, label: tag }));
      case 'unread':
        return [
          { value: 'true', label: 'Unread' },
          { value: 'false', label: 'Read' },
        ];
      case 'assignedTo':
        return [
          { value: '1', label: 'Me' },
          { value: 'null', label: 'Unassigned' },
        ];
      default:
        return [];
    }
  };

  return (
    <>
      <div
        className="fixed inset-0 bg-black/50 z-40"
        onClick={onClose}
      />
      <div className="fixed inset-0 flex items-center justify-center z-50 p-4">
        <div className="bg-white rounded-xl shadow-2xl w-full max-w-3xl max-h-[90vh] overflow-y-auto">
          {/* Header */}
          <div className="sticky top-0 bg-white flex items-center justify-between px-6 py-4 border-b border-gray-200 rounded-t-xl">
            <div>
              <h2 className="text-gray-900">
                {editingView ? 'Edit View' : 'Create New View'}
              </h2>
              <p className="text-sm text-gray-600 mt-1">
                Configure filters to show specific conversations
              </p>
            </div>
            <button
              onClick={onClose}
              className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
            >
              <X className="w-5 h-5 text-gray-600" />
            </button>
          </div>

          {/* Body */}
          <div className="px-6 py-4 space-y-6">
            {/* View Details */}
            <div className="space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">View Name *</label>
                <input
                  type="text"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  placeholder="e.g., Urgent Email Conversations"
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                  autoFocus
                />
              </div>

              <div>
                <label className="block text-sm text-gray-700 mb-2">Description (Optional)</label>
                <textarea
                  value={description}
                  onChange={(e) => setDescription(e.target.value)}
                  placeholder="Describe what this view shows..."
                  rows={2}
                  className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                />
              </div>
            </div>

            {/* Filter Configuration */}
            <div className="border-t border-gray-200 pt-6">
              <div className="flex items-center justify-between mb-4">
                <div>
                  <h3 className="text-gray-900 flex items-center gap-2">
                    <Filter className="w-5 h-5" />
                    Filters
                  </h3>
                  <p className="text-sm text-gray-600 mt-1">
                    Show conversations that match {' '}
                    <select
                      value={filters.logic}
                      onChange={(e) => setFilters({ ...filters, logic: e.target.value as 'AND' | 'OR' })}
                      className="inline-block px-2 py-1 border border-gray-300 rounded text-sm focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                    >
                      <option value="AND">all</option>
                      <option value="OR">any</option>
                    </select>
                    {' '} of these conditions:
                  </p>
                </div>
                <button
                  onClick={addFilter}
                  className="px-3 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 text-sm"
                >
                  <Plus className="w-4 h-4" />
                  Add Filter
                </button>
              </div>

              {/* Filter Conditions */}
              <div className="space-y-3">
                {filters.conditions.length === 0 ? (
                  <div className="text-center py-8 bg-gray-50 rounded-lg border-2 border-dashed border-gray-300">
                    <Filter className="w-12 h-12 mx-auto mb-3 text-gray-300" />
                    <p className="text-gray-600 mb-2">No filters configured</p>
                    <p className="text-sm text-gray-500">
                      Click "Add Filter" to configure conditions for this view
                    </p>
                  </div>
                ) : (
                  filters.conditions.map((condition, index) => (
                    <div key={condition.id} className="flex items-start gap-3 p-4 bg-gray-50 rounded-lg border border-gray-200">
                      <div className="flex-1 grid grid-cols-3 gap-3">
                        {/* Field Select */}
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Field</label>
                          <select
                            value={condition.field}
                            onChange={(e) => {
                              const newField = e.target.value;
                              const defaultValue = getValueOptions(newField)[0]?.value || '';
                              updateFilter(condition.id, { 
                                field: newField,
                                value: defaultValue,
                                operator: getOperatorOptions(newField)[0].value
                              });
                            }}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                          >
                            <option value="status">Status</option>
                            <option value="channel">Channel</option>
                            <option value="tags">Tags</option>
                            <option value="unread">Read Status</option>
                            <option value="assignedTo">Assigned To</option>
                          </select>
                        </div>

                        {/* Operator Select */}
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Condition</label>
                          <select
                            value={condition.operator}
                            onChange={(e) => updateFilter(condition.id, { operator: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                          >
                            {getOperatorOptions(condition.field).map((op) => (
                              <option key={op.value} value={op.value}>
                                {op.label}
                              </option>
                            ))}
                          </select>
                        </div>

                        {/* Value Select */}
                        <div>
                          <label className="block text-xs text-gray-600 mb-1">Value</label>
                          <select
                            value={condition.value}
                            onChange={(e) => updateFilter(condition.id, { value: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent text-sm"
                          >
                            {getValueOptions(condition.field).map((opt) => (
                              <option key={opt.value} value={opt.value}>
                                {opt.label}
                              </option>
                            ))}
                          </select>
                        </div>
                      </div>

                      {/* Remove Button */}
                      <button
                        onClick={() => removeFilter(condition.id)}
                        className="p-2 hover:bg-red-50 text-red-600 rounded-lg transition-colors mt-6"
                        title="Remove filter"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  ))
                )}
              </div>

              {/* Filter Preview */}
              {filters.conditions.length > 0 && (
                <div className="mt-4 p-4 bg-blue-50 border border-blue-200 rounded-lg">
                  <p className="text-sm text-blue-900 mb-2">
                    <strong>Filter Summary:</strong>
                  </p>
                  <p className="text-sm text-blue-800">
                    Show conversations where{' '}
                    <strong>
                      {filters.logic === 'AND' ? 'all' : 'any'}
                    </strong>
                    {' '}of the following are true:
                  </p>
                  <ul className="mt-2 space-y-1">
                    {filters.conditions.map((condition) => {
                      const operator = getOperatorOptions(condition.field).find(op => op.value === condition.operator);
                      const value = getValueOptions(condition.field).find(v => v.value === condition.value);
                      return (
                        <li key={condition.id} className="text-sm text-blue-800 ml-4">
                          • {getFieldLabel(condition.field)} {operator?.label.toLowerCase()} "{value?.label}"
                        </li>
                      );
                    })}
                  </ul>
                </div>
              )}
            </div>
          </div>

          {/* Footer */}
          <div className="sticky bottom-0 bg-gray-50 flex items-center justify-end gap-2 px-6 py-4 border-t border-gray-200 rounded-b-xl">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={handleSave}
              disabled={!name.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {editingView ? 'Save Changes' : 'Create View'}
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
