import { useState } from 'react';
import { Filter, Plus, X, Trash2 } from 'lucide-react';

export type FilterOperator = 
  | 'equals' 
  | 'notEquals' 
  | 'contains' 
  | 'notContains' 
  | 'startsWith' 
  | 'endsWith'
  | 'greaterThan'
  | 'lessThan'
  | 'greaterThanOrEqual'
  | 'lessThanOrEqual'
  | 'isEmpty'
  | 'isNotEmpty'
  | 'isAnyOf'
  | 'isNoneOf'
  | 'between'
  | 'before'
  | 'after'
  | 'inTheLast'
  | 'inTheNext';

export interface FilterCondition {
  id: string;
  field: string;
  operator: FilterOperator;
  value: any;
  value2?: any; // For "between" operator
}

export interface FilterGroup {
  logic: 'AND' | 'OR';
  conditions: FilterCondition[];
}

interface AdvancedFilterPanelProps {
  isOpen: boolean;
  onClose: () => void;
  filterGroup: FilterGroup;
  onFilterChange: (filterGroup: FilterGroup) => void;
  availableFields: { id: string; label: string; type: 'text' | 'number' | 'date' | 'select' | 'multiselect'; options?: string[] }[];
  onApply: () => void;
  onReset: () => void;
}

export function AdvancedFilterPanel({
  isOpen,
  onClose,
  filterGroup,
  onFilterChange,
  availableFields,
  onApply,
  onReset,
}: AdvancedFilterPanelProps) {
  if (!isOpen) return null;

  const addCondition = () => {
    const newCondition: FilterCondition = {
      id: `condition-${Date.now()}`,
      field: availableFields[0]?.id || '',
      operator: 'equals',
      value: '',
    };
    onFilterChange({
      ...filterGroup,
      conditions: [...filterGroup.conditions, newCondition],
    });
  };

  const removeCondition = (conditionId: string) => {
    onFilterChange({
      ...filterGroup,
      conditions: filterGroup.conditions.filter(c => c.id !== conditionId),
    });
  };

  const updateCondition = (conditionId: string, updates: Partial<FilterCondition>) => {
    onFilterChange({
      ...filterGroup,
      conditions: filterGroup.conditions.map(c =>
        c.id === conditionId ? { ...c, ...updates } : c
      ),
    });
  };

  const getOperatorsForField = (fieldType: string): { value: FilterOperator; label: string }[] => {
    const commonOperators = [
      { value: 'equals' as FilterOperator, label: 'Equals' },
      { value: 'notEquals' as FilterOperator, label: 'Does not equal' },
      { value: 'isEmpty' as FilterOperator, label: 'Is empty' },
      { value: 'isNotEmpty' as FilterOperator, label: 'Is not empty' },
    ];

    const textOperators = [
      { value: 'contains' as FilterOperator, label: 'Contains' },
      { value: 'notContains' as FilterOperator, label: 'Does not contain' },
      { value: 'startsWith' as FilterOperator, label: 'Starts with' },
      { value: 'endsWith' as FilterOperator, label: 'Ends with' },
    ];

    const numberOperators = [
      { value: 'greaterThan' as FilterOperator, label: 'Greater than' },
      { value: 'lessThan' as FilterOperator, label: 'Less than' },
      { value: 'greaterThanOrEqual' as FilterOperator, label: 'Greater than or equal' },
      { value: 'lessThanOrEqual' as FilterOperator, label: 'Less than or equal' },
      { value: 'between' as FilterOperator, label: 'Between' },
    ];

    const selectOperators = [
      { value: 'isAnyOf' as FilterOperator, label: 'Is any of' },
      { value: 'isNoneOf' as FilterOperator, label: 'Is none of' },
    ];

    const dateOperators = [
      { value: 'before' as FilterOperator, label: 'Before' },
      { value: 'after' as FilterOperator, label: 'After' },
      { value: 'between' as FilterOperator, label: 'Between' },
      { value: 'inTheLast' as FilterOperator, label: 'In the last' },
      { value: 'inTheNext' as FilterOperator, label: 'In the next' },
    ];

    switch (fieldType) {
      case 'text':
        return [...commonOperators, ...textOperators];
      case 'number':
        return [...commonOperators, ...numberOperators];
      case 'date':
        return [...commonOperators, ...dateOperators];
      case 'select':
      case 'multiselect':
        return [...commonOperators, ...selectOperators];
      default:
        return commonOperators;
    }
  };

  const needsValueInput = (operator: FilterOperator) => {
    return !['isEmpty', 'isNotEmpty'].includes(operator);
  };

  const needsSecondValue = (operator: FilterOperator) => {
    return operator === 'between';
  };

  return (
    <>
      <div
        className="fixed inset-0 bg-black/50 z-40"
        onClick={onClose}
      />
      <div className="fixed right-0 top-0 bottom-0 w-[600px] bg-white shadow-2xl z-50 flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-gray-200">
          <div className="flex items-center gap-3">
            <div className="w-8 h-8 bg-blue-100 rounded-lg flex items-center justify-center">
              <Filter className="w-4 h-4 text-blue-600" />
            </div>
            <div>
              <h2 className="text-gray-900">Advanced Filters</h2>
              <p className="text-sm text-gray-600">
                {filterGroup.conditions.length} condition{filterGroup.conditions.length !== 1 ? 's' : ''}
              </p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 hover:bg-gray-100 rounded-lg transition-colors"
          >
            <X className="w-5 h-5 text-gray-600" />
          </button>
        </div>

        {/* Logic Selector */}
        {filterGroup.conditions.length > 1 && (
          <div className="px-6 py-4 border-b border-gray-200 bg-gray-50">
            <label className="text-sm text-gray-700 mb-2 block">Match</label>
            <div className="flex gap-2">
              <button
                onClick={() => onFilterChange({ ...filterGroup, logic: 'AND' })}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  filterGroup.logic === 'AND'
                    ? 'bg-blue-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                All conditions (AND)
              </button>
              <button
                onClick={() => onFilterChange({ ...filterGroup, logic: 'OR' })}
                className={`px-4 py-2 rounded-lg transition-colors ${
                  filterGroup.logic === 'OR'
                    ? 'bg-blue-600 text-white'
                    : 'bg-white border border-gray-300 text-gray-700 hover:bg-gray-50'
                }`}
              >
                Any condition (OR)
              </button>
            </div>
          </div>
        )}

        {/* Conditions List */}
        <div className="flex-1 overflow-y-auto px-6 py-4">
          {filterGroup.conditions.length === 0 ? (
            <div className="text-center py-12">
              <Filter className="w-12 h-12 text-gray-300 mx-auto mb-3" />
              <p className="text-gray-600 mb-4">No filter conditions yet</p>
              <button
                onClick={addCondition}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
              >
                <Plus className="w-4 h-4" />
                Add Condition
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              {filterGroup.conditions.map((condition, index) => {
                const field = availableFields.find(f => f.id === condition.field);
                const operators = field ? getOperatorsForField(field.type) : [];

                return (
                  <div key={condition.id} className="border border-gray-200 rounded-lg p-4 bg-white">
                    {/* Condition Header */}
                    <div className="flex items-center justify-between mb-3">
                      <span className="text-xs text-gray-500 uppercase tracking-wider">
                        Condition {index + 1}
                        {index > 0 && (
                          <span className="ml-2 px-2 py-0.5 bg-gray-100 rounded text-gray-700">
                            {filterGroup.logic}
                          </span>
                        )}
                      </span>
                      <button
                        onClick={() => removeCondition(condition.id)}
                        className="p-1 hover:bg-red-50 rounded transition-colors"
                      >
                        <Trash2 className="w-4 h-4 text-red-600" />
                      </button>
                    </div>

                    {/* Field Selector */}
                    <div className="mb-3">
                      <label className="text-sm text-gray-700 mb-1 block">Field</label>
                      <select
                        value={condition.field}
                        onChange={(e) => updateCondition(condition.id, { 
                          field: e.target.value,
                          operator: 'equals', // Reset operator when field changes
                          value: ''
                        })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      >
                        {availableFields.map(field => (
                          <option key={field.id} value={field.id}>{field.label}</option>
                        ))}
                      </select>
                    </div>

                    {/* Operator Selector */}
                    <div className="mb-3">
                      <label className="text-sm text-gray-700 mb-1 block">Operator</label>
                      <select
                        value={condition.operator}
                        onChange={(e) => updateCondition(condition.id, { operator: e.target.value as FilterOperator })}
                        className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                      >
                        {operators.map(op => (
                          <option key={op.value} value={op.value}>{op.label}</option>
                        ))}
                      </select>
                    </div>

                    {/* Value Input */}
                    {needsValueInput(condition.operator) && (
                      <div className="mb-3">
                        <label className="text-sm text-gray-700 mb-1 block">Value</label>
                        {field?.type === 'select' || field?.type === 'multiselect' ? (
                          condition.operator === 'isAnyOf' || condition.operator === 'isNoneOf' ? (
                            <div className="border border-gray-300 rounded-lg p-2 max-h-32 overflow-y-auto">
                              {field.options?.map(option => (
                                <label key={option} className="flex items-center gap-2 py-1 cursor-pointer">
                                  <input
                                    type="checkbox"
                                    checked={Array.isArray(condition.value) && condition.value.includes(option)}
                                    onChange={(e) => {
                                      const currentValues = Array.isArray(condition.value) ? condition.value : [];
                                      const newValue = e.target.checked
                                        ? [...currentValues, option]
                                        : currentValues.filter(v => v !== option);
                                      updateCondition(condition.id, { value: newValue });
                                    }}
                                    className="w-4 h-4"
                                  />
                                  <span className="text-sm text-gray-700">{option}</span>
                                </label>
                              ))}
                            </div>
                          ) : (
                            <select
                              value={condition.value}
                              onChange={(e) => updateCondition(condition.id, { value: e.target.value })}
                              className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                            >
                              <option value="">Select...</option>
                              {field.options?.map(option => (
                                <option key={option} value={option}>{option}</option>
                              ))}
                            </select>
                          )
                        ) : field?.type === 'number' ? (
                          <input
                            type="number"
                            value={condition.value}
                            onChange={(e) => updateCondition(condition.id, { value: e.target.value })}
                            placeholder="Enter number..."
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        ) : field?.type === 'date' ? (
                          <input
                            type="date"
                            value={condition.value}
                            onChange={(e) => updateCondition(condition.id, { value: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        ) : (
                          <input
                            type="text"
                            value={condition.value}
                            onChange={(e) => updateCondition(condition.id, { value: e.target.value })}
                            placeholder="Enter value..."
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        )}
                      </div>
                    )}

                    {/* Second Value Input (for "between" operator) */}
                    {needsSecondValue(condition.operator) && (
                      <div>
                        <label className="text-sm text-gray-700 mb-1 block">And</label>
                        {field?.type === 'number' ? (
                          <input
                            type="number"
                            value={condition.value2 || ''}
                            onChange={(e) => updateCondition(condition.id, { value2: e.target.value })}
                            placeholder="Enter second number..."
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        ) : field?.type === 'date' ? (
                          <input
                            type="date"
                            value={condition.value2 || ''}
                            onChange={(e) => updateCondition(condition.id, { value2: e.target.value })}
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        ) : (
                          <input
                            type="text"
                            value={condition.value2 || ''}
                            onChange={(e) => updateCondition(condition.id, { value2: e.target.value })}
                            placeholder="Enter second value..."
                            className="w-full px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
                          />
                        )}
                      </div>
                    )}
                  </div>
                );
              })}

              {/* Add Condition Button */}
              <button
                onClick={addCondition}
                className="w-full py-2.5 border-2 border-dashed border-gray-300 rounded-lg hover:border-blue-500 hover:bg-blue-50 transition-colors flex items-center justify-center gap-2 text-gray-600 hover:text-blue-600"
              >
                <Plus className="w-4 h-4" />
                Add Condition
              </button>
            </div>
          )}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between px-6 py-4 border-t border-gray-200 bg-gray-50">
          <button
            onClick={onReset}
            className="px-4 py-2 text-gray-700 hover:text-gray-900 transition-colors"
          >
            Reset All
          </button>
          <div className="flex items-center gap-2">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-100 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={() => {
                onApply();
                onClose();
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
            >
              Apply Filters
            </button>
          </div>
        </div>
      </div>
    </>
  );
}
