import { useState, useCallback } from "react";
import { AnimatePresence, motion } from "motion/react";
import { BottomNav } from "./components/BottomNav";
import { OnboardingScreen } from "./components/OnboardingScreen";
import { SearchScreen } from "./components/SearchScreen";
import { TimelineScreen } from "./components/TimelineScreen";
import { SettingsScreen } from "./components/SettingsScreen";
import { MediaDetailScreen } from "./components/MediaDetailScreen";
import { DirectoryExclusionScreen } from "./components/DirectoryExclusionScreen";

type TabId = "search" | "timeline" | "settings";
type ScreenId = TabId | "onboarding" | "detail" | "directory-exclusions";

interface NavState {
  screen: ScreenId;
  selectedMediaId?: string;
}

const TOP_LEVEL: ScreenId[] = ["search", "timeline", "settings"];

const slideUp = {
  initial: { y: "100%", opacity: 0 },
  animate: { y: 0, opacity: 1 },
  exit: { y: "100%", opacity: 0 },
};

const slideRight = {
  initial: { x: "100%", opacity: 0 },
  animate: { x: 0, opacity: 1 },
  exit: { x: "100%", opacity: 0 },
};

const fade = {
  initial: { opacity: 0 },
  animate: { opacity: 1 },
  exit: { opacity: 0 },
};

function getVariant(screen: ScreenId) {
  if (screen === "detail") return slideUp;
  if (screen === "directory-exclusions") return slideRight;
  return fade;
}

export default function App() {
  const [nav, setNav] = useState<NavState>({ screen: "onboarding" });
  const [activeTab, setActiveTab] = useState<TabId>("search");

  const navigate = useCallback((screen: ScreenId, selectedMediaId?: string) => {
    if ((TOP_LEVEL as string[]).includes(screen)) {
      setActiveTab(screen as TabId);
    }
    setNav({ screen, selectedMediaId });
  }, []);

  const goBack = useCallback(() => {
    if (nav.screen === "detail") navigate(activeTab);
    else if (nav.screen === "directory-exclusions") navigate("settings");
    else navigate("search");
  }, [nav.screen, activeTab, navigate]);

  const isTopLevel = (TOP_LEVEL as string[]).includes(nav.screen);
  const variant = getVariant(nav.screen);

  return (
    <div className="w-full h-full bg-[#0A0A14] flex items-center justify-center overflow-hidden">
      {/* Phone frame */}
      <div
        className="relative flex flex-col bg-[#121212] overflow-hidden"
        style={{
          width: "min(412px, 100%)",
          height: "min(915px, 100%)",
          borderRadius: "clamp(0px, 3vw, 44px)",
          boxShadow: "0 0 0 clamp(0px, 1vw, 8px) #050508, 0 24px 80px rgba(0,0,0,0.9)",
        }}
      >
        {/* Status bar */}
        <div className="flex-none h-10 bg-[#0D0D0D] flex items-center justify-between px-6">
          <span className="text-[#E0E0E0] text-xs font-medium">9:41</span>
          <div className="flex items-center gap-1.5">
            <svg width="16" height="12" viewBox="0 0 16 12" fill="none">
              <rect x="0" y="3" width="3" height="9" rx="1" fill="#E0E0E0" />
              <rect x="4.5" y="2" width="3" height="10" rx="1" fill="#E0E0E0" />
              <rect x="9" y="0.5" width="3" height="11.5" rx="1" fill="#E0E0E0" />
              <rect x="13.5" y="0" width="2.5" height="12" rx="1" fill="#E0E0E0" opacity="0.4" />
            </svg>
            <svg width="15" height="12" viewBox="0 0 15 12" fill="none">
              <path d="M7.5 2.5C9.8 2.5 11.9 3.5 13.3 5L14.8 3.5C13 1.3 10.4 0 7.5 0C4.6 0 2 1.3 0.2 3.5L1.7 5C3.1 3.5 5.2 2.5 7.5 2.5Z" fill="#E0E0E0" />
              <path d="M7.5 5.5C9 5.5 10.3 6.1 11.3 7L12.8 5.5C11.4 4.2 9.5 3.5 7.5 3.5C5.5 3.5 3.6 4.2 2.2 5.5L3.7 7C4.7 6.1 6 5.5 7.5 5.5Z" fill="#E0E0E0" />
              <circle cx="7.5" cy="10" r="2" fill="#E0E0E0" />
            </svg>
            <span className="text-[#E0E0E0] text-xs font-medium">100%</span>
          </div>
        </div>

        {/* Screen area */}
        <div className="flex-1 overflow-hidden relative">
          <AnimatePresence mode="wait">
            <motion.div
              key={nav.screen}
              initial={variant.initial}
              animate={variant.animate}
              exit={variant.exit}
              transition={{ duration: 0.3, ease: [0.4, 0, 0.2, 1] }}
              className="absolute inset-0 flex flex-col"
            >
              {nav.screen === "onboarding" && (
                <OnboardingScreen onComplete={() => navigate("search")} />
              )}
              {nav.screen === "search" && (
                <SearchScreen onNavigateToDetail={(id) => navigate("detail", id)} />
              )}
              {nav.screen === "timeline" && (
                <TimelineScreen onNavigateToDetail={(id) => navigate("detail", id)} />
              )}
              {nav.screen === "settings" && (
                <SettingsScreen onNavigateToExclusions={() => navigate("directory-exclusions")} />
              )}
              {nav.screen === "detail" && nav.selectedMediaId && (
                <MediaDetailScreen mediaId={nav.selectedMediaId} onBack={goBack} />
              )}
              {nav.screen === "directory-exclusions" && (
                <DirectoryExclusionScreen onBack={goBack} />
              )}
            </motion.div>
          </AnimatePresence>
        </div>

        {/* Bottom nav (top-level only) */}
        {isTopLevel && (
          <BottomNav
            activeTab={activeTab}
            onTabChange={(tab) => navigate(tab)}
          />
        )}
      </div>
    </div>
  );
}
