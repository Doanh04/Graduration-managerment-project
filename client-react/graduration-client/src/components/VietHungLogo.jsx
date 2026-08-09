import React from 'react';

/**
 * Logo chính thức Trường Đại Học Công Nghiệp Việt - Hưng
 * Tone màu: Xanh Cyan (#00A3D9) & Trắng
 */
export default function VietHungLogo({ className = '', height = 48, showSubtitle = true }) {
  return (
    <div className={`flex items-center gap-3 select-none ${className}`} style={{ height: `${height}px` }}>
      {/* Biểu tượng logo VH cách điệu */}
      <svg
        viewBox="0 0 160 160"
        fill="none"
        xmlns="http://www.w3.org/2000/svg"
        style={{ height: '100%', width: 'auto', aspectRatio: '1/1' }}
      >
        <circle cx="80" cy="80" r="76" fill="#00A3D9" fillOpacity="0.08" stroke="#00A3D9" strokeWidth="3" />
        <path
          d="M35 45L65 115H85L55 45H35Z"
          fill="#00A3D9"
        />
        <path
          d="M60 45L90 115H110L80 45H60Z"
          fill="#0084C7"
        />
        <path
          d="M40 75C60 60 100 60 120 75C125 78.8 128 85 125 90C120 100 90 125 45 115"
          stroke="#00A3D9"
          strokeWidth="10"
          strokeLinecap="round"
        />
      </svg>

      {/* Chữ thương hiệu Đại Học Công Nghiệp Việt - Hưng */}
      <div className="flex flex-col justify-center leading-none">
        <span
          className="font-bold tracking-wider text-[#00A3D9]"
          style={{ fontSize: `${height * 0.32}px`, fontFamily: "'Inter', 'Segoe UI', sans-serif" }}
        >
          ĐẠI HỌC CÔNG NGHIỆP
        </span>
        <span
          className="font-extrabold tracking-widest text-[#0077B6] mt-1"
          style={{ fontSize: `${height * 0.42}px`, fontFamily: "'Inter', 'Segoe UI', sans-serif" }}
        >
          VIỆT - HƯNG
        </span>
        {showSubtitle && (
          <span
            className="text-[9px] font-medium tracking-tight text-slate-500 mt-1 uppercase"
            style={{ fontSize: `${Math.max(8, height * 0.16)}px` }}
          >
            VIET HUNG INDUSTRIAL UNIVERSITY
          </span>
        )}
      </div>
    </div>
  );
}
