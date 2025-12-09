import { ArrowLeft } from 'lucide-react';

interface BackToSettingsProps {
  onNavigate: (screen: string) => void;
}

export function BackToSettings({ onNavigate }: BackToSettingsProps) {
  return (
    <button
      onClick={() => onNavigate('settings')}
      className="mb-6 flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors"
    >
      <ArrowLeft className="w-4 h-4" />
      <span>Back to Settings</span>
    </button>
  );
}
