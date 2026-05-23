import { useState, useEffect, useRef } from "react";
import { Search, X, Image, ImageOff } from "lucide-react";
import { EmptyState, ErrorState, LoadingState } from "./EmptyState";
import { mockMediaItems, semanticSearch } from "./mockData";

interface SearchScreenProps {
  onNavigateToDetail: (id: string) => void;
}

const indexedCount = mockMediaItems.filter((m) => m.isIndexed).length;
const totalCount = mockMediaItems.length;

export function SearchScreen({ onNavigateToDetail }: SearchScreenProps) {
  const [query, setQuery] = useState("");
  const [debouncedQuery, setDebouncedQuery] = useState("");
  const [isSearching, setIsSearching] = useState(false);
  const [results, setResults] = useState<ReturnType<typeof semanticSearch>>([]);
  const [error, setError] = useState<string | null>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Debounce
  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query), 300);
    return () => clearTimeout(t);
  }, [query]);

  // Search
  useEffect(() => {
    if (!debouncedQuery.trim()) {
      setResults([]);
      setIsSearching(false);
      setError(null);
      return;
    }
    setIsSearching(true);
    setError(null);
    const t = setTimeout(() => {
      const found = semanticSearch(mockMediaItems, debouncedQuery);
      setResults(found);
      setIsSearching(false);
    }, 500);
    return () => clearTimeout(t);
  }, [debouncedQuery]);

  const blankQuery = !query.trim();

  const renderContent = () => {
    if (error) {
      return (
        <ErrorState
          title="Search failed"
          description={error}
          onRetry={() => {
            setError(null);
            setDebouncedQuery(query);
          }}
        />
      );
    }

    if (blankQuery && indexedCount === 0 && totalCount > 0) {
      return (
        <EmptyState
          icon={<Image size={64} />}
          title="Indexing not started"
          description="Your photos haven't been indexed yet. Go to Settings to start."
          animateIcon={false}
        />
      );
    }

    if (blankQuery) {
      const description =
        totalCount > 0
          ? `${indexedCount} of ${totalCount} photos indexed. Describe a photo or moment — Recall will find matching images in your library.`
          : "Describe a photo or moment — Recall will find matching images once your library is indexed.";
      return (
        <EmptyState
          icon={<Search size={64} />}
          title="Search your memories"
          description={description}
          animateIcon
        />
      );
    }

    if (isSearching) return <LoadingState />;

    if (results.length === 0) {
      return (
        <EmptyState
          icon={<ImageOff size={64} />}
          title="No photos match your search"
          description="Try a different description — Recall matches photos by meaning, not exact filenames."
          animateIcon={false}
        />
      );
    }

    return (
      <div className="flex-1 overflow-y-auto p-2">
        <div
          className="grid gap-1"
          style={{ gridTemplateColumns: "repeat(3, 1fr)" }}
        >
          {results.map((item) => (
            <button
              key={item.id}
              onClick={() => onNavigateToDetail(item.id)}
              className="relative aspect-square rounded-lg overflow-hidden bg-[#2A2A2A]/40 active:opacity-80 transition-opacity"
              aria-label={`${item.displayName}, ${item.score}% match`}
            >
              <img
                src={item.thumbnailUrl}
                alt={item.displayName}
                className="w-full h-full object-cover"
                loading="lazy"
              />
              {/* Score badge */}
              <div className="absolute top-1.5 right-1.5 bg-[#F5A623]/90 px-1.5 py-0.5 rounded text-[#1A1A1A] text-[11px] font-medium leading-none">
                {item.score}%
              </div>
            </button>
          ))}
        </div>
      </div>
    );
  };

  return (
    <div className="flex-1 bg-[#121212] flex flex-col overflow-hidden">
      {/* Search bar */}
      <div className="flex-none px-4 pt-3 pb-3">
        <div
          className="flex items-center gap-3 px-4 h-14 rounded-2xl border bg-[#0D0D0D] transition-colors"
          style={{ borderColor: query ? "#F5A623" : "#3D3D3D" }}
        >
          <Search size={24} color="#B0B0B0" />
          <input
            ref={inputRef}
            type="search"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="Search your memories…"
            className="flex-1 bg-transparent text-[#E8E8E8] text-base outline-none placeholder:text-[#B0B0B0]"
            aria-label="Search query"
          />
          {query && (
            <button
              onClick={() => setQuery("")}
              className="text-[#B0B0B0] active:text-[#E0E0E0] transition-colors"
              aria-label="Clear search"
            >
              <X size={20} />
            </button>
          )}
        </div>
      </div>

      {/* Content */}
      <div className="flex-1 flex flex-col overflow-hidden">{renderContent()}</div>
    </div>
  );
}
