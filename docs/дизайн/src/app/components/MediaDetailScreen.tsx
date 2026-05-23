import { Play } from "lucide-react";
import { TopBar } from "./TopBar";
import { LoadingState } from "./EmptyState";
import { mockMediaItems, formatDuration, formatFileSize, formatDetailDate } from "./mockData";

interface MediaDetailScreenProps {
  mediaId: string;
  onBack: () => void;
}

export function MediaDetailScreen({ mediaId, onBack }: MediaDetailScreenProps) {
  const item = mockMediaItems.find((m) => m.id === mediaId);

  if (!item) {
    return (
      <div className="flex-1 bg-[#121212] flex flex-col">
        <TopBar title="Media" onBack={onBack} />
        <LoadingState />
      </div>
    );
  }

  return (
    <div className="flex-1 bg-[#121212] flex flex-col overflow-hidden">
      <TopBar title={item.displayName} onBack={onBack} />

      {/* Preview */}
      <div className="flex-1 bg-[#0D0D0D] relative flex items-center justify-center overflow-hidden">
        <img
          src={item.fullUrl}
          alt={`${item.isVideo ? "Video" : "Photo"} preview: ${item.displayName}`}
          className="w-full h-full object-contain"
        />
        {item.isVideo && (
          <div className="absolute inset-0 flex items-center justify-center">
            <div className="bg-black/50 rounded-full p-6">
              <Play size={40} color="rgba(224,224,224,0.7)" fill="rgba(224,224,224,0.7)" />
            </div>
          </div>
        )}
      </div>

      {/* Metadata panel */}
      <div className="flex-none bg-[#2C2C2C] px-4 py-4 flex flex-col gap-3">
        {/* Indexing badge */}
        <div className="flex items-center gap-2">
          <div
            className={`px-3 py-1 rounded-full text-xs font-medium ${
              item.isIndexed
                ? "bg-[#3D2E10] text-[#F5D9A0]"
                : "bg-[#3D2A1F] text-[#F0D4C0]"
            }`}
          >
            {item.isIndexed ? "Indexed" : "Not indexed"}
          </div>
        </div>

        {/* Metadata rows */}
        <MetaRow label="Filename" value={item.displayName} />
        <MetaRow label="Date" value={formatDetailDate(item.date)} />
        <MetaRow label="Dimensions" value={`${item.width} × ${item.height}`} />
        <MetaRow label="Size" value={formatFileSize(item.size)} />
        <MetaRow label="Type" value={item.mimeType} />
        {item.isVideo && item.duration !== undefined && (
          <MetaRow label="Duration" value={formatDuration(item.duration)} />
        )}
      </div>
    </div>
  );
}

function MetaRow({ label, value }: { label: string; value: string }) {
  return (
    <div className="flex items-start gap-4">
      <span className="flex-none w-24 text-[#B0B0B0] text-[11px] font-medium uppercase tracking-wide leading-5">
        {label}
      </span>
      <span className="flex-1 text-[#E0E0E0] text-sm leading-5 break-all">{value}</span>
    </div>
  );
}
