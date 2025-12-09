import { useState } from 'react';
import { X, Sparkles, Wand2, ListChecks, FileText, Lightbulb, Copy, RotateCw } from 'lucide-react';

export function LLMNoteAssistant({ initialNote, onSave, onClose, enableLLM }: any) {
  const [noteText, setNoteText] = useState(initialNote || '');
  const [aiSuggestion, setAiSuggestion] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [showAISuggestion, setShowAISuggestion] = useState(false);
  const [selectedAction, setSelectedAction] = useState<string | null>(null);

  const aiActions = [
    {
      id: 'improve',
      label: 'Improve Writing',
      description: 'Enhance grammar, clarity, and professionalism',
      icon: Wand2,
      prompt: 'Improve the following note to be more professional and clear:',
    },
    {
      id: 'summarize',
      label: 'Summarize',
      description: 'Create a concise summary of the content',
      icon: FileText,
      prompt: 'Summarize the following note concisely:',
    },
    {
      id: 'action_items',
      label: 'Extract Action Items',
      description: 'Identify tasks and follow-ups',
      icon: ListChecks,
      prompt: 'Extract action items and follow-ups from the following note:',
    },
    {
      id: 'expand',
      label: 'Expand Details',
      description: 'Add more context and details',
      icon: Lightbulb,
      prompt: 'Expand the following note with more details and context:',
    },
  ];

  const handleAIAction = async (action: any) => {
    if (!noteText.trim()) {
      alert('Please enter some text first');
      return;
    }

    setSelectedAction(action.id);
    setIsGenerating(true);
    setShowAISuggestion(true);

    // Simulate AI processing
    setTimeout(() => {
      let suggestion = '';
      
      switch (action.id) {
        case 'improve':
          suggestion = `Enhanced version of your note:\n\n${noteText}\n\nKey discussion points covered:\n• Main objective outlined\n• Action items identified\n• Follow-up scheduled for next week\n\nNext steps: Review and confirm timeline with stakeholders.`;
          break;
        case 'summarize':
          suggestion = `Summary:\n\nBrief overview of the key points from the original note, highlighting the most important information and decisions made.`;
          break;
        case 'action_items':
          suggestion = `Action Items:\n\n☐ Review contract terms\n☐ Schedule follow-up call\n☐ Send documentation to client\n☐ Confirm timeline with team\n\nDeadlines: Next week`;
          break;
        case 'expand':
          suggestion = `Expanded version:\n\n${noteText}\n\nAdditional context: This interaction represents a significant milestone in our relationship with the client. The discussion covered multiple strategic areas including technical implementation, timeline expectations, and resource allocation.\n\nBackground: Previous conversations established the foundation for this engagement, and today's meeting solidified the next steps.\n\nConsiderations: Budget constraints, technical capabilities, and team availability all factor into the proposed timeline.`;
          break;
      }

      setAiSuggestion(suggestion);
      setIsGenerating(false);
    }, 1500);
  };

  const handleUseSuggestion = () => {
    setNoteText(aiSuggestion);
    setShowAISuggestion(false);
    setAiSuggestion('');
    setSelectedAction(null);
  };

  const handleCopySuggestion = () => {
    navigator.clipboard.writeText(aiSuggestion);
    // Could show a toast notification here
  };

  const handleRegenerate = () => {
    const action = aiActions.find(a => a.id === selectedAction);
    if (action) {
      handleAIAction(action);
    }
  };

  return (
    <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={onClose}>
      <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-4xl w-full max-h-[90vh] overflow-hidden flex flex-col">
        {/* Header */}
        <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between flex-shrink-0">
          <div className="flex items-center gap-3">
            <h2 className="text-gray-900">Edit Note</h2>
            {enableLLM && (
              <span className="px-2 py-1 bg-purple-100 text-purple-700 rounded-lg text-xs flex items-center gap-1">
                <Sparkles className="w-3 h-3" />
                AI Enabled
              </span>
            )}
          </div>
          <button onClick={onClose} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Main Content */}
        <div className="flex-1 overflow-y-auto">
          <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 p-6">
            {/* Note Editor */}
            <div className="lg:col-span-2 space-y-4">
              <div>
                <label className="block text-sm text-gray-700 mb-2">Your Note</label>
                <textarea
                  value={noteText}
                  onChange={(e) => setNoteText(e.target.value)}
                  rows={12}
                  className="w-full px-4 py-3 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent resize-none"
                  placeholder="Add your notes here...&#10;&#10;Tip: Use the AI assistant to improve, summarize, or extract action items from your notes."
                />
              </div>

              {/* AI Suggestion Display */}
              {showAISuggestion && (
                <div className="bg-purple-50 border-2 border-purple-200 rounded-lg p-4">
                  <div className="flex items-center justify-between mb-3">
                    <div className="flex items-center gap-2">
                      <Sparkles className="w-5 h-5 text-purple-600" />
                      <span className="text-sm text-purple-900">AI Suggestion</span>
                    </div>
                    <div className="flex items-center gap-2">
                      <button
                        onClick={handleRegenerate}
                        disabled={isGenerating}
                        className="p-1.5 hover:bg-purple-100 rounded transition-colors disabled:opacity-50"
                        title="Regenerate"
                      >
                        <RotateCw className={`w-4 h-4 text-purple-700 ${isGenerating ? 'animate-spin' : ''}`} />
                      </button>
                      <button
                        onClick={handleCopySuggestion}
                        className="p-1.5 hover:bg-purple-100 rounded transition-colors"
                        title="Copy to clipboard"
                      >
                        <Copy className="w-4 h-4 text-purple-700" />
                      </button>
                    </div>
                  </div>

                  {isGenerating ? (
                    <div className="flex items-center gap-3 py-8">
                      <div className="w-6 h-6 border-3 border-purple-600 border-t-transparent rounded-full animate-spin" />
                      <span className="text-sm text-purple-700">Generating suggestion...</span>
                    </div>
                  ) : (
                    <>
                      <div className="bg-white rounded-lg p-4 mb-3 max-h-64 overflow-y-auto">
                        <pre className="text-sm text-gray-900 whitespace-pre-wrap font-sans">{aiSuggestion}</pre>
                      </div>
                      <div className="flex gap-2">
                        <button
                          onClick={handleUseSuggestion}
                          className="flex-1 px-4 py-2 bg-purple-600 text-white rounded-lg hover:bg-purple-700 transition-colors text-sm"
                        >
                          Use This Suggestion
                        </button>
                        <button
                          onClick={() => {
                            setShowAISuggestion(false);
                            setAiSuggestion('');
                            setSelectedAction(null);
                          }}
                          className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors text-sm"
                        >
                          Dismiss
                        </button>
                      </div>
                    </>
                  )}
                </div>
              )}
            </div>

            {/* AI Assistant Panel */}
            {enableLLM && (
              <div className="space-y-4">
                <div>
                  <h3 className="text-gray-900 mb-2 flex items-center gap-2">
                    <Sparkles className="w-4 h-4 text-purple-600" />
                    AI Assistant
                  </h3>
                  <p className="text-sm text-gray-600 mb-4">
                    Select an action to improve your note
                  </p>
                </div>

                <div className="space-y-2">
                  {aiActions.map((action) => {
                    const Icon = action.icon;
                    return (
                      <button
                        key={action.id}
                        onClick={() => handleAIAction(action)}
                        disabled={isGenerating || !noteText.trim()}
                        className={`w-full p-4 border-2 rounded-lg transition-all text-left ${
                          selectedAction === action.id
                            ? 'border-purple-500 bg-purple-50'
                            : 'border-gray-200 hover:border-purple-300 hover:bg-purple-50'
                        } disabled:opacity-50 disabled:cursor-not-allowed`}
                      >
                        <div className="flex items-start gap-3">
                          <div className="w-8 h-8 bg-purple-100 rounded-lg flex items-center justify-center flex-shrink-0">
                            <Icon className="w-4 h-4 text-purple-600" />
                          </div>
                          <div className="flex-1">
                            <div className="text-sm text-gray-900 mb-1">{action.label}</div>
                            <div className="text-xs text-gray-600">{action.description}</div>
                          </div>
                        </div>
                      </button>
                    );
                  })}
                </div>

                <div className="pt-4 border-t border-gray-200">
                  <div className="bg-blue-50 border border-blue-200 rounded-lg p-3">
                    <div className="text-xs text-blue-800">
                      <strong>Tip:</strong> Write your initial thoughts, then use AI to refine and organize them.
                    </div>
                  </div>
                </div>
              </div>
            )}

            {/* If LLM is disabled, show encouragement */}
            {!enableLLM && (
              <div className="bg-gray-50 border border-gray-200 rounded-lg p-6 flex flex-col items-center justify-center text-center">
                <Sparkles className="w-12 h-12 text-gray-400 mb-3" />
                <h3 className="text-gray-900 mb-2">AI Assistant Available</h3>
                <p className="text-sm text-gray-600 mb-4">
                  Enable AI features in Settings to get intelligent note suggestions
                </p>
                <button className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-white transition-colors text-sm">
                  Go to Settings
                </button>
              </div>
            )}
          </div>
        </div>

        {/* Footer */}
        <div className="border-t border-gray-200 px-6 py-4 flex items-center justify-between flex-shrink-0 bg-gray-50">
          <div className="text-sm text-gray-600">
            {noteText.length} characters
          </div>
          <div className="flex items-center gap-3">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
            >
              Cancel
            </button>
            <button
              onClick={() => onSave({ text: noteText })}
              disabled={!noteText.trim()}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
            >
              Save Note
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}
