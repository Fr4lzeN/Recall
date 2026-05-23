export interface MediaItem {
  id: string;
  displayName: string;
  date: Date;
  width: number;
  height: number;
  size: number;
  mimeType: string;
  isVideo: boolean;
  duration?: number;
  isIndexed: boolean;
  thumbnailUrl: string;
  fullUrl: string;
  tags: string[];
}

const d = (y: number, mo: number, day: number, h = 12, m = 0) =>
  new Date(y, mo - 1, day, h, m);

export const mockMediaItems: MediaItem[] = [
  {
    id: "1",
    displayName: "IMG_20260520_143201.jpg",
    date: d(2026, 5, 20, 14, 32),
    width: 4032, height: 3024, size: 4_200_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/beach11/300/300",
    fullUrl: "https://picsum.photos/seed/beach11/800/600",
    tags: ["beach", "sunset", "ocean", "sky", "water", "summer", "golden hour"],
  },
  {
    id: "2",
    displayName: "IMG_20260520_120543.jpg",
    date: d(2026, 5, 20, 12, 5),
    width: 3024, height: 4032, size: 3_800_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/city22/300/300",
    fullUrl: "https://picsum.photos/seed/city22/800/600",
    tags: ["city", "street", "urban", "architecture", "buildings", "downtown"],
  },
  {
    id: "3",
    displayName: "VID_20260520_101500.mp4",
    date: d(2026, 5, 20, 10, 15),
    width: 1920, height: 1080, size: 45_000_000,
    mimeType: "video/mp4", isVideo: true, duration: 47, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/park33/300/300",
    fullUrl: "https://picsum.photos/seed/park33/800/600",
    tags: ["park", "nature", "trees", "grass", "walk", "green"],
  },
  {
    id: "4",
    displayName: "IMG_20260520_094021.jpg",
    date: d(2026, 5, 20, 9, 40),
    width: 4032, height: 3024, size: 5_100_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: false,
    thumbnailUrl: "https://picsum.photos/seed/cafe44/300/300",
    fullUrl: "https://picsum.photos/seed/cafe44/800/600",
    tags: ["cafe", "coffee", "morning", "food", "drink", "breakfast"],
  },
  {
    id: "5",
    displayName: "IMG_20260519_185322.jpg",
    date: d(2026, 5, 19, 18, 53),
    width: 4032, height: 3024, size: 4_500_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/mountain55/300/300",
    fullUrl: "https://picsum.photos/seed/mountain55/800/600",
    tags: ["mountain", "hiking", "nature", "landscape", "view", "sky", "adventure"],
  },
  {
    id: "6",
    displayName: "IMG_20260519_154811.jpg",
    date: d(2026, 5, 19, 15, 48),
    width: 4032, height: 3024, size: 3_900_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/dog66/300/300",
    fullUrl: "https://picsum.photos/seed/dog66/800/600",
    tags: ["dog", "pet", "animal", "cute", "outdoor", "puppy"],
  },
  {
    id: "7",
    displayName: "IMG_20260519_112234.jpg",
    date: d(2026, 5, 19, 11, 22),
    width: 3024, height: 4032, size: 4_200_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/food77/300/300",
    fullUrl: "https://picsum.photos/seed/food77/800/600",
    tags: ["food", "restaurant", "lunch", "meal", "delicious", "plate"],
  },
  {
    id: "8",
    displayName: "VID_20260519_090012.mp4",
    date: d(2026, 5, 19, 9, 0),
    width: 1920, height: 1080, size: 78_000_000,
    mimeType: "video/mp4", isVideo: true, duration: 123, isIndexed: false,
    thumbnailUrl: "https://picsum.photos/seed/sunrise88/300/300",
    fullUrl: "https://picsum.photos/seed/sunrise88/800/600",
    tags: ["morning", "sunrise", "nature", "sky", "peaceful", "golden"],
  },
  {
    id: "9",
    displayName: "IMG_20260518_201045.jpg",
    date: d(2026, 5, 18, 20, 10),
    width: 4032, height: 3024, size: 5_300_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/night99/300/300",
    fullUrl: "https://picsum.photos/seed/night99/800/600",
    tags: ["night", "city", "lights", "urban", "dark", "neon", "street"],
  },
  {
    id: "10",
    displayName: "IMG_20260518_163412.jpg",
    date: d(2026, 5, 18, 16, 34),
    width: 4032, height: 3024, size: 4_100_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/flower1010/300/300",
    fullUrl: "https://picsum.photos/seed/flower1010/800/600",
    tags: ["flower", "garden", "nature", "colorful", "spring", "bloom"],
  },
  {
    id: "11",
    displayName: "IMG_20260518_141256.jpg",
    date: d(2026, 5, 18, 14, 12),
    width: 4032, height: 3024, size: 3_700_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/arch1111/300/300",
    fullUrl: "https://picsum.photos/seed/arch1111/800/600",
    tags: ["architecture", "building", "city", "design", "modern", "glass"],
  },
  {
    id: "12",
    displayName: "IMG_20260518_092837.jpg",
    date: d(2026, 5, 18, 9, 28),
    width: 4032, height: 3024, size: 4_800_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: false,
    thumbnailUrl: "https://picsum.photos/seed/cat1212/300/300",
    fullUrl: "https://picsum.photos/seed/cat1212/800/600",
    tags: ["cat", "pet", "animal", "cute", "indoor", "kitten"],
  },
  {
    id: "13",
    displayName: "IMG_20260517_175033.jpg",
    date: d(2026, 5, 17, 17, 50),
    width: 4032, height: 3024, size: 4_600_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/forest1313/300/300",
    fullUrl: "https://picsum.photos/seed/forest1313/800/600",
    tags: ["forest", "trees", "nature", "hiking", "path", "woods"],
  },
  {
    id: "14",
    displayName: "IMG_20260517_141820.jpg",
    date: d(2026, 5, 17, 14, 18),
    width: 3024, height: 4032, size: 3_500_000,
    mimeType: "image/jpeg", isVideo: false, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/portrait1414/300/300",
    fullUrl: "https://picsum.photos/seed/portrait1414/800/600",
    tags: ["portrait", "person", "smile", "outdoor", "candid"],
  },
  {
    id: "15",
    displayName: "VID_20260517_100445.mp4",
    date: d(2026, 5, 17, 10, 4),
    width: 1920, height: 1080, size: 120_000_000,
    mimeType: "video/mp4", isVideo: true, duration: 203, isIndexed: true,
    thumbnailUrl: "https://picsum.photos/seed/waterfall1515/300/300",
    fullUrl: "https://picsum.photos/seed/waterfall1515/800/600",
    tags: ["waterfall", "nature", "water", "river", "landscape", "scenic"],
  },
];

export function formatDuration(seconds: number): string {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, "0")}`;
}

export function formatFileSize(bytes: number): string {
  if (bytes >= 1_000_000_000) return `${(bytes / 1_000_000_000).toFixed(1)} GB`;
  if (bytes >= 1_000_000) return `${(bytes / 1_000_000).toFixed(1)} MB`;
  if (bytes >= 1_000) return `${Math.round(bytes / 1_000)} KB`;
  return `${bytes} B`;
}

export function formatDetailDate(date: Date): string {
  return (
    date.toLocaleDateString("en-US", { month: "short", day: "numeric", year: "numeric" }) +
    " at " +
    date.toLocaleTimeString("en-US", { hour: "numeric", minute: "2-digit" })
  );
}

export function formatDateHeader(date: Date): string {
  return date.toLocaleDateString("en-US", {
    weekday: "long",
    month: "short",
    day: "numeric",
    year: "numeric",
  });
}

export function groupByDate(items: MediaItem[]): Array<{ dateKey: string; label: string; items: MediaItem[] }> {
  const map = new Map<string, MediaItem[]>();
  for (const item of items) {
    const key = item.date.toISOString().slice(0, 10);
    if (!map.has(key)) map.set(key, []);
    map.get(key)!.push(item);
  }
  return Array.from(map.entries())
    .sort((a, b) => b[0].localeCompare(a[0]))
    .map(([key, its]) => ({
      dateKey: key,
      label: formatDateHeader(its[0].date),
      items: its.sort((a, b) => b.date.getTime() - a.date.getTime()),
    }));
}

export function semanticSearch(
  items: MediaItem[],
  query: string
): Array<MediaItem & { score: number }> {
  const words = query.toLowerCase().split(/\s+/).filter(Boolean);
  if (!words.length) return [];
  return items
    .filter((item) => item.isIndexed)
    .map((item) => {
      let score = 0;
      let matchCount = 0;
      for (const word of words) {
        for (const tag of item.tags) {
          if (tag === word) { score += 1.0; matchCount++; }
          else if (tag.includes(word) || word.includes(tag)) { score += 0.6; matchCount++; }
        }
        if (item.displayName.toLowerCase().includes(word)) score += 0.2;
      }
      const pct = Math.min(Math.round((score / words.length) * 75 + (matchCount > 0 ? 15 : 0)), 97);
      return { ...item, score: pct };
    })
    .filter((item) => item.score > 18)
    .sort((a, b) => b.score - a.score)
    .slice(0, 50);
}
