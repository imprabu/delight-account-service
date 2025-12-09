import { useState } from 'react';
import { CreditCard, Check, Clock, Download, Calendar, ArrowLeft } from 'lucide-react';
import { BackToSettings } from './BackToSettings';
import { DemoBanner } from './DemoBanner';

interface BillingSettingsProps {
  onNavigate?: (screen: string) => void;
}

export function BillingSettings({ onNavigate }: BillingSettingsProps) {
  const [selectedPlan, setSelectedPlan] = useState('professional');

  const plans = [
    {
      id: 'starter',
      name: 'Starter',
      price: 29,
      description: 'Perfect for small teams getting started',
      features: [
        '500 conversations/month',
        '1 team member',
        'Email support',
        'Basic analytics',
        'Web chat channel',
      ],
    },
    {
      id: 'professional',
      name: 'Professional',
      price: 99,
      description: 'For growing businesses',
      popular: true,
      features: [
        '5,000 conversations/month',
        '5 team members',
        'Priority support',
        'Advanced analytics',
        'All channels (Web, Email, WhatsApp, SMS)',
        'Custom AI training',
        'API access',
      ],
    },
    {
      id: 'enterprise',
      name: 'Enterprise',
      price: 299,
      description: 'For large organizations',
      features: [
        'Unlimited conversations',
        'Unlimited team members',
        '24/7 dedicated support',
        'Custom analytics & reports',
        'All channels + Custom integrations',
        'Advanced AI customization',
        'Dedicated account manager',
        'SLA guarantee',
      ],
    },
  ];

  const billingHistory = [
    { id: 1, date: '2024-12-01', amount: 99, status: 'paid', invoice: 'INV-2024-12-001' },
    { id: 2, date: '2024-11-01', amount: 99, status: 'paid', invoice: 'INV-2024-11-001' },
    { id: 3, date: '2024-10-01', amount: 99, status: 'paid', invoice: 'INV-2024-10-001' },
  ];

  return (
    <div className="p-4 md:p-6 lg:p-8 max-w-6xl mx-auto">
      {/* Back Button */}
      {onNavigate && <BackToSettings onNavigate={onNavigate} />}
      <DemoBanner />
      {onNavigate && false && (
        <button
          onClick={() => onNavigate('settings')}
          className="mb-6 flex items-center gap-2 text-gray-600 hover:text-gray-900 transition-colors hidden"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Back to Settings</span>
        </button>
      )}
      
      <div className="mb-6 md:mb-8">
        <h1 className="text-gray-900 mb-2">Billing & Subscription</h1>
        <p className="text-gray-600 text-sm md:text-base">Manage your subscription plan and billing information</p>
      </div>

      {/* Current Plan */}
      <div className="bg-gradient-to-r from-blue-600 to-purple-600 rounded-xl p-4 md:p-6 mb-6 md:mb-8 text-white">
        <div className="flex flex-col md:flex-row md:items-start justify-between gap-4">
          <div className="flex-1">
            <div className="text-sm opacity-90 mb-1">Current Plan</div>
            <h2 className="text-white mb-2">Professional Plan</h2>
            <div className="text-2xl md:text-3xl mb-4">$99<span className="text-base md:text-lg opacity-90">/month</span></div>
            <div className="flex flex-col sm:flex-row sm:items-center gap-2 sm:gap-4 text-sm opacity-90">
              <div className="flex items-center gap-2">
                <Clock className="w-4 h-4" />
                Next billing: Jan 1, 2025
              </div>
              <div className="flex items-center gap-2">
                <Calendar className="w-4 h-4" />
                <span className="hidden sm:inline">2,847 / 5,000 conversations used</span>
                <span className="sm:hidden">2,847 / 5,000 used</span>
              </div>
            </div>
          </div>
          <button className="px-4 py-2 bg-white text-blue-600 rounded-lg hover:bg-blue-50 transition-colors w-full md:w-auto">
            Upgrade Plan
          </button>
        </div>
      </div>

      {/* Available Plans */}
      <div className="mb-6 md:mb-8">
        <h2 className="text-gray-900 mb-4">Available Plans</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 md:gap-6">
          {plans.map((plan) => (
            <div
              key={plan.id}
              className={`relative bg-white rounded-xl border-2 p-4 md:p-6 transition-all ${
                selectedPlan === plan.id
                  ? 'border-blue-600 shadow-lg'
                  : 'border-gray-200 hover:border-gray-300'
              }`}
            >
              {plan.popular && (
                <div className="absolute -top-3 left-1/2 -translate-x-1/2">
                  <span className="px-3 py-1 bg-blue-600 text-white text-xs rounded-full">
                    Most Popular
                  </span>
                </div>
              )}

              <div className="mb-4">
                <h3 className="text-gray-900 mb-1">{plan.name}</h3>
                <p className="text-sm text-gray-600">{plan.description}</p>
              </div>

              <div className="mb-4">
                <span className="text-3xl text-gray-900">${plan.price}</span>
                <span className="text-gray-600">/month</span>
              </div>

              <ul className="space-y-3 mb-6">
                {plan.features.map((feature, idx) => (
                  <li key={idx} className="flex items-start gap-2 text-sm text-gray-700">
                    <Check className="w-4 h-4 text-green-600 flex-shrink-0 mt-0.5" />
                    <span>{feature}</span>
                  </li>
                ))}
              </ul>

              <button
                onClick={() => setSelectedPlan(plan.id)}
                className={`w-full py-2 rounded-lg transition-colors ${
                  selectedPlan === plan.id
                    ? 'bg-blue-600 text-white'
                    : 'bg-gray-100 text-gray-900 hover:bg-gray-200'
                }`}
              >
                {selectedPlan === plan.id ? 'Current Plan' : 'Select Plan'}
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* Payment Method */}
      <div className="bg-white rounded-xl border border-gray-200 p-6 mb-8">
        <div className="flex items-center justify-between mb-4">
          <h2 className="text-gray-900">Payment Method</h2>
          <button className="px-4 py-2 bg-gray-100 text-gray-900 rounded-lg hover:bg-gray-200 transition-colors">
            Update Card
          </button>
        </div>

        <div className="flex items-center gap-4 p-4 bg-gray-50 rounded-lg">
          <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center">
            <CreditCard className="w-6 h-6 text-blue-600" />
          </div>
          <div className="flex-1">
            <div className="text-sm text-gray-900 mb-1">Visa ending in 4242</div>
            <div className="text-xs text-gray-600">Expires 12/2026</div>
          </div>
          <div className="px-3 py-1 bg-green-100 text-green-700 text-xs rounded-full">
            Active
          </div>
        </div>
      </div>

      {/* Billing History */}
      <div className="bg-white rounded-xl border border-gray-200 p-4 md:p-6">
        <h2 className="text-gray-900 mb-4">Billing History</h2>
        
        <div className="space-y-3">
          {billingHistory.map((bill) => (
            <div key={bill.id} className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 p-4 bg-gray-50 rounded-lg">
              <div className="flex items-center gap-4">
                <div className="w-10 h-10 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
                  <CreditCard className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <div className="text-sm text-gray-900">{bill.invoice}</div>
                  <div className="text-xs text-gray-600">
                    {new Date(bill.date).toLocaleDateString('en-US', { 
                      year: 'numeric', 
                      month: 'long', 
                      day: 'numeric' 
                    })}
                  </div>
                </div>
              </div>
              <div className="flex items-center gap-4 justify-between sm:justify-end">
                <div className="text-sm text-gray-900">${bill.amount}</div>
                <div className="px-3 py-1 bg-green-100 text-green-700 text-xs rounded-full">
                  {bill.status}
                </div>
                <button className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                  <Download className="w-4 h-4 text-gray-600" />
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Usage Stats */}
      <div className="mt-8 bg-blue-50 border border-blue-200 rounded-lg p-4">
        <p className="text-sm text-blue-900">
          <strong>Note:</strong> Your current usage is tracked in real-time. Upgrade your plan anytime to access more conversations and features.
        </p>
      </div>
    </div>
  );
}