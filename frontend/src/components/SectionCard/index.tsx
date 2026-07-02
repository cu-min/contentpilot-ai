import { Card } from 'antd';
import type { CardProps } from 'antd';
import './style.css';

export default function SectionCard(props: CardProps) {
  return <Card {...props} className={`section-card ${props.className || ''}`} />;
}
