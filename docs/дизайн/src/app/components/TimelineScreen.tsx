import { useState } from "react";
import { Settings } from "lucide-react";
import { TopBar } from "./TopBar";
import { EmptyState } from "./EmptyState";
import { mockMediaItems, groupByDate, formatDuration } from "./mockData";
import { Play } from "lucide-react";

interface TimelineScreenProps {
  onNavigateToDetail: (id: string) => void;
}

const groups = groupByDate(mockMediaItems);
const indexedCount = mockMediaItems.filter((m) => m.isIndexed).length;
const totalCount = mockMediaItems.length;
const isIndexing = true;

export function TimelineScreen({ onNavigateToDetail }: TimelineScreenProps) {
  const [refreshing, setRefreshing] = useState(false);
  const [touchStart, setTouchStart] = useState<number | null>(null);
  const [pullDistance, setPullDistance] = useState(0);

  const handleRefresh = () => {
    setRefreshing(true);
    setTimeout(() => setRefreshing(false), 2000);
  };

  const indexingLabel =
    isIndexing && indexedCount < totalCount ? (
      <span className="text-[#F5A623] text-[11px] font-medium pr-4">
        Indexing {indexedCount}/{totalCount}
      </span>
    ) : null;

  return (
    <div className="flex-1 bg-[#121212] flex flex-col overflow-hidden">
      <TopBar title="Timeline" actions={indexingLabel} />

      {/* Indeterminate progress */}
      {isIndexing && (
        <div className="flex-none h-1 bg-[#3D3D3D] overflow-hidden">
          <div
            className="h-full bg-[#F5A623] animate-pulse"
            style={{ width: `${(indexedCount / totalCount) * 100}%` }}
          />
        </div>
      )}

      {/* Pull-to-refresh indicator */}
      {refreshing && (
        <div className="flex-none flex items-center justify-center py-2">
          <div
            className="w-6 h-6 rounded-full border-2 border-transparent animate-spin"
            style={{ borderTopColor: "#F5A623", borderRightColor: "#F5A623" }}
          />
        </div>
      )}

      {/* Content */}
      {totalCount === 0 ? (
        <EmptyState
          icon={<Settings size={64} />}
          title="No photos found"
          description="Grant access in Settings to see your library here."
          animateIcon={false}
        />
      ) : (
        <div className="flex-1 overflow-y-auto">
          {/* Pull hint */}
          <button
            onClick={handleRefresh}
            className="w-full py-2 text-[#B0B0B0] text-xs text-center"
          >
            ↓ Pull to refresh & re-index
          </button>

          {groups.map((group) => (
            <div key={group.dateKey}>
              {/* Date header */}
              <div className="px-2 py-2 col-span-full">
                <span className="text-[#E0E0E0] text-base font-medium">{group.label}</span>
              </div>

              {/* Grid */}
              <div
                className="grid gap-1 px-2 pb-2"
                style={{ gridTemplateColumns: "repeat(3, 1fr)" }}
              >
                {group.items.map((item) => (
                  <button
                    key={item.id}
                    onClick={() => onNavigateToDetail(item.id)}
                    className="relative aspect-square rounded-lg overflow-hidden bg-[#2A2A2A]/40 active:opacity-80 transition-opacity"
                    aria-label={`${item.displayName}${item.isVideo ? ", video" : ""}${!item.isIndexed ? ", indexing" : ""}`}
                  >
                    <img
                      src={item.thumbnailUrl}
                      alt={item.displayName}
                      className="w-full h-full object-cover"
                      loading="lazy"
                    />

                    {/* Video overlay */}
                    {item.isVideo && (
                      <>
                        <div className="absolute inset-0 flex items-center justify-center">
                          <div className="bg-black/40 rounded-full p-1">
                            <Play size={16} color="rgba(224,224,224,0.85)" fill="rgba(224,224,224,0.85)" />
                          </div>
                        </div>
                        {item.duration !== undefined && (
                          <div className="absolute bottom-1.5 right-1.5 bg-black/70 px-1.5 py-0.5 rounded text-[#E8E8E8] text-[11px] font-medium leading-none">
                            {formatDuration(item.duration)}
                          </div>
                        )}
                      </>
                    )}

                    {/* Indexing chip */}
                    {!item.isIndexed && (
                      <div className="absolute top-1.5 left-1.5 bg-[#E8A87C]/90 px-1.5 py-0.5 rounded text-[#1A1A1A] text-[11px] font-medium leading-none">
                        Indexing
                      </div>
                    )}
                  </button>
                ))}
              </div>
            </div>
          ))}
        </div>
      )}
    </div>
  );
}
