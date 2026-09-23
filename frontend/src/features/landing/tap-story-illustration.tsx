/**
 * A flat, monochrome illustration (not a photorealistic cartoon or a real video - this repo
 * has no video/animation-generation tool) of two people tapping phones to share a contact,
 * animated with CSS. Kept grayscale to match the site's black-and-white palette.
 */
export function TapStoryIllustration() {
  return (
    <svg viewBox="0 0 640 260" className="mx-auto h-auto w-full max-w-2xl" role="img" aria-label="Two people tapping their phones together to share a contact card">
      {/* Girl */}
      <g className="hero-float">
        <ellipse cx="150" cy="80" rx="34" ry="38" fill="#404040" />
        <circle cx="150" cy="88" r="24" fill="#d9d9d9" />
        <polygon points="128,150 172,150 190,230 110,230" fill="#e5e5e5" />
        <line x1="172" y1="165" x2="253" y2="140" stroke="#e5e5e5" strokeWidth="14" strokeLinecap="round" />
        <rect x="247" y="126" width="18" height="30" rx="3" fill="#171717" stroke="#f5f5f5" strokeWidth="1.5" transform="rotate(-14 256 141)" />
      </g>

      {/* Boy */}
      <g className="hero-float" style={{ animationDelay: "1.4s" }}>
        <path d="M466,58 Q490,36 514,58 L514,74 L466,74 Z" fill="#262626" />
        <circle cx="490" cy="88" r="24" fill="#c9c9c9" />
        <polygon points="466,150 514,150 522,230 458,230" fill="#d4d4d4" />
        <line x1="466" y1="165" x2="385" y2="140" stroke="#d4d4d4" strokeWidth="14" strokeLinecap="round" />
        <rect x="375" y="126" width="18" height="30" rx="3" fill="#171717" stroke="#f5f5f5" strokeWidth="1.5" transform="rotate(14 384 141)" />
      </g>

      {/* Tap connection */}
      <circle className="story-pulse-el" cx="320" cy="141" r="16" fill="none" stroke="#f5f5f5" strokeWidth="2" />
      <g className="story-card-el">
        <rect x="256" y="130" width="22" height="15" rx="3" fill="#f5f5f5" />
        <line x1="260" y1="135.5" x2="274" y2="135.5" stroke="#0a0a0a" strokeWidth="1.3" />
        <line x1="260" y1="139.5" x2="269" y2="139.5" stroke="#0a0a0a" strokeWidth="1.3" />
      </g>
    </svg>
  );
}
