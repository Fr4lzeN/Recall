import { useState } from "react";
import { FolderX, Folder, RefreshCw } from "lucide-react";
import { TopBar } from "./TopBar";

interface FolderEntry {
  id: string;
  name: string;
  path: string;
  itemCount: number;
  included: boolean;
  thumbnails: string[];
}

const INITIAL_FOLDERS: FolderEntry[] = [
  {
    id: "f1",
    name: "DCIM/Camera",
    path: "/storage/emulated/0/DCIM/Camera",
    itemCount: 1842,
    included: true,
    thumbnails: [
      "https://picsum.photos/seed/dcim1/56/56",
      "https://picsum.photos/seed/dcim2/56/56",
      "https://picsum.photos/seed/dcim3/56/56",
      "https://picsum.photos/seed/dcim4/56/56",
    ],
  },
  {
    id: "f2",
    name: "Pictures/Screenshots",
    path: "/storage/emulated/0/Pictures/Screenshots",
    itemCount: 326,
    included: true,
    thumbnails: [
      "https://picsum.photos/seed/ss1/56/56",
      "https://picsum.photos/seed/ss2/56/56",
      "https://picsum.photos/seed/ss3/56/56",
      "https://picsum.photos/seed/ss4/56/56",
    ],
  },
  {
    id: "f3",
    name: "Downloads",
    path: "/storage/emulated/0/Download",
    itemCount: 89,
    included: false,
    thumbnails: [
      "https://picsum.photos/seed/dl1/56/56",
      "https://picsum.photos/seed/dl2/56/56",
      "https://picsum.photos/seed/dl3/56/56",
      "https://picsum.photos/seed/dl4/56/56",
    ],
  },
  {
    id: "f4",
    name: "WhatsApp/Media",
    path: "/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media",
    itemCount: 412,
    included: false,
    thumbnails: [
      "https://picsum.photos/seed/wa1/56/56",
      "https://picsum.photos/seed/wa2/56/56",
      "https://picsum.photos/seed/wa3/56/56",
      "https://picsum.photos/seed/wa4/56/56",
    ],
  },
  {
    id: "f5",
    name: "Pictures/Recall",
    path: "/storage/emulated/0/Pictures/Recall",
    itemCount: 12,
    included: true,
    thumbnails: [
      "https://picsum.photos/seed/rc1/56/56",
      "https://picsum.photos/seed/rc2/56/56",
      "https://picsum.photos/seed/rc3/56/56",
      "https://picsum.photos/seed/rc4/56/56",
    ],
  },
];

interface DirectoryExclusionScreenProps {
  onBack: () => void;
}

export function DirectoryExclusionScreen({ onBack }: DirectoryExclusionScreenProps) {
  const [folders, setFolders] = useState<FolderEntry[]>(INITIAL_FOLDERS);
  const [initialState] = useState(() => JSON.stringify(INITIAL_FOLDERS.map((f) => f.included)));
  const [showReindexDialog, setShowReindexDialog] = useState(false);
  const [showSnackbar, setShowSnackbar] = useState(false);
  const [snackbarMsg, setSnackbarMsg] = useState("");

  const hasChanges =
    JSON.stringify(folders.map((f) => f.included)) !== initialState;

  const excludedFolders = folders.filter((f) => !f.included);
  const excludedCount = excludedFolders.length;
  const skippedItems = excludedFolders.reduce((sum, f) => sum + f.itemCount, 0);

  const toggle = (id: string) => {
    setFolders((prev) =>
      prev.map((f) => (f.id === id ? { ...f, included: !f.included } : f))
    );
  };

  const showSnack = (msg: string) => {
    setSnackbarMsg(msg);
    setShowSnackbar(true);
    setTimeout(() => setShowSnackbar(false), 3000);
  };

  const handleApplyReindex = () => {
    setShowReindexDialog(true);
  };

  const handleConfirmReindex = () => {
    setShowReindexDialog(false);
    showSnack("Re-indexing started with new exclusions.");
    onBack();
  };

  const handleApplyOnly = () => {
    showSnack("Exclusions saved. Re-index from Settings when ready.");
    setTimeout(() => onBack(), 1500);
  };

  return (
    <div className="flex-1 bg-[#121212] flex flex-col overflow-hidden relative">
      <TopBar title="Excluded Folders" onBack={onBack} />

      {/* Summary card */}
      <div className="flex-none mx-4 mt-3 mb-2">
        <div
          className="rounded-xl p-4 flex items-start gap-3"
          style={{
            background: excludedCount > 0 ? "rgba(61,46,16,0.6)" : "#1C1C1C",
            border: `1px solid ${excludedCount > 0 ? "#3D2E10" : "#3D3D3D"}`,
          }}
        >
          <FolderX size={24} color="#F5A623" className="flex-none mt-0.5" />
          <div className="flex-1">
            <p className="text-[#E0E0E0] text-base font-medium">
              {excludedCount === 0
                ? "No folders excluded"
                : `${excludedCount} folder${excludedCount > 1 ? "s" : ""} excluded`}
            </p>
            <p className="text-[#B0B0B0] text-sm mt-0.5">
              {excludedCount === 0
                ? "Your entire library will be indexed."
                : `${skippedItems.toLocaleString()} items will be skipped on next index`}
            </p>
            <p className="text-[#B0B0B0]/80 text-xs mt-1">
              Changes take effect on the next indexing run.
            </p>
          </div>
        </div>
      </div>

      {/* Folder list */}
      <div className="flex-1 overflow-y-auto pb-28">
        {folders.map((folder) => {
          const excluded = !folder.included;
          return (
            <div
              key={folder.id}
              className="mx-2 mb-1 rounded-xl overflow-hidden transition-colors duration-200"
              style={{
                background: excluded ? "rgba(207,102,121,0.08)" : "transparent",
                borderLeft: excluded ? "4px solid #CF6679" : "4px solid transparent",
              }}
            >
              <div className="flex items-center gap-3 px-3 py-3 min-h-[80px]">
                {/* Thumbnail collage */}
                <div className="flex-none w-14 h-14 rounded-lg overflow-hidden relative bg-[#2A2A2A]">
                  <div className="grid grid-cols-2 gap-px w-full h-full">
                    {folder.thumbnails.slice(0, 4).map((url, i) => (
                      <img
                        key={i}
                        src={url}
                        alt=""
                        className="w-full h-full object-cover"
                        loading="lazy"
                      />
                    ))}
                  </div>
                  {excluded && (
                    <div className="absolute inset-0 bg-black/50 flex items-center justify-center">
                      <FolderX size={20} color="#CF6679" />
                    </div>
                  )}
                </div>

                {/* Text */}
                <div className="flex-1 min-w-0">
                  <p className="text-[#E0E0E0] text-base font-medium truncate">{folder.name}</p>
                  <p className="text-[#B0B0B0] text-xs truncate mt-0.5">{folder.path}</p>
                  <p className="text-[#B0B0B0] text-xs mt-0.5 font-medium">
                    {folder.itemCount.toLocaleString()} photos & videos
                  </p>
                </div>

                {/* Switch */}
                <button
                  role="switch"
                  aria-checked={folder.included}
                  aria-label={`Include ${folder.name} in indexing`}
                  onClick={() => toggle(folder.id)}
                  className={`flex-none w-12 h-6 rounded-full transition-colors duration-200 relative ${
                    folder.included ? "bg-[#F5A623]" : "bg-[#3D3D3D]"
                  }`}
                >
                  <div
                    className={`absolute top-0.5 w-5 h-5 bg-white rounded-full shadow transition-transform duration-200 ${
                      folder.included ? "translate-x-6" : "translate-x-0.5"
                    }`}
                  />
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Sticky bottom bar (shown when changes exist) */}
      {hasChanges && (
        <div
          className="absolute bottom-0 left-0 right-0 bg-[#0D0D0D] px-4 pt-3 pb-4 flex flex-col gap-2"
          style={{ boxShadow: "0 -4px 16px rgba(0,0,0,0.5)" }}
        >
          <button
            onClick={handleApplyReindex}
            className="w-full flex items-center justify-center gap-2 bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
          >
            <RefreshCw size={16} />
            Apply & Re-index
          </button>
          <button
            onClick={handleApplyOnly}
            className="w-full text-[#F5A623] py-2 text-sm font-medium active:opacity-70 transition-opacity"
          >
            Apply without re-indexing
          </button>
        </div>
      )}

      {/* Snackbar */}
      {showSnackbar && (
        <div className="absolute bottom-24 left-4 right-4 bg-[#2C2C2C] rounded-2xl px-4 py-3 z-50">
          <p className="text-[#E0E0E0] text-sm">{snackbarMsg}</p>
        </div>
      )}

      {/* Confirm dialog */}
      {showReindexDialog && (
        <div className="absolute inset-0 bg-black/70 flex items-center justify-center p-8 z-50">
          <div className="bg-[#2C2C2C] rounded-3xl p-6 w-full">
            <h2 className="text-[#E0E0E0] text-xl font-semibold mb-3">
              Re-index with new exclusions?
            </h2>
            <p className="text-[#B0B0B0] text-sm leading-5 mb-6">
              Excluded folders will be removed from the search index. This may take a while.
            </p>
            <div className="flex justify-end gap-2">
              <button
                onClick={() => setShowReindexDialog(false)}
                className="text-[#B0B0B0] px-4 py-2 text-sm font-medium"
              >
                Later
              </button>
              <button
                onClick={handleConfirmReindex}
                className="text-[#F5A623] px-4 py-2 text-sm font-medium"
              >
                Re-index
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
