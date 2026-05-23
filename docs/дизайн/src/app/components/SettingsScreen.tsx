import { useState } from "react";
import {
  RefreshCw,
  SlidersHorizontal,
  Bot,
  Cpu,
  Smartphone,
  HardDrive,
  FolderX,
  Info,
  Trash2,
  ChevronRight,
} from "lucide-react";
import { TopBar } from "./TopBar";
import { mockMediaItems } from "./mockData";

interface SettingsScreenProps {
  onNavigateToExclusions: () => void;
}

const indexedCount = mockMediaItems.filter((m) => m.isIndexed).length;
const totalCount = mockMediaItems.length;
const isIndexing = true;
const excludedCount = 2;
const skippedItems = 501;

export function SettingsScreen({ onNavigateToExclusions }: SettingsScreenProps) {
  const [showReindexDialog, setShowReindexDialog] = useState(false);
  const [showClearDialog, setShowClearDialog] = useState(false);
  const [isRunningIndex, setIsRunningIndex] = useState(false);

  const handleReindex = () => {
    setShowReindexDialog(false);
    setIsRunningIndex(true);
    setTimeout(() => setIsRunningIndex(false), 3000);
  };

  const handleClear = () => {
    setShowClearDialog(false);
  };

  const pct = Math.round((indexedCount / totalCount) * 100);

  return (
    <div className="flex-1 bg-[#121212] flex flex-col overflow-hidden relative">
      <TopBar title="Settings" />

      <div className="flex-1 overflow-y-auto">
        {/* ── Indexing Status ── */}
        <SectionHeader label="Indexing Status" />
        <div className="px-4 pb-2">
          <div className="flex items-start gap-4 py-2">
            <RefreshCw size={20} color="#B0B0B0" className="mt-0.5 flex-none" />
            <div className="flex-1">
              <p className="text-[#E0E0E0] text-sm font-medium">
                {indexedCount} of {totalCount} items indexed
              </p>
              <div className="mt-2 h-1 bg-[#3D3D3D] rounded-full overflow-hidden">
                {isRunningIndex ? (
                  <div className="h-full bg-[#F5A623] animate-pulse w-3/4" />
                ) : (
                  <div
                    className="h-full bg-[#F5A623] rounded-full transition-all duration-500"
                    style={{ width: `${pct}%` }}
                  />
                )}
              </div>
              <p className="text-[#B0B0B0] text-xs mt-1">
                {isRunningIndex
                  ? "Indexing in progress…"
                  : isIndexing
                  ? `${pct}% complete`
                  : "Scan your library to begin indexing"}
              </p>
            </div>
          </div>
        </div>

        <Divider />

        {/* ── Model Profile ── */}
        <SectionHeader label="Model Profile" />
        <div className="px-4 pb-2 flex flex-col gap-3">
          <SettingsRow
            icon={<SlidersHorizontal size={20} color="#B0B0B0" />}
            headline="CLIP ViT-B/32"
            supporting="512 dimensions · INT8"
          />
          <SettingsRow
            icon={<Bot size={20} color="#B0B0B0" />}
            headline="Model file"
            supporting="clip_vit_b32_int8.ort"
          />
        </div>

        <Divider />

        {/* ── Device Info ── */}
        <SectionHeader label="Device Info" />
        <div className="px-4 pb-2 flex flex-col gap-3">
          <SettingsRow
            icon={<Cpu size={20} color="#B0B0B0" />}
            headline="RAM"
            supporting="3,412 MB available / 8,192 MB total"
          />
          <SettingsRow
            icon={<Cpu size={20} color="#B0B0B0" />}
            headline="CPU cores"
            supporting="8"
          />
          <SettingsRow
            icon={<Smartphone size={20} color="#B0B0B0" />}
            headline="Android API"
            supporting="API 34"
          />
          <SettingsRow
            icon={<Cpu size={20} color="#B0B0B0" />}
            headline="NNAPI"
            supporting="Supported"
          />
        </div>

        <Divider />

        {/* ── Storage ── */}
        <SectionHeader label="Storage" />
        <div className="px-4 pb-2">
          <SettingsRow
            icon={<HardDrive size={20} color="#B0B0B0" />}
            headline="Free disk"
            supporting="42.3 GB free"
          />
        </div>

        <Divider />

        {/* ── Indexing Scope (NEW) ── */}
        <SectionHeader label="Indexing Scope" />
        <div className="px-4 pb-2">
          <button
            onClick={onNavigateToExclusions}
            className="w-full flex items-center gap-4 py-3 min-h-[72px] active:bg-white/5 rounded-xl transition-colors -mx-1 px-1"
          >
            <FolderX size={20} color="#B0B0B0" className="flex-none" />
            <div className="flex-1 text-left">
              <p className="text-[#E0E0E0] text-sm font-medium">Manage Excluded Directories</p>
              <p className="text-[#B0B0B0] text-xs mt-0.5">
                {excludedCount > 0
                  ? `${excludedCount} folders excluded · ${skippedItems} items skipped`
                  : "No folders excluded — all media will be indexed."}
              </p>
            </div>
            <ChevronRight size={20} color="#B0B0B0" className="flex-none" />
          </button>
        </div>

        <Divider />

        {/* ── Actions ── */}
        <div className="px-4 py-2 flex flex-col gap-2">
          <button
            onClick={() => setShowReindexDialog(true)}
            disabled={isRunningIndex}
            className="w-full flex items-center justify-center gap-2 bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium disabled:opacity-40 active:opacity-80 transition-opacity"
          >
            <RefreshCw size={16} />
            Re-index All
          </button>
          <button
            onClick={() => setShowClearDialog(true)}
            disabled={isRunningIndex || indexedCount === 0}
            className="w-full flex items-center justify-center gap-2 border border-[#F5A623] text-[#F5A623] py-3 rounded-full text-sm font-medium disabled:opacity-40 active:opacity-80 transition-opacity"
          >
            <Trash2 size={16} />
            Clear Index
          </button>
        </div>

        <Divider />

        {/* ── About ── */}
        <SectionHeader label="About" />
        <div className="px-4 pb-6">
          <SettingsRow
            icon={<Info size={20} color="#B0B0B0" />}
            headline="Recall"
            supporting="Version 1.0.0"
          />
        </div>
      </div>

      {/* Re-index dialog */}
      {showReindexDialog && (
        <ConfirmDialog
          title="Re-index all photos?"
          body="This will re-scan and re-embed your entire library. It may take a while and use battery while running."
          confirmLabel="Re-index"
          onConfirm={handleReindex}
          onCancel={() => setShowReindexDialog(false)}
        />
      )}

      {/* Clear dialog */}
      {showClearDialog && (
        <ConfirmDialog
          title="Clear search index?"
          body="This removes all embeddings from the index. Your photos stay on device, but search won't work until indexing runs again."
          confirmLabel="Clear"
          onConfirm={handleClear}
          onCancel={() => setShowClearDialog(false)}
        />
      )}
    </div>
  );
}

function SectionHeader({ label }: { label: string }) {
  return (
    <div className="px-4 pt-4 pb-2">
      <span className="text-[#F5A623] text-xs font-medium uppercase tracking-wide">{label}</span>
    </div>
  );
}

function Divider() {
  return <div className="mx-4 my-1 h-px bg-[#3D3D3D]" />;
}

function SettingsRow({
  icon,
  headline,
  supporting,
}: {
  icon: React.ReactNode;
  headline: string;
  supporting: string;
}) {
  return (
    <div className="flex items-start gap-4 py-1">
      <div className="flex-none mt-0.5">{icon}</div>
      <div className="flex-1">
        <p className="text-[#E0E0E0] text-sm font-medium">{headline}</p>
        <p className="text-[#B0B0B0] text-xs mt-0.5">{supporting}</p>
      </div>
    </div>
  );
}

function ConfirmDialog({
  title,
  body,
  confirmLabel,
  onConfirm,
  onCancel,
}: {
  title: string;
  body: string;
  confirmLabel: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    <div className="absolute inset-0 bg-black/70 flex items-center justify-center p-8 z-50">
      <div className="bg-[#2C2C2C] rounded-3xl p-6 w-full">
        <h2 className="text-[#E0E0E0] text-xl font-semibold mb-3">{title}</h2>
        <p className="text-[#B0B0B0] text-sm leading-5 mb-6">{body}</p>
        <div className="flex justify-end gap-2">
          <button onClick={onCancel} className="text-[#B0B0B0] px-4 py-2 text-sm font-medium">
            Cancel
          </button>
          <button onClick={onConfirm} className="text-[#F5A623] px-4 py-2 text-sm font-medium">
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  );
}
