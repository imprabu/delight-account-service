import { useState } from 'react';
import { Upload, Download, Trash2, File, FileText, Image, FileSpreadsheet, FileVideo, Music, Archive, X, Search, Filter, Calendar, User } from 'lucide-react';

interface Attachment {
  id: string;
  name: string;
  type: string;
  size: string;
  uploadedBy: string;
  uploadedAt: string;
  category?: string;
}

export function AttachmentsView({ entityName, entityType }: { entityName: string; entityType: 'contact' | 'account' }) {
  const [attachments, setAttachments] = useState<Attachment[]>([
    { id: '1', name: 'Contract_Agreement_2024.pdf', type: 'application/pdf', size: '2.4 MB', uploadedBy: 'John Doe', uploadedAt: '2024-01-15', category: 'Contracts' },
    { id: '2', name: 'Product_Demo_Recording.mp4', type: 'video/mp4', size: '45.2 MB', uploadedBy: 'Sarah Smith', uploadedAt: '2024-01-10', category: 'Recordings' },
    { id: '3', name: 'Meeting_Notes_Q1.docx', type: 'application/msword', size: '156 KB', uploadedBy: 'Mike Johnson', uploadedAt: '2024-01-08', category: 'Documents' },
    { id: '4', name: 'Budget_Proposal.xlsx', type: 'application/vnd.ms-excel', size: '892 KB', uploadedBy: 'Emma Wilson', uploadedAt: '2024-01-05', category: 'Spreadsheets' },
    { id: '5', name: 'Logo_Design.png', type: 'image/png', size: '3.1 MB', uploadedBy: 'John Doe', uploadedAt: '2023-12-28', category: 'Images' },
  ]);

  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('all');
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [showDeleteConfirm, setShowDeleteConfirm] = useState<string | null>(null);

  const categories = ['all', 'Contracts', 'Documents', 'Images', 'Recordings', 'Spreadsheets', 'Other'];

  const getFileIcon = (type: string) => {
    if (type.startsWith('image/')) return Image;
    if (type.startsWith('video/')) return FileVideo;
    if (type.startsWith('audio/')) return Music;
    if (type.includes('pdf')) return FileText;
    if (type.includes('spreadsheet') || type.includes('excel')) return FileSpreadsheet;
    if (type.includes('zip') || type.includes('rar')) return Archive;
    return File;
  };

  const handleDelete = (id: string) => {
    setAttachments(attachments.filter(a => a.id !== id));
    setShowDeleteConfirm(null);
  };

  const handleUpload = () => {
    // Simulate file upload
    const newAttachment: Attachment = {
      id: Date.now().toString(),
      name: 'New_Document.pdf',
      type: 'application/pdf',
      size: '1.2 MB',
      uploadedBy: 'Current User',
      uploadedAt: new Date().toISOString().split('T')[0],
      category: 'Documents',
    };
    setAttachments([newAttachment, ...attachments]);
    setShowUploadModal(false);
  };

  const filteredAttachments = attachments.filter(attachment => {
    const matchesSearch = attachment.name.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory = selectedCategory === 'all' || attachment.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const totalSize = attachments.reduce((acc, att) => {
    const size = parseFloat(att.size);
    const unit = att.size.split(' ')[1];
    if (unit === 'MB') return acc + size;
    if (unit === 'KB') return acc + size / 1024;
    if (unit === 'GB') return acc + size * 1024;
    return acc;
  }, 0);

  return (
    <div className="space-y-6">
      {/* Header */}
      <div>
        <div className="flex items-center justify-between mb-2">
          <h2 className="text-gray-900">{entityName} - Attachments</h2>
          <button
            onClick={() => setShowUploadModal(true)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
          >
            <Upload className="w-4 h-4" />
            Upload Files
          </button>
        </div>
        <p className="text-sm text-gray-600">
          {attachments.length} files • {totalSize.toFixed(2)} MB total
        </p>
      </div>

      {/* Search and Filter */}
      <div className="bg-white rounded-xl border border-gray-200 p-4">
        <div className="flex flex-col md:flex-row gap-3">
          {/* Search */}
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              placeholder="Search files..."
              className="w-full pl-10 pr-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent"
            />
          </div>

          {/* Category Filter */}
          <div className="relative">
            <Filter className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400" />
            <select
              value={selectedCategory}
              onChange={(e) => setSelectedCategory(e.target.value)}
              className="pl-10 pr-10 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent appearance-none bg-white"
            >
              {categories.map(cat => (
                <option key={cat} value={cat}>
                  {cat === 'all' ? 'All Categories' : cat}
                </option>
              ))}
            </select>
          </div>
        </div>
      </div>

      {/* Attachments Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {filteredAttachments.map((attachment) => {
          const FileIcon = getFileIcon(attachment.type);
          return (
            <div
              key={attachment.id}
              className="bg-white rounded-xl border border-gray-200 p-4 hover:shadow-lg transition-all group"
            >
              {/* File Icon & Name */}
              <div className="flex items-start gap-3 mb-3">
                <div className="w-12 h-12 bg-blue-100 rounded-lg flex items-center justify-center flex-shrink-0">
                  <FileIcon className="w-6 h-6 text-blue-600" />
                </div>
                <div className="flex-1 min-w-0">
                  <h4 className="text-sm text-gray-900 truncate mb-1">{attachment.name}</h4>
                  <div className="flex items-center gap-2">
                    <span className="text-xs text-gray-500">{attachment.size}</span>
                    {attachment.category && (
                      <>
                        <span className="text-xs text-gray-400">•</span>
                        <span className="text-xs px-2 py-0.5 bg-gray-100 text-gray-600 rounded">
                          {attachment.category}
                        </span>
                      </>
                    )}
                  </div>
                </div>
              </div>

              {/* Metadata */}
              <div className="space-y-1.5 mb-4">
                <div className="flex items-center gap-2 text-xs text-gray-600">
                  <User className="w-3.5 h-3.5" />
                  <span>{attachment.uploadedBy}</span>
                </div>
                <div className="flex items-center gap-2 text-xs text-gray-600">
                  <Calendar className="w-3.5 h-3.5" />
                  <span>{new Date(attachment.uploadedAt).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' })}</span>
                </div>
              </div>

              {/* Actions */}
              <div className="flex items-center gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                <button className="flex-1 px-3 py-1.5 bg-blue-50 text-blue-600 rounded-lg hover:bg-blue-100 transition-colors flex items-center justify-center gap-1.5 text-sm">
                  <Download className="w-3.5 h-3.5" />
                  Download
                </button>
                <button
                  onClick={() => setShowDeleteConfirm(attachment.id)}
                  className="px-3 py-1.5 bg-red-50 text-red-600 rounded-lg hover:bg-red-100 transition-colors"
                >
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Empty State */}
      {filteredAttachments.length === 0 && (
        <div className="bg-white rounded-xl border border-gray-200 p-12 text-center">
          <div className="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mx-auto mb-4">
            <File className="w-8 h-8 text-gray-400" />
          </div>
          <h3 className="text-gray-900 mb-2">No attachments found</h3>
          <p className="text-sm text-gray-600 mb-4">
            {searchQuery || selectedCategory !== 'all'
              ? 'Try adjusting your search or filter'
              : `Upload files to attach them to this ${entityType}`}
          </p>
          <button
            onClick={() => setShowUploadModal(true)}
            className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors inline-flex items-center gap-2"
          >
            <Upload className="w-4 h-4" />
            Upload Files
          </button>
        </div>
      )}

      {/* Upload Modal */}
      {showUploadModal && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={() => setShowUploadModal(false)}>
          <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-lg w-full">
            <div className="border-b border-gray-200 px-6 py-4 flex items-center justify-between">
              <h2 className="text-gray-900">Upload Attachments</h2>
              <button onClick={() => setShowUploadModal(false)} className="p-2 hover:bg-gray-100 rounded-lg transition-colors">
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="p-6">
              {/* Drag & Drop Area */}
              <div className="border-2 border-dashed border-gray-300 rounded-lg p-12 text-center hover:border-blue-500 hover:bg-blue-50 transition-all cursor-pointer">
                <Upload className="w-12 h-12 text-gray-400 mx-auto mb-4" />
                <h3 className="text-gray-900 mb-2">Drop files here or click to browse</h3>
                <p className="text-sm text-gray-600">Support for PDF, DOC, XLS, images, videos, and more</p>
              </div>

              {/* Category Selection */}
              <div className="mt-4">
                <label className="block text-sm text-gray-700 mb-2">File Category</label>
                <select className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:ring-2 focus:ring-blue-500 focus:border-transparent">
                  <option value="">Select a category...</option>
                  {categories.filter(c => c !== 'all').map(cat => (
                    <option key={cat} value={cat}>{cat}</option>
                  ))}
                </select>
              </div>
            </div>

            <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowUploadModal(false)}
                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={handleUpload}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2"
              >
                <Upload className="w-4 h-4" />
                Upload
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteConfirm && (
        <div className="fixed inset-0 bg-black bg-opacity-50 flex items-center justify-center p-4 z-50" onClick={() => setShowDeleteConfirm(null)}>
          <div onClick={(e) => e.stopPropagation()} className="bg-white rounded-xl shadow-2xl max-w-md w-full">
            <div className="border-b border-gray-200 px-6 py-4">
              <h2 className="text-gray-900">Delete Attachment</h2>
            </div>

            <div className="p-6">
              <p className="text-gray-700">
                Are you sure you want to delete this file? This action cannot be undone.
              </p>
            </div>

            <div className="bg-gray-50 border-t border-gray-200 px-6 py-4 flex items-center justify-end gap-3">
              <button
                onClick={() => setShowDeleteConfirm(null)}
                className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50 transition-colors"
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(showDeleteConfirm)}
                className="px-4 py-2 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors flex items-center gap-2"
              >
                <Trash2 className="w-4 h-4" />
                Delete
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
