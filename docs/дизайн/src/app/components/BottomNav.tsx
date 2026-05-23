import { Search, History, Settings } from "lucide-react";

type TabId = "search" | "timeline" | "settings";

interface BottomNavProps {
  activeTab: TabId;
  onTabChange: (tab: TabId) => void;
}

const TABS: Array<{ id: TabId; label: string; Icon: React.ComponentType<{ size?: number }> }> = [
  { id: "search", label: "Search", Icon: Search },
  { id: "timeline", label: "Timeline", Icon: History },
  { id: "settings", label: "Settings", Icon: Settings },
];

export function BottomNav({ activeTab, onTabChange }: BottomNavProps) {
  return (
    <div className="flex-none bg-[#0D0D0D] border-t border-[#1E1E1E]">
      <div className="flex h-20">
        {TABS.map(({ id, label, Icon }) => {
          const active = activeTab === id;
          return (
            <button
              key={id}
              onClick={() => onTabChange(id)}
              aria-label={label}
              className="flex-1 flex flex-col items-center justify-center gap-1 transition-colors"
            >
              <div
                className={`relative flex items-center justify-center h-8 w-16 rounded-full transition-all duration-200 ${
                  active ? "bg-[#F5A623]/20" : ""
                }`}
              >
                <Icon
                  size={24}
                  color={active ? "#F5A623" : "#B0B0B0"}
                />
              </div>
              <span
                className={`text-[12px] font-medium leading-none transition-colors ${
                  active ? "text-[#F5A623]" : "text-[#B0B0B0]"
                }`}
              >
                {label}
              </span>
            </button>
          );
        })}
      </div>
      {/* Home indicator area */}
      <div className="h-5 flex items-center justify-center pb-1">
        <div className="w-28 h-1 bg-[#3D3D3D] rounded-full" />
      </div>
    </div>
  );
}
