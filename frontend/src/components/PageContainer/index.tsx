import { Typography } from 'antd';
import type { ReactNode } from 'react';
import './style.css';

interface PageContainerProps {
  title: string;
  description?: string;
  children: ReactNode;
}

export default function PageContainer({ title, description, children }: PageContainerProps) {
  return (
    <div className="page-container">
      <div className="page-hero">
        <div>
          <Typography.Title level={3} className="page-title">
            {title}
          </Typography.Title>
          {description && (
            <Typography.Paragraph className="page-description">
              {description}
            </Typography.Paragraph>
          )}
        </div>
      </div>
      {children}
    </div>
  );
}
