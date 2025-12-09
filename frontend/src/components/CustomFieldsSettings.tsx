import { useState } from 'react';
import { Plus, Edit2, Trash2, X, Save, GripVertical, Type, List, Hash, AlignLeft, Circle, CheckSquare, FolderPlus } from 'lucide-react';

type FieldType = 'text' | 'dropdown' | 'number' | 'paragraph' | 'radio' | 'checkbox' | 'multiselect';

interface CustomField {
  id: string;
  name: string;
  type: FieldType;
  group: string;
  required: boolean;
  options?: string[];
  defaultValue?: string;
}

interface FieldGroup {
  id: string;
  name: string;
  order: number;
}

const FIELD_LIMITS: Record<FieldType, number> = {
  text: 20,
  dropdown: 20,
  number: 20,
  paragraph: 5,
  radio: 10,
  checkbox: 10,
  multiselect: 5,
};

const FIELD_TYPE_INFO = [
  { type: 'text' as FieldType, label: 'Text Field', icon: Type, description: 'Single line text input', color: 'blue' },
  { type: 'number' as FieldType, label: 'Number Field', icon: Hash, description: 'Numeric values only', color: 'green' },
  { type: 'dropdown' as FieldType, label: 'Dropdown', icon: List, description: 'Select one option from list', color: 'purple' },
  { type: 'multiselect' as FieldType, label: 'Multi-Select', icon: CheckSquare, description: 'Select multiple options', color: 'indigo' },
  { type: 'radio' as FieldType, label: 'Radio Buttons', icon: Circle, description: 'Choose one option', color: 'pink' },
  { type: 'checkbox' as FieldType, label: 'Checkboxes', icon: CheckSquare, description: 'Multiple selections', color: 'teal' },
  { type: 'paragraph' as FieldType, label: 'Paragraph', icon: AlignLeft, description: 'Multi-line text area', color: 'orange' },
];

export function CustomFieldsSettings({ entityType }: { entityType: 'contact' | 'account' }) {
  const [fields, setFields] = useState<CustomField[]>([
    { id: '1', name: 'Industry', type: 'dropdown', group: 'Business Info', required: false, options: ['Technology', 'Healthcare', 'Finance', 'Retail'] },
    { id: '2', name: 'Annual Revenue', type: 'number', group: 'Business Info', required: false },
    { id: '3', name: 'Notes', type: 'paragraph', group: 'General', required: false },
  ]);
  
  const [groups, setGroups] = useState<FieldGroup[]>([
    { id: '1', name: 'General', order: 1 },
    { id: '2', name: 'Business Info', order: 2 },
    { id: '3', name: 'Custom Data', order: 3 },
  ]);

  const [showFieldModal, setShowFieldModal] = useState(false);
  const [showGroupModal, setShowGroupModal] = useState(false);
  const [editingField, setEditingField] = useState<CustomField | null>(null);
  const [editingGroup, setEditingGroup] = useState<FieldGroup | null>(null);

  const getFieldCount = (type: FieldType) => fields.filter(f => f.type === type).length;

  const handleAddField = (field: CustomField) => {
    if (editingField) {
      setFields(fields.map(f => f.id === editingField.id ? field : f));
    } else {
      setFields([...fields, field]);
    }
    setShowFieldModal(false);
    setEditingField(null);
  };

  const handleDeleteField = (id: string) => {
    setFields(fields.filter(f => f.id !== id));
  };

  const handleAddGroup = (group: FieldGroup) => {
    if (editingGroup) {
      setGroups(groups.map(g => g.id === editingGroup.id ? group : g));
    } else {
      setGroups([...groups, group]);
    }
    setShowGroupModal(false);
    setEditingGroup(null);
  };

  const handleDeleteGroup = (id: string) => {
    const group = groups.find(g => g.id === id);
    if (group && fields.some(f => f.group === group.name)) {
      alert('Cannot delete group that contains fields. Please move or delete the fields first.');
      return;
    }
    setGroups(groups.filter(g => g.id !== id));
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <h3 className="text-gray-900 mb-2">Custom Fields</h3>
        <p className="text-sm text-gray-600">
          Create custom fields to capture additional information specific to your {entityType}s
        </p>
      </div>

      {/* Field Type Overview */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h4 className="text-gray-900">Field Type Limits</h4>
          <button
            onClick={() => {
              setEditingField(null);
              setShowFieldModal(true);
            }}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Plus className="w-4 h-4" />
            Add Field
          </button>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {FIELD_TYPE_INFO.map((info) => {
            const count = getFieldCount(info.type);
            const limit = FIELD_LIMITS[info.type];
            const percentage = (count / limit) * 100;
            const Icon = info.icon;

            return (
              <div key={info.type} className="border border-gray-200 rounded-lg p-4">
                <div className="flex items-start justify-between mb-3">
                  <div className={`w-10 h-10 rounded-lg bg-${info.color}-100 flex items-center justify-center`}>
                    <Icon className={`w-5 h-5 text-${info.color}-600`} />
                  </div>
                  <span className="text-xs text-gray-500">{count}/{limit}</span>
                </div>
                <h5 className="text-sm text-gray-900 mb-1">{info.label}</h5>
                <p className="text-xs text-gray-500 mb-3">{info.description}</p>
                <div className="w-full bg-gray-200 rounded-full h-1.5">
                  <div
                    className={`h-1.5 rounded-full ${
                      percentage >= 100 ? 'bg-red-600' : percentage >= 80 ? 'bg-yellow-600' : `bg-${info.color}-600`
                    }`}
                    style={{ width: `${Math.min(percentage, 100)}%` }}
                  />
                </div>
              </div>
            );
          })}
        </div>
      </div>

      {/* Field Groups */}
      <div className="bg-white rounded-xl border border-gray-200 p-6">
        <div className="flex items-center justify-between mb-4">
          <h4 className="text-gray-900">Field Groups</h4>
          <button
            onClick={() => {
              setEditingGroup(null);
              setShowGroupModal(true);
            }}
            className="px-3 py-1.5 text-sm border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors flex items-center gap-2"
          >
            <FolderPlus className="w-4 h-4" />
            Add Group
          </button>
        </div>

        <div className="space-y-3">
          {groups.sort((a, b) => a.order - b.order).map((group) => {
            const groupFields = fields.filter(f => f.group === group.name);
            return (
              <div key={group.id} className="border border-gray-200 rounded-lg p-4">
                <div className="flex items-center justify-between mb-3">
                  <div className="flex items-center gap-3">
                    <GripVertical className="w-4 h-4 text-gray-400" />
                    <div>
                      <h5 className="text-sm text-gray-900">{group.name}</h5>
                      <p className="text-xs text-gray-500">{groupFields.length} fields</p>
                    </div>
                  </div>
                  <div className="flex items-center gap-2">
                    <button
                      onClick={() => {
                        setEditingGroup(group);
                        setShowGroupModal(true);
                      }}
                      className="p-1.5 hover:bg-gray-100 rounded transition-colors"
                    >
                      <Edit2 className="w-3.5 h-3.5 text-gray-600" />
                    </button>
                    <button
                      onClick={() => handleDeleteGroup(group.id)}
                      className="p-1.5 hover:bg-red-50 rounded transition-colors"
                    >
                      <Trash2 className="w-3.5 h-3.5 text-red-600" />
                    </button>
                  </div>
                </div>

                {groupFields.length > 0 && (
                  <div className="space-y-2 mt-3 pt-3 border-t border-gray-200">
                    {groupFields.map((field) => (
                      <div key={field.id} className="flex items-center justify-between py-2 px-3 bg-gray-50 rounded-lg">
                        <div className="flex items-center gap-3">
                          <div className="text-xs text-gray-500 uppercase w-20">{field.type}</div>
                          <span className="text-sm text-gray-900">{field.name}</span>
                          {field.required && (
                            <span className="px-2 py-0.5 bg-red-100 text-red-700 text-xs rounded">Required</span>
                          )}
                        </div>
                        <div className="flex items-center gap-2">
                          <button
                            onClick={() => {
                              setEditingField(field);
                              setShowFieldModal(true);
                            }}
                            className="p-1.5 hover:bg-white rounded transition-colors"
                          >
                            <Edit2 className="w-3.5 h-3.5 text-gray-600" />
                          </button>
                          <button
                            onClick={() => handleDeleteField(field.id)}
                            className="p-1.5 hover:bg-white rounded transition-colors"
                          >
                            <Trash2 className="w-3.5 h-3.5 text-red-600" />
                          </button>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Add/Edit Field Modal */}
      {showFieldModal && (
        <FieldModal
          field={editingField}
          groups={groups}
          onSave={handleAddField}
          onClose={() => {
            setShowFieldModal(false);
            setEditingField(null);
          }}
          fieldCounts={FIELD_TYPE_INFO.map(info => ({
            type: info.type,
            count: getFieldCount(info.type),
            limit: FIELD_LIMITS[info.type],
          }))}
        />
      )}

      {/* Add/Edit Group Modal */}
      {showGroupModal && (
        <GroupModal
          group={editingGroup}
          onSave={handleAddGroup}
          onClose={() => {
            setShowGroupModal(false);
            setEditingGroup(null);
          }}
        />
      )}
    </div>
  );
}

function FieldModal({ field, groups, onSave, onClose, fieldCounts }: any) {
  const [formData, setFormData] = useState<CustomField>(
    field || {
      id: Date.now().toString(),
      name: '',
      type: 'text' as FieldType,
      group: groups[0]?.name || '',
      required: false,
      options: [],
      defaultValue: '',
    }
  );
  const [optionsText, setOptionsText] = useState(field?.options?.join('\n') || '');

  const selectedTypeInfo = FIELD_TYPE_INFO.find(t => t.type === formData.type);
  const currentCount = fieldCounts.find((c: any) => c.type === formData.type);
  const isAtLimit = !field && currentCount && currentCount.count >= currentCount.limit;

  const needsOptions = ['dropdown', 'radio', 'checkbox', 'multiselect'].includes(formData.type);

  const handleSave = () => {
    if (!formData.name.trim()) {
      alert('Please enter a field name');
      return;
    }

    if (needsOptions && optionsText.trim() === '') {
      alert('Please enter at least one option');
      return;
    }

    const options = needsOptions ? optionsText.split('\n').filter(o => o.trim()) : undefined;
    onSave({ ...formData, options });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-2xl w-full max-h-[90vh] overflow-y-auto">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between sticky top-0 bg-white">
          <h2 className="text-gray-900">{field ? 'Edit' : 'Add'} Custom Field</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6 space-y-4">
          {/* Field Name */}
          <div>
            <label className="block text-sm text-gray-700 mb-2">Field Name *</label>
            <input
              type="text"
              value={formData.name}
              onChange={(e) => setFormData({ ...formData, name: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              placeholder="e.g., Industry, Budget, Department..."
            />
          </div>

          {/* Field Type */}
          <div>
            <label className="block text-sm text-gray-700 mb-2">Field Type *</label>
            <div className="grid grid-cols-2 gap-3">
              {FIELD_TYPE_INFO.map((typeInfo) => {
                const typeCount = fieldCounts.find((c: any) => c.type === typeInfo.type);
                const isDisabled = !field && typeCount && typeCount.count >= typeCount.limit;
                const Icon = typeInfo.icon;

                return (
                  <button
                    key={typeInfo.type}
                    onClick={() => !isDisabled && setFormData({ ...formData, type: typeInfo.type })}
                    disabled={isDisabled}
                    className={`p-3 border-2 rounded-lg text-left transition-all ${
                      formData.type === typeInfo.type
                        ? 'border-blue-500 bg-blue-50'
                        : isDisabled
                        ? 'border-gray-200 bg-gray-50 opacity-50 cursor-not-allowed'
                        : 'border-gray-200 hover:border-gray-300'
                    }`}
                  >
                    <div className="flex items-center gap-2 mb-1">
                      <Icon className="w-4 h-4" />
                      <span className="text-sm">{typeInfo.label}</span>
                    </div>
                    <p className="text-xs text-gray-500">{typeCount?.count}/{typeCount?.limit}</p>
                  </button>
                );
              })}
            </div>
            {isAtLimit && (
              <p className="text-xs text-red-600 mt-2">
                Limit reached for this field type ({currentCount.count}/{currentCount.limit})
              </p>
            )}
          </div>

          {/* Group */}
          <div>
            <label className="block text-sm text-gray-700 mb-2">Group *</label>
            <select
              value={formData.group}
              onChange={(e) => setFormData({ ...formData, group: e.target.value })}
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            >
              {groups.map((group: FieldGroup) => (
                <option key={group.id} value={group.name}>
                  {group.name}
                </option>
              ))}
            </select>
          </div>

          {/* Options (for dropdown, radio, checkbox, multiselect) */}
          {needsOptions && (
            <div>
              <label className="block text-sm text-gray-700 mb-2">Options (one per line) *</label>
              <textarea
                value={optionsText}
                onChange={(e) => setOptionsText(e.target.value)}
                rows={6}
                className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                placeholder="Option 1&#10;Option 2&#10;Option 3"
              />
            </div>
          )}

          {/* Required */}
          <div className="flex items-center gap-2">
            <input
              type="checkbox"
              id="required"
              checked={formData.required}
              onChange={(e) => setFormData({ ...formData, required: e.target.checked })}
              className="w-4 h-4 text-blue-600 rounded focus:ring-2 focus:ring-blue-500"
            />
            <label htmlFor="required" className="text-sm text-gray-700">
              Make this field required
            </label>
          </div>
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            disabled={isAtLimit}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            <Save className="w-4 h-4" />
            {field ? 'Update' : 'Create'} Field
          </button>
        </div>
      </div>
    </div>
  );
}

function GroupModal({ group, onSave, onClose }: any) {
  const [name, setName] = useState(group?.name || '');

  const handleSave = () => {
    if (!name.trim()) {
      alert('Please enter a group name');
      return;
    }

    onSave({
      id: group?.id || Date.now().toString(),
      name: name.trim(),
      order: group?.order || Date.now(),
    });
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full">
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
          <h2 className="text-gray-900">{group ? 'Edit' : 'Add'} Field Group</h2>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-6">
          <label className="block text-sm text-gray-700 mb-2">Group Name *</label>
          <input
            type="text"
            value={name}
            onChange={(e) => setName(e.target.value)}
            className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            placeholder="e.g., Business Info, Custom Data..."
          />
        </div>

        <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
          <button
            onClick={onClose}
            className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
          >
            Cancel
          </button>
          <button
            onClick={handleSave}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Save className="w-4 h-4" />
            {group ? 'Update' : 'Create'} Group
          </button>
        </div>
      </div>
    </div>
  );
}