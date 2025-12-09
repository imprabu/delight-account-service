import { Info } from 'lucide-react';

export function DemoBanner() {
  return (
    <div className="mb-6 p-4 bg-blue-50 border border-blue-200 rounded-lg flex items-start gap-3">
      <Info className="w-5 h-5 text-blue-600 flex-shrink-0 mt-0.5" />
      <div>
        <div className="text-blue-900 font-medium mb-1">Demo Mode</div>
        <p className="text-sm text-blue-700">
          You're viewing sample data. In production, this will show your real data and you can perform actual actions.
        </p>
      </div>
    </div>
  );
}
