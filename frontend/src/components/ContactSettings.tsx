import { useState } from 'react';
import { Save, Plus, Trash2, Users, Shield, Tag, ArrowLeft, Crown } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface ContactSettingsProps {
  onNavigate?: (screen: string) => void;
  currentPlan?: 'free' | 'professional' | 'enterprise';
}

export function ContactSettings({ onNavigate, currentPlan = 'professional' }: ContactSettingsProps) {
  const [customFields, setCustomFields] = useState([
    { id: 1, name: 'Job Title', type: 'text', enabled: true },
    { id: 2, name: 'Department', type: 'text', enabled: true },
  ]);

  const [tags, setTags] = useState([
    { id: 1, name: 'VIP', color: '#F59E0B' },
    { id: 2, name: 'Partner', color: '#8B5CF6' },
    { id: 3, name: 'Lead', color: '#3B82F6' },
  ]);

  const [newFieldName, setNewFieldName] = useState('');
  const [newFieldType, setNewFieldType] = useState('text');
  const [newTagName, setNewTagName] = useState('');
  const [newTagColor, setNewTagColor] = useState('#3B82F6');
  
  const [autoDeleteInactive, setAutoDeleteInactive] = useState(false);
  const [enableDeduplication, setEnableDeduplication] = useState(true);
  const [showSaveNotification, setShowSaveNotification] = useState(false);
  const [showUpgradePrompt, setShowUpgradePrompt] = useState(false);

  // Get field limit based on plan
  const fieldLimits = {
    free: 2,
    professional: 10,
    enterprise: 20,
  };

  const maxFields = fieldLimits[currentPlan];
  const canAddMoreFields = customFields.length < maxFields;

  const addCustomField = () => {
    if (!canAddMoreFields) {
      setShowUpgradePrompt(true);
      setTimeout(() => setShowUpgradePrompt(false), 4000);
      return;
    }

    if (newFieldName.trim()) {
      setCustomFields([
        ...customFields,
        {
          id: Date.now(),
          name: newFieldName.trim(),
          type: newFieldType,
          enabled: true,
        },
      ]);
      setNewFieldName('');
      setNewFieldType('text');
    }
  };

  const removeCustomField = (id: number) => {
    setCustomFields(customFields.filter(f => f.id !== id));
  };

  const addTag = () => {
    if (newTagName.trim()) {
      setTags([
        ...tags,
        {
          id: Date.now(),
          name: newTagName.trim(),
          color: newTagColor,
        },
      ]);
      setNewTagName('');
    }
  };

  const removeTag = (id: number) => {
    setTags(tags.filter(t => t.id !== id));
  };

  const saveChanges = () => {
    console.log('Saving changes...', {
      customFields,
      tags,
      autoDeleteInactive,
      enableDeduplication
    });
    setShowSaveNotification(true);
    setTimeout(() => setShowSaveNotification(false), 3000);
  };

  return (
    <div className="p-4 md:p-6 lg:p-8 max-w-4xl mx-auto">
      {/* Back Button */}
      {onNavigate && <BackToSettings onNavigate={onNavigate} />}
      <DemoBanner />
      
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Contact Settings</h1>
        <p className="text-gray-600 text-sm md:text-base">Configure custom fields and contact management options</p>
      </div>

      <div className="space-y-6">
        {/* Custom Fields */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4">
            <div className="flex items-center gap-3">
              <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center">
                <Users className="w-5 h-5 text-blue-600" />
              </div>
              <div>
                <h2 className="text-gray-900">Custom Fields</h2>
                <p className="text-sm text-gray-600">Add custom fields to capture additional contact information</p>
              </div>
            </div>
            <div className="flex items-center gap-2">
              <div className={`px-3 py-1 rounded-full text-sm ${
                customFields.length >= maxFields 
                  ? 'bg-red-100 text-red-700' 
                  : customFields.length >= maxFields * 0.8 
                  ? 'bg-yellow-100 text-yellow-700' 
                  : 'bg-blue-100 text-blue-700'
              }`}>
                {customFields.length}/{maxFields} fields
              </div>
            </div>
          </div>

          <div className="space-y-3 mb-4">
            {customFields.map((field) => (
              <div key={field.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div className="text-sm text-gray-900">{field.name}</div>
                  <span className="px-2 py-0.5 text-xs bg-gray-200 text-gray-700 rounded">
                    {field.type}
                  </span>
                </div>
                <button
                  type="button"
                  onClick={() => removeCustomField(field.id)}
                  className="p-2 hover:bg-red-50 text-red-600 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>

          <div className="flex gap-2">
            <input
              type="text"
              value={newFieldName}
              onChange={(e) => setNewFieldName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault()
                  addCustomField();
                }
              }}
              placeholder="Field name (e.g., LinkedIn URL)"
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={!canAddMoreFields}
            />
            <select
              value={newFieldType}
              onChange={(e) => setNewFieldType(e.target.value)}
              className="px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
              disabled={!canAddMoreFields}
            >
              <option value="text">Text</option>
              <option value="number">Number</option>
              <option value="date">Date</option>
              <option value="email">Email</option>
              <option value="phone">Phone</option>
            </select>
            <button
              type="button"
              onClick={addCustomField}
              disabled={!canAddMoreFields}
              className={`px-4 py-2 rounded-lg transition-colors flex items-center gap-2 ${
                canAddMoreFields 
                  ? 'bg-blue-600 text-white hover:bg-blue-700' 
                  : 'bg-gray-300 text-gray-500 cursor-not-allowed'
              }`}
            >
              <Plus className="w-4 h-4" />
              Add Field
            </button>
          </div>

          {/* Upgrade prompt when at limit */}
          {!canAddMoreFields && (
            <div className="mt-4 p-4 bg-gradient-to-r from-purple-50 to-blue-50 border border-purple-200 rounded-lg">
              <div className="flex items-start gap-3">
                <div className="w-10 h-10 bg-purple-600 rounded-lg flex items-center justify-center flex-shrink-0">
                  <Crown className="w-5 h-5 text-white" />
                </div>
                <div className="flex-1">
                  <h3 className="text-gray-900 mb-1">Custom Field Limit Reached</h3>
                  <p className="text-sm text-gray-600 mb-3">
                    You've reached the {maxFields} custom field limit on the {currentPlan.charAt(0).toUpperCase() + currentPlan.slice(1)} plan. 
                    {currentPlan === 'free' && ' Upgrade to Professional for 10 fields or Enterprise for 20 fields.'}
                    {currentPlan === 'professional' && ' Upgrade to Enterprise for up to 20 custom fields.'}
                  </p>
                  {currentPlan !== 'enterprise' && onNavigate && (
                    <button
                      onClick={() => onNavigate('billing')}
                      className="px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm"
                    >
                      Upgrade Now
                    </button>
                  )}
                </div>
              </div>
            </div>
          )}
        </div>

        {/* Contact Tags */}
        <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-purple-100 rounded-lg flex items-center justify-center">
              <Tag className="w-5 h-5 text-purple-600" />
            </div>
            <div>
              <h2 className="text-gray-900">Contact Tags</h2>
              <p className="text-sm text-gray-600">Manage tags to organize and categorize contacts</p>
            </div>
          </div>

          <div className="space-y-3 mb-4">
            {tags.map((tag) => (
              <div key={tag.id} className="flex items-center justify-between p-3 bg-gray-50 rounded-lg">
                <div className="flex items-center gap-3">
                  <div
                    className="w-4 h-4 rounded-full"
                    style={{ backgroundColor: tag.color }}
                  />
                  <div className="text-sm text-gray-900">{tag.name}</div>
                </div>
                <button
                  type="button"
                  onClick={() => removeTag(tag.id)}
                  className="p-2 hover:bg-red-50 text-red-600 rounded-lg transition-colors"
                >
                  <Trash2 className="w-4 h-4" />
                </button>
              </div>
            ))}
          </div>

          <div className="flex gap-2">
            <input
              type="text"
              value={newTagName}
              onChange={(e) => setNewTagName(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  e.preventDefault();
                  addTag();
                }
              }}
              placeholder="Tag name"
              className="flex-1 px-3 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
            <input
              type="color"
              value={newTagColor}
              onChange={(e) => setNewTagColor(e.target.value)}
              className="w-12 h-10 border border-gray-300 rounded-lg cursor-pointer"
            />
            <button
              type="button"
              onClick={addTag}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
            >
              <Plus className="w-4 h-4" />
              Add Tag
            </button>
          </div>
        </div>

        {/* Privacy Settings */}
        <div className="bg-white rounded-xl border border-gray-200 p-6">
          <div className="flex items-center gap-3 mb-4">
            <div className="w-10 h-10 bg-green-100 rounded-lg flex items-center justify-center">
              <Shield className="w-5 h-5 text-green-600" />
            </div>
            <div>
              <h2 className="text-gray-900">Privacy & Data</h2>
              <p className="text-sm text-gray-600">Configure data retention and privacy settings</p>
            </div>
          </div>

          <div className="space-y-4">
            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
              <div>
                <div className="text-sm text-gray-900 mb-1">Auto-delete inactive contacts</div>
                <div className="text-xs text-gray-600">
                  Automatically remove contacts with no activity after a certain period
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  className="sr-only peer" 
                  checked={autoDeleteInactive} 
                  onChange={(e) => setAutoDeleteInactive(e.target.checked)} 
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>

            <div className="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
              <div>
                <div className="text-sm text-gray-900 mb-1">Enable contact deduplication</div>
                <div className="text-xs text-gray-600">
                  Automatically detect and merge duplicate contacts
                </div>
              </div>
              <label className="relative inline-flex items-center cursor-pointer">
                <input 
                  type="checkbox" 
                  className="sr-only peer" 
                  checked={enableDeduplication} 
                  onChange={(e) => setEnableDeduplication(e.target.checked)} 
                />
                <div className="w-11 h-6 bg-gray-200 peer-focus:outline-none peer-focus:ring-4 peer-focus:ring-blue-300 rounded-full peer peer-checked:after:translate-x-full peer-checked:after:border-white after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 after:border after:rounded-full after:h-5 after:w-5 after:transition-all peer-checked:bg-blue-600"></div>
              </label>
            </div>
          </div>
        </div>

        {/* Save Button */}
        <div className="flex justify-end">
          <button 
            type="button"
            onClick={() => saveChanges()} 
            className="px-6 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Save className="w-4 h-4" />
            Save Changes
          </button>
        </div>
      </div>

      {/* Save Notification */}
      {showSaveNotification && (
        <div className="fixed bottom-4 right-4 bg-green-500 text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-2 animate-fade-in">
          <div className="w-5 h-5 bg-white rounded-full flex items-center justify-center">
            <Save className="w-3 h-3 text-green-500" />
          </div>
          Changes saved successfully!
        </div>
      )}

      {/* Upgrade Prompt */}
      {showUpgradePrompt && (
        <div className="fixed bottom-4 right-4 bg-red-500 text-white px-6 py-3 rounded-lg shadow-lg flex items-center gap-2 animate-fade-in">
          <div className="w-5 h-5 bg-white rounded-full flex items-center justify-center">
            <Crown className="w-3 h-3 text-red-500" />
          </div>
          Upgrade to add more fields!
        </div>
      )}
    </div>
  );
}