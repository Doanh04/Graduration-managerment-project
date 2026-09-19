import React from 'react';
import logo from '../../img/sv_logo_dashboard.png';
import '../../style/PageLoadingSkeleton.scss';

export default function PageLoadingSkeleton({ fullScreen = false, message = 'Đang tải dữ liệu...' }) {
  return (
    <div className={`page-loading-skeleton ${fullScreen ? 'full-screen' : 'in-layout'}`} role="status" aria-live="polite" aria-label={message}>
      <div className="loading-skeleton-canvas" aria-hidden="true">
        <span className="skeleton-line heading" />
        <span className="skeleton-line subtitle" />
        <div className="skeleton-cards"><span /><span /><span /></div>
        <div className="skeleton-panel"><span /><span /><span /><span /></div>
      </div>
      <div className="loading-logo-wrap">
        <img src={logo} alt="" />
        <strong>{message}</strong>
        <span>Vui lòng chờ trong giây lát</span>
      </div>
      <div className="diagonal-shimmer" aria-hidden="true" />
    </div>
  );
}
