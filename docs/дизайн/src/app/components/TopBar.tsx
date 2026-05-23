import { ArrowLeft } from "lucide-react";

interface TopBarProps {
  title: string;
  onBack?: () => void;
  actions?: React.ReactNode;
}

export function TopBar({ title, onBack, actions }: TopBarProps) {
  return (
    <div className="flex-none flex items-center h-16 bg-[#0D0D0D]">
      <div className="w-12 flex-none flex justify-center">
        {onBack && (
          <button
            onClick={onBack}
            className="w-12 h-12 flex items-center justify-center text-[#E0E0E0] rounded-full active:bg-white/10 transition-colors"
            aria-label="Navigate back"
          >
            <ArrowLeft size={24} />
          </button>
        )}
      </div>
      <div className="flex-1 flex justify-center">
        <span className="text-[#E0E0E0] text-[22px] leading-7 font-medium truncate px-2">
          {title}
        </span>
      </div>
      <div className="w-12 flex-none flex justify-end pr-2">
        {actions}
      </div>
    </div>
  );
}
