import React, { useState } from 'react';
import { Driver, DocumentVerification } from '../../types';
import {
  FileText,
  CheckCircle,
  XCircle,
  AlertTriangle,
  ZoomIn,
  ZoomOut,
  RotateCw,
  ExternalLink,
  ShieldCheck,
  Check,
  X,
  FileCheck,
  Clock,
  Phone,
  Mail,
  UserCheck
} from 'lucide-react';

interface DriverKycModalProps {
  driver: Driver;
  onClose: () => void;
  onApproveDocument: (driverId: string, docId: string) => void;
  onRejectDocument: (driverId: string, docId: string, reason: string) => void;
  onApproveAllDocuments: (driverId: string) => void;
}

export const DriverKycModal: React.FC<DriverKycModalProps> = ({
  driver,
  onClose,
  onApproveDocument,
  onRejectDocument,
  onApproveAllDocuments,
}) => {
  // Lightbox zoom & rotate state
  const [lightboxDoc, setLightboxDoc] = useState<{ title: string; url: string; docNumber: string; type: string } | null>(null);
  const [zoomLevel, setZoomLevel] = useState(1);
  const [rotation, setRotation] = useState(0);

  // Inline rejection state
  const [rejectingDocId, setRejectingDocId] = useState<string | null>(null);
  const [rejectReasonText, setRejectReasonText] = useState<string>('Document image illegible or blurry');
  const [customReason, setCustomReason] = useState('');

  // Local optimistic overrides to guarantee zero latency feedback
  const [optimisticDocStatus, setOptimisticDocStatus] = useState<Record<string, { status: 'verified' | 'rejected' | 'pending'; reason?: string }>>({});

  const rawDocs = driver.documents || [];
  const docs = rawDocs.map((doc) => {
    const override = optimisticDocStatus[doc.id] || optimisticDocStatus[doc.docType || ''] || optimisticDocStatus[doc.type || ''];
    if (override) {
      return {
        ...doc,
        status: override.status,
        rejectionReason: override.reason !== undefined ? override.reason : doc.rejectionReason,
      };
    }
    return doc;
  });

  const pendingDocsCount = docs.filter(d => d.status === 'pending').length;
  const verifiedDocsCount = docs.filter(d => d.status === 'verified').length;

  const handleOpenLightbox = (doc: DocumentVerification) => {
    setLightboxDoc({
      title: doc.title,
      url: doc.fileUrl || 'https://images.unsplash.com/photo-1628155930542-3c7a64e2c833?w=800&auto=format&fit=crop&q=80',
      docNumber: doc.documentNumber || 'DL-8B15-992',
      type: doc.type || 'Document'
    });
    setZoomLevel(1);
    setRotation(0);
  };

  const handleApproveSingleDoc = (docId: string) => {
    setOptimisticDocStatus((prev) => ({
      ...prev,
      [docId]: { status: 'verified', reason: '' }
    }));
    onApproveDocument(driver.id, docId);
  };

  const handleConfirmRejection = (docId: string) => {
    const finalReason = rejectReasonText === 'Custom' ? (customReason.trim() || 'Document image illegible or unverified') : rejectReasonText;
    setOptimisticDocStatus((prev) => ({
      ...prev,
      [docId]: { status: 'rejected', reason: finalReason }
    }));
    onRejectDocument(driver.id, docId, finalReason);
    setRejectingDocId(null);
    setCustomReason('');
  };

  const handleApproveAll = () => {
    const newOverrides: Record<string, { status: 'verified'; reason?: string }> = {};
    docs.forEach((d) => {
      newOverrides[d.id] = { status: 'verified', reason: '' };
    });
    setOptimisticDocStatus(newOverrides);
    onApproveAllDocuments(driver.id);
  };

  return (
    <div className="fixed inset-0 bg-slate-900/60 backdrop-blur-xs flex items-center justify-center p-4 z-50 animate-fadeIn overflow-y-auto">
      <div className="bg-white rounded-3xl max-w-2xl w-full p-6 shadow-2xl space-y-4 border border-slate-200 my-8">
        {/* Header */}
        <div className="flex items-start justify-between border-b border-slate-100 pb-3.5">
          <div className="flex items-center space-x-3.5">
            <img
              src={driver.avatar}
              alt={driver.fullName}
              className="w-12 h-12 rounded-2xl object-cover border-2 border-slate-200 shadow-xs shrink-0"
            />
            <div>
              <div className="flex items-center gap-2">
                <h3 className="text-base font-black text-slate-900">{driver.fullName}</h3>
                <span className={`text-[10px] px-2.5 py-0.5 rounded-full font-bold uppercase ${
                  pendingDocsCount > 0 ? 'bg-amber-100 text-amber-800 border border-amber-300 animate-pulse' : 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                }`}>
                  {pendingDocsCount > 0 ? `${pendingDocsCount} Pending Review` : 'Verified KYC'}
                </span>
              </div>
              <p className="text-xs text-slate-500 font-mono mt-0.5">
                {driver.vehicle.make} {driver.vehicle.model} • <span className="font-bold text-slate-800">{driver.vehicle.licensePlate}</span>
              </p>
            </div>
          </div>

          <div className="flex items-center space-x-2">
            {pendingDocsCount > 0 && (
              <button
                onClick={handleApproveAll}
                className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl shadow-xs transition-colors flex items-center gap-1 cursor-pointer"
              >
                <CheckCircle className="w-3.5 h-3.5" />
                Approve All ({pendingDocsCount})
              </button>
            )}
            <button
              onClick={onClose}
              className="text-slate-400 hover:text-slate-700 p-1.5 rounded-xl hover:bg-slate-100 transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Informational Guidance Banner */}
        <div className="bg-blue-50/70 p-3 rounded-2xl border border-blue-200/80 flex items-center justify-between text-xs text-blue-900">
          <div className="flex items-center space-x-2">
            <ShieldCheck className="w-4 h-4 text-blue-600 shrink-0" />
            <span>
              Verify uploaded driver credentials (CNIC, Driving License, Registration, Inspection) against regional transport regulations.
            </span>
          </div>
          <span className="text-[10px] bg-blue-100 text-blue-800 font-bold px-2 py-0.5 rounded-md font-mono shrink-0 ml-2">
            {verifiedDocsCount} / {docs.length} Approved
          </span>
        </div>

        {/* Document Cards List */}
        <div className="space-y-3.5 max-h-[55vh] overflow-y-auto pr-1">
          {docs.length === 0 && (
            <div className="text-center py-8 text-slate-400 text-xs">
              No document uploads submitted yet for this driver.
            </div>
          )}

          {docs.map((doc) => {
            const isRejecting = rejectingDocId === doc.id;
            const hasImage = Boolean(doc.fileUrl);

            return (
              <div
                key={doc.id}
                className="p-4 bg-slate-50 border border-slate-200/90 rounded-2xl space-y-3 transition-all hover:border-slate-300"
              >
                {/* Doc Header Row */}
                <div className="flex items-center justify-between">
                  <div className="flex items-center space-x-2.5">
                    <div className="p-2 bg-blue-100/80 text-blue-700 rounded-xl">
                      <FileText className="w-4 h-4" />
                    </div>
                    <div>
                      <h4 className="font-extrabold text-xs text-slate-900">{doc.title}</h4>
                      <p className="text-[10px] text-slate-500 font-mono">
                        Doc #{doc.documentNumber || 'DL-8B15-992'} • <span className="uppercase font-bold text-slate-700">{String(doc.type || '').replace(/_/g, ' ')}</span>
                      </p>
                    </div>
                  </div>

                  <span className={`text-[10px] font-extrabold px-2.5 py-0.5 rounded-full uppercase tracking-wider ${
                    doc.status === 'verified'
                      ? 'bg-emerald-100 text-emerald-800 border border-emerald-300'
                      : doc.status === 'rejected'
                      ? 'bg-rose-100 text-rose-800 border border-rose-300'
                      : 'bg-amber-100 text-amber-800 border border-amber-300 animate-pulse'
                  }`}>
                    {doc.status}
                  </span>
                </div>

                {/* Metadata Row */}
                <div className="grid grid-cols-3 gap-2 text-[11px] bg-white p-2.5 rounded-xl border border-slate-100">
                  <div>
                    <span className="text-slate-400 block text-[10px]">Issue Date</span>
                    <span className="font-semibold text-slate-700">{doc.issueDate || '2023-01-15'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px]">Expiry Date</span>
                    <span className="font-semibold text-slate-700">{doc.expiryDate || '2028-01-15'}</span>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[10px]">Last Review</span>
                    <span className="font-semibold text-slate-700">{doc.lastReviewedAt || 'Submitted'}</span>
                  </div>
                </div>

                {/* Rejection Alert if Rejected */}
                {doc.status === 'rejected' && doc.rejectionReason && (
                  <div className="p-2.5 bg-rose-50 border border-rose-200 rounded-xl text-xs text-rose-800 flex items-start space-x-2">
                    <AlertTriangle className="w-4 h-4 text-rose-600 shrink-0 mt-0.5" />
                    <div>
                      <span className="font-bold">Rejection Reason:</span> {doc.rejectionReason}
                    </div>
                  </div>
                )}

                {/* Document Preview Box */}
                <div className="relative group bg-slate-900 rounded-xl overflow-hidden border border-slate-800 h-36 flex items-center justify-center">
                  {hasImage ? (
                    <img
                      src={doc.fileUrl}
                      alt={doc.title}
                      className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-300 opacity-90"
                    />
                  ) : (
                    <div className="text-center p-3 space-y-1">
                      <FileCheck className="w-6 h-6 text-blue-400 mx-auto" />
                      <p className="text-xs text-slate-300 font-mono">Document Record Attached</p>
                    </div>
                  )}

                  {/* Hover Overlay */}
                  <div className="absolute inset-0 bg-slate-900/60 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center space-x-2.5 p-3">
                    <button
                      onClick={() => handleOpenLightbox(doc)}
                      className="px-3 py-1.5 bg-white text-slate-900 font-bold text-xs rounded-xl shadow-lg hover:bg-slate-100 flex items-center gap-1 transition-all cursor-pointer"
                    >
                      <ZoomIn className="w-3.5 h-3.5 text-blue-600" />
                      Zoom Lightbox
                    </button>
                    {doc.fileUrl && (
                      <a
                        href={doc.fileUrl}
                        target="_blank"
                        rel="noopener noreferrer"
                        className="px-3 py-1.5 bg-slate-800 text-white font-bold text-xs rounded-xl shadow-lg hover:bg-slate-700 flex items-center gap-1 transition-all"
                      >
                        <ExternalLink className="w-3.5 h-3.5" />
                        Open File
                      </a>
                    )}
                  </div>
                </div>

                {/* Doc Verification Action Buttons */}
                <div className="flex items-center justify-between pt-1">
                  <span className="text-[10px] text-slate-400 font-mono">ID: {doc.id}</span>

                  {!isRejecting ? (
                    <div className="flex items-center space-x-2">
                      {doc.status !== 'verified' && (
                        <button
                          onClick={() => handleApproveSingleDoc(doc.id)}
                          className="px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white font-bold text-xs rounded-xl transition-all shadow-2xs flex items-center gap-1 cursor-pointer"
                        >
                          <Check className="w-3.5 h-3.5" />
                          Approve Doc
                        </button>
                      )}

                      {doc.status !== 'rejected' && (
                        <button
                          onClick={() => setRejectingDocId(doc.id)}
                          className="px-3 py-1.5 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 font-bold text-xs rounded-xl transition-all flex items-center gap-1 cursor-pointer"
                        >
                          <X className="w-3.5 h-3.5" />
                          Reject
                        </button>
                      )}
                    </div>
                  ) : (
                    <div className="w-full bg-rose-50 p-3 rounded-2xl border border-rose-200 space-y-2 animate-fadeIn">
                      <label className="block text-xs font-bold text-rose-900">
                        Reason for rejecting {doc.title}:
                      </label>
                      <select
                        value={rejectReasonText}
                        onChange={(e) => setRejectReasonText(e.target.value)}
                        className="w-full p-2 bg-white border border-rose-300 rounded-xl text-xs font-medium focus:ring-2 focus:ring-rose-500 focus:outline-none"
                      >
                        <option value="Document image illegible or blurry">Document image illegible or blurry</option>
                        <option value="Expired driver license or permit">Expired driver license or permit</option>
                        <option value="Vehicle registration plate mismatch">Vehicle registration plate mismatch</option>
                        <option value="Commercial insurance policy expired">Commercial insurance policy expired</option>
                        <option value="Vehicle inspection certificate missing or failed">Vehicle inspection certificate missing or failed</option>
                        <option value="Custom">Other custom reason...</option>
                      </select>

                      {rejectReasonText === 'Custom' && (
                        <input
                          type="text"
                          value={customReason}
                          placeholder="Enter specific audit note or rejection reason..."
                          onChange={(e) => setCustomReason(e.target.value)}
                          className="w-full p-2 bg-white border border-rose-300 rounded-xl text-xs"
                        />
                      )}

                      <div className="flex justify-end space-x-2 pt-1">
                        <button
                          onClick={() => setRejectingDocId(null)}
                          className="px-3 py-1.5 bg-slate-200 hover:bg-slate-300 text-slate-700 font-bold text-xs rounded-xl"
                        >
                          Cancel
                        </button>
                        <button
                          onClick={() => handleConfirmRejection(doc.id)}
                          className="px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white font-bold text-xs rounded-xl shadow-2xs"
                        >
                          Confirm Rejection
                        </button>
                      </div>
                    </div>
                  )}
                </div>
              </div>
            );
          })}
        </div>

        {/* Footer */}
        <div className="flex items-center justify-between pt-3 border-t border-slate-100 text-xs">
          <span className="text-slate-400 font-mono text-[11px]">
            Realtime Firebase sync: <span className="text-slate-700 font-bold">{driver.id}</span>
          </span>
          <button
            onClick={onClose}
            className="px-4 py-2 bg-slate-900 hover:bg-slate-800 text-white font-bold rounded-xl transition-all"
          >
            Close KYC Inspector
          </button>
        </div>
      </div>

      {/* Enlarged Lightbox Modal with Zoom & Rotate */}
      {lightboxDoc && (
        <div className="fixed inset-0 bg-slate-950/85 backdrop-blur-md flex items-center justify-center p-4 z-60 animate-fadeIn">
          <div className="bg-slate-900 text-white rounded-3xl max-w-3xl w-full p-5 shadow-2xl space-y-4 border border-slate-800">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <div>
                <h3 className="font-black text-sm text-white flex items-center gap-2">
                  <FileText className="w-4 h-4 text-blue-400" />
                  {lightboxDoc.title}
                </h3>
                <p className="text-xs text-slate-400 font-mono">Doc #{lightboxDoc.docNumber}</p>
              </div>

              {/* Lightbox Controls */}
              <div className="flex items-center space-x-2">
                <button
                  onClick={() => setZoomLevel(prev => Math.max(0.6, prev - 0.2))}
                  className="p-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-xs"
                  title="Zoom Out"
                >
                  <ZoomOut className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setZoomLevel(prev => Math.min(2.5, prev + 0.2))}
                  className="p-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-xs"
                  title="Zoom In"
                >
                  <ZoomIn className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setRotation(prev => (prev + 90) % 360)}
                  className="p-1.5 bg-slate-800 hover:bg-slate-700 text-white rounded-lg text-xs"
                  title="Rotate 90 deg"
                >
                  <RotateCw className="w-4 h-4" />
                </button>
                <button
                  onClick={() => setLightboxDoc(null)}
                  className="text-slate-400 hover:text-white text-2xl font-bold px-2 py-0.5 rounded-lg"
                >
                  ×
                </button>
              </div>
            </div>

            {/* Image Canvas */}
            <div className="max-h-[65vh] overflow-hidden rounded-2xl border border-slate-800 bg-slate-950 flex items-center justify-center p-4">
              <img
                src={lightboxDoc.url}
                alt={lightboxDoc.title}
                style={{
                  transform: `scale(${zoomLevel}) rotate(${rotation}deg)`,
                  transition: 'transform 0.2s ease-in-out'
                }}
                className="max-w-full max-h-[55vh] object-contain rounded-lg"
              />
            </div>

            <div className="flex justify-between items-center text-xs pt-1">
              <span className="text-slate-400 text-[11px]">Zoom: {Math.round(zoomLevel * 100)}% • Rotation: {rotation}°</span>
              <button
                onClick={() => setLightboxDoc(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-700 text-white font-bold rounded-xl"
              >
                Close Viewer
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
