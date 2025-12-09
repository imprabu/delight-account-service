import { Crown } from 'lucide-react';

interface PlanSwitcherProps {
  currentPlan: 'free' | 'professional' | 'enterprise';
  onPlanChange: (plan: 'free' | 'professional' | 'enterprise') => void;
}

export function PlanSwitcher({ currentPlan, onPlanChange }: PlanSwitcherProps) {
  return (
    <div className="fixed bottom-4 left-4 md:left-auto md:right-4 bg-white rounded-lg shadow-xl border border-gray-200 p-2 md:p-3 z-50">
      <div className="flex items-center gap-2 mb-2">
        <Crown className="w-3.5 h-3.5 md:w-4 md:h-4 text-purple-600" />
        <span className="text-xs md:text-sm font-medium text-gray-900">Switch Plan</span>
      </div>
      <div className="flex flex-col md:flex-row items-stretch md:items-center gap-1.5 md:gap-2">
        <button
          onClick={() => onPlanChange('free')}
          className={`px-3 py-2 md:py-1.5 rounded text-xs font-medium transition-colors ${
            currentPlan === 'free'
              ? 'bg-gray-900 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          Free
        </button>
        <button
          onClick={() => onPlanChange('professional')}
          className={`px-3 py-2 md:py-1.5 rounded text-xs font-medium transition-colors ${
            currentPlan === 'professional'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          Professional
        </button>
        <button
          onClick={() => onPlanChange('enterprise')}
          className={`px-3 py-2 md:py-1.5 rounded text-xs font-medium transition-colors ${
            currentPlan === 'enterprise'
              ? 'bg-purple-600 text-white'
              : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
          }`}
        >
          Enterprise
        </button>
      </div>
    </div>
  );
}
