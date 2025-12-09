import { useState } from 'react';
import { Columns, X, GripVertical, Eye, EyeOff } from 'lucide-react';

interface Column {
  id: string;
  label: string;
  visible: boolean;
  required?: boolean;
}

interface ColumnCustomizerProps {
  columns: Column[];
  onColumnsChange: (columns: Column[]) => void;
}

export function ColumnCustomizer({ columns, onColumnsChange }: ColumnCustomizerProps) {
  const [isOpen, setIsOpen] = useState(false);
  const [localColumns, setLocalColumns] = useState(columns);

  const toggleColumn = (columnId: string) => {
    const updated = localColumns.map(col =>
      col.id === columnId && !col.required ? { ...col, visible: !col.visible } : col
    );
    setLocalColumns(updated);
  };

  const applyChanges = () => {
    onColumnsChange(localColumns);
    setIsOpen(false);
  };

  const resetToDefault = () => {
    const reset = columns.map(col => ({ ...col, visible: true }));
    setLocalColumns(reset);
  };

  const visibleCount = localColumns.filter(col => col.visible).length;

  return (
    <div className="relative">
      <button
        onClick={() => setIsOpen(!isOpen)}
        className="flex items-center gap-2 px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
      >
        <Columns className="w-4 h-4" />
        <span>Columns</span>
        <span className="px-2 py-0.5 bg-gray-100 text-gray-600 rounded text-xs">
          {visibleCount}
        </span>
      </button>

      {isOpen && (
        <>
          <div
            className="fixed inset-0 z-10"
            onClick={() => setIsOpen(false)}
          />
          <div className="absolute top-full right-0 mt-2 w-80 bg-white border border-gray-200 rounded-xl shadow-lg z-20">
            {/* Header */}
            <div className="flex items-center justify-between px-4 py-3 border-b border-gray-200">
              <div>
                <h3 className="text-gray-900">Customize Columns</h3>
                <p className="text-xs text-gray-600 mt-0.5">{visibleCount} of {localColumns.length} visible</p>
              </div>
              <button
                onClick={() => setIsOpen(false)}
                className="p-1 hover:bg-gray-100 rounded transition-colors"
              >
                <X className="w-4 h-4 text-gray-600" />
              </button>
            </div>

            {/* Columns List */}
            <div className="p-2 max-h-96 overflow-y-auto">
              {localColumns.map((column) => (
                <div
                  key={column.id}
                  className={`flex items-center gap-3 px-3 py-2.5 rounded-lg hover:bg-gray-50 transition-colors ${
                    column.required ? 'opacity-60' : 'cursor-pointer'
                  }`}
                  onClick={() => !column.required && toggleColumn(column.id)}
                >
                  <GripVertical className="w-4 h-4 text-gray-400 flex-shrink-0" />
                  <div className="flex-1">
                    <span className="text-sm text-gray-900">{column.label}</span>
                    {column.required && (
                      <span className="ml-2 text-xs text-gray-500">(Required)</span>
                    )}
                  </div>
                  {column.visible ? (
                    <Eye className="w-4 h-4 text-blue-600 flex-shrink-0" />
                  ) : (
                    <EyeOff className="w-4 h-4 text-gray-400 flex-shrink-0" />
                  )}
                </div>
              ))}
            </div>

            {/* Footer */}
            <div className="flex items-center justify-between px-4 py-3 border-t border-gray-200 bg-gray-50 rounded-b-xl">
              <button
                onClick={resetToDefault}
                className="text-sm text-gray-600 hover:text-gray-900 transition-colors"
              >
                Reset to Default
              </button>
              <button
                onClick={applyChanges}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors text-sm"
              >
                Apply
              </button>
            </div>
          </div>
        </>
      )}
    </div>
  );
}
