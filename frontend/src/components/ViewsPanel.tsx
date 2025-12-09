import { useState } from 'react';
import { Plus, Star, Edit2, Trash2, Copy, Filter as FilterIcon, Columns, ChevronDown, MoreVertical } from 'lucide-react';

interface View {
  id: string;
  name: string;
  isDefault?: boolean;
  isPredefined?: boolean;
  filters: any;
  visibleColumns: string[];
  icon?: any;
}

interface ViewsPanelProps {
  views: View[];
  activeView: string;
  onViewChange: (viewId: string) => void;
  onCreateView: () => void;
  onEditView: (viewId: string) => void;
  onDeleteView: (viewId: string) => void;
  onDuplicateView: (viewId: string) => void;
}

export function ViewsPanel({
  views,
  activeView,
  onViewChange,
  onCreateView,
  onEditView,
  onDeleteView,
  onDuplicateView,
}: ViewsPanelProps) {
  const [showDropdown, setShowDropdown] = useState(false);
  const [menuViewId, setMenuViewId] = useState<string | null>(null);

  const currentView = views.find(v => v.id === activeView);

  return (
    <div className="relative">
      <button
        onClick={() => setShowDropdown(!showDropdown)}
        className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
      >
        {currentView?.icon && <currentView.icon className="w-4 h-4" />}
        <span className="text-gray-900">{currentView?.name || 'All Items'}</span>
        <ChevronDown className={`w-4 h-4 text-gray-500 transition-transform ${showDropdown ? 'rotate-180' : ''}`} />
      </button>

      {showDropdown && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => {
              setShowDropdown(false);
              setMenuViewId(null);
            }}
          />
          <div className="absolute top-full left-0 mt-2 w-80 bg-white border border-gray-200 rounded-xl shadow-lg z-20 py-2">
            {/* Predefined Views */}
            <div className="px-3 py-2 text-xs text-gray-500 uppercase tracking-wider">
              System Views
            </div>
            {views.filter(v => v.isPredefined).map((view) => (
              <div key={view.id} className="relative group">
                <button
                  onClick={() => {
                    onViewChange(view.id);
                    setShowDropdown(false);
                  }}
                  className={`w-full flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 transition-colors ${
                    activeView === view.id ? 'bg-blue-50' : ''
                  }`}
                >
                  {view.icon && <view.icon className="w-4 h-4 text-gray-600" />}
                  <span className={`flex-1 text-left ${activeView === view.id ? 'text-blue-700' : 'text-gray-900'}`}>
                    {view.name}
                  </span>
                  {view.isDefault && (
                    <Star className="w-4 h-4 text-yellow-500 fill-yellow-500" />
                  )}
                </button>
              </div>
            ))}

            {/* Custom Views */}
            {views.filter(v => !v.isPredefined).length > 0 && (
              <>
                <div className="h-px bg-gray-200 my-2" />
                <div className="px-3 py-2 text-xs text-gray-500 uppercase tracking-wider">
                  Custom Views
                </div>
                {views.filter(v => !v.isPredefined).map((view) => (
                  <div key={view.id} className="relative group">
                    <button
                      onClick={() => {
                        onViewChange(view.id);
                        setShowDropdown(false);
                      }}
                      className={`w-full flex items-center gap-3 px-4 py-2.5 hover:bg-gray-50 transition-colors ${
                        activeView === view.id ? 'bg-blue-50' : ''
                      }`}
                    >
                      <FilterIcon className="w-4 h-4 text-gray-600" />
                      <span className={`flex-1 text-left ${activeView === view.id ? 'text-blue-700' : 'text-gray-900'}`}>
                        {view.name}
                      </span>
                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          setMenuViewId(menuViewId === view.id ? null : view.id);
                        }}
                        className="opacity-0 group-hover:opacity-100 p-1 hover:bg-gray-200 rounded transition-all"
                      >
                        <MoreVertical className="w-4 h-4 text-gray-600" />
                      </button>
                    </button>

                    {/* View Menu */}
                    {menuViewId === view.id && (
                      <div className="absolute right-2 top-full mt-1 w-48 bg-white border border-gray-200 rounded-lg shadow-lg z-30 py-1">
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onEditView(view.id);
                            setMenuViewId(null);
                            setShowDropdown(false);
                          }}
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-50 text-sm text-gray-700"
                        >
                          <Edit2 className="w-4 h-4" />
                          Edit View
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onDuplicateView(view.id);
                            setMenuViewId(null);
                            setShowDropdown(false);
                          }}
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-50 text-sm text-gray-700"
                        >
                          <Copy className="w-4 h-4" />
                          Duplicate
                        </button>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            onDeleteView(view.id);
                            setMenuViewId(null);
                            setShowDropdown(false);
                          }}
                          className="w-full flex items-center gap-2 px-3 py-2 hover:bg-gray-50 text-sm text-red-600"
                        >
                          <Trash2 className="w-4 h-4" />
                          Delete
                        </button>
                      </div>
                    )}
                  </div>
                ))}
              </>
            )}

            {/* Create New View */}
            <div className="h-px bg-gray-200 my-2" />
            <button
              onClick={() => {
                onCreateView();
                setShowDropdown(false);
              }}
              className="w-full flex items-center gap-2 px-4 py-2.5 hover:bg-gray-50 text-blue-600 transition-colors"
            >
              <Plus className="w-4 h-4" />
              Create New View
            </button>
          </div>
        </>
      )}
    </div>
  );
}
