import { useState } from "react";
import { Lock } from "lucide-react";

type PermState = "NOT_REQUESTED" | "GRANTED" | "DENIED" | "PERMANENTLY_DENIED";

interface OnboardingScreenProps {
  onComplete: () => void;
}

export function OnboardingScreen({ onComplete }: OnboardingScreenProps) {
  const [permState, setPermState] = useState<PermState>("NOT_REQUESTED");
  const [showDialog, setShowDialog] = useState(false);

  const handleGrantAccess = () => {
    setShowDialog(true);
  };

  const handleDialogGrant = () => {
    setShowDialog(false);
    setPermState("GRANTED");
  };

  const handleDialogDismiss = () => {
    setShowDialog(false);
    setPermState("DENIED");
  };

  const handleOpenSettings = () => {
    setPermState("GRANTED");
  };

  const handleIveGranted = () => {
    setPermState("GRANTED");
  };

  return (
    <div className="flex-1 bg-[#121212] flex flex-col items-center justify-center px-8 relative">
      {/* Hero icon */}
      <Lock size={72} color="#F5A623" aria-hidden="true" />

      <div className="h-6" />

      {/* Title */}
      <h1 className="text-[#E8E8E8] text-[28px] leading-9 font-semibold text-center">
        Welcome to Recall
      </h1>

      <div className="h-3" />

      {/* Description */}
      <p className="text-[#B0B0B0] text-base leading-6 text-center">
        Your personal photo memory assistant. Recall searches your library using on-device AI — your
        photos never leave your phone.
      </p>

      <div className="h-8" />

      {/* State-based CTA */}
      {permState === "NOT_REQUESTED" && (
        <button
          onClick={handleGrantAccess}
          className="w-full bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
        >
          Grant Access
        </button>
      )}

      {permState === "GRANTED" && (
        <button
          onClick={onComplete}
          className="w-full bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
        >
          Continue
        </button>
      )}

      {permState === "DENIED" && (
        <>
          <button
            onClick={handleGrantAccess}
            className="w-full bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
          >
            Grant Access
          </button>
        </>
      )}

      {permState === "PERMANENTLY_DENIED" && (
        <>
          <p className="text-[#CF6679] text-sm leading-5 text-center mb-4">
            Media access was denied. Open Settings to grant permission.
          </p>
          <button
            onClick={handleOpenSettings}
            className="w-full bg-[#F5A623] text-[#1A1A1A] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
          >
            Open Settings
          </button>
          <div className="h-2" />
          <button
            onClick={handleIveGranted}
            className="w-full border border-[#F5A623] text-[#F5A623] py-3 rounded-full text-sm font-medium active:opacity-80 transition-opacity"
          >
            I've granted access
          </button>
        </>
      )}

      {/* Demo links */}
      <div className="mt-6 flex gap-4">
        <button
          onClick={() => setPermState("PERMANENTLY_DENIED")}
          className="text-[#B0B0B0] text-xs underline underline-offset-2"
        >
          Simulate denial
        </button>
        <button
          onClick={onComplete}
          className="text-[#B0B0B0] text-xs underline underline-offset-2"
        >
          Skip →
        </button>
      </div>

      {/* Rationale dialog */}
      {showDialog && (
        <div className="absolute inset-0 bg-black/70 flex items-center justify-center p-8 z-50">
          <div className="bg-[#2C2C2C] rounded-3xl p-6 w-full">
            <div className="flex justify-center mb-3">
              <Lock size={24} color="#F5A623" />
            </div>
            <h2 className="text-[#E0E0E0] text-xl font-semibold text-center mb-3">
              Media access needed
            </h2>
            <p className="text-[#B0B0B0] text-sm leading-5 text-center mb-6">
              Recall needs access to your photos and videos to search and organize your memories. All
              processing stays on your device.
            </p>
            <div className="flex justify-end gap-2">
              <button
                onClick={handleDialogDismiss}
                className="text-[#F5A623] px-4 py-2 text-sm font-medium active:opacity-70"
              >
                Not now
              </button>
              <button
                onClick={handleDialogGrant}
                className="text-[#F5A623] px-4 py-2 text-sm font-medium active:opacity-70"
              >
                Grant access
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
