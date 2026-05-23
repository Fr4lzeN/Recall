import { motion } from "motion/react";
import { AlertCircle } from "lucide-react";

interface EmptyStateProps {
  icon: React.ReactNode;
  title: string;
  description: string;
  animateIcon?: boolean;
}

export function EmptyState({ icon, title, description, animateIcon = true }: EmptyStateProps) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
      <motion.div
        animate={animateIcon ? { scale: [1, 1.08, 1, 0.92, 1] } : undefined}
        transition={animateIcon ? { duration: 1.4, repeat: Infinity, ease: "easeInOut" } : undefined}
        className="mb-4 opacity-60"
        style={{ color: "#B0B0B0" }}
      >
        {icon}
      </motion.div>
      <p className="text-[#E0E0E0] text-[22px] leading-7 font-medium mb-2">{title}</p>
      <p className="text-[#B0B0B0] text-sm leading-5">{description}</p>
    </div>
  );
}

interface ErrorStateProps {
  title: string;
  description: string;
  retryLabel?: string;
  onRetry?: () => void;
}

export function ErrorState({ title, description, retryLabel = "Try again", onRetry }: ErrorStateProps) {
  return (
    <div className="flex-1 flex flex-col items-center justify-center p-8 text-center">
      <div className="mb-4">
        <AlertCircle size={64} color="#CF6679" />
      </div>
      <p className="text-[#E0E0E0] text-[22px] leading-7 font-medium mb-2">{title}</p>
      <p className="text-[#B0B0B0] text-sm leading-5 mb-6">{description}</p>
      {onRetry && (
        <button
          onClick={onRetry}
          className="bg-[#F5A623] text-[#1A1A1A] px-6 py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
        >
          {retryLabel}
        </button>
      )}
    </div>
  );
}

export function LoadingState() {
  return (
    <div className="flex-1 flex items-center justify-center">
      <div
        className="w-10 h-10 rounded-full border-2 border-transparent border-t-[#F5A623] animate-spin"
        style={{ borderTopColor: "#F5A623", borderRightColor: "#F5A623" }}
      />
    </div>
  );
}
