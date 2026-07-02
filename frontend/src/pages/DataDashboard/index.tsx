import { Card, Typography } from 'antd';

export default function DataDashboard() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>数据看板</Typography.Title>
      <Typography.Text className="muted-text">阶段 5 后续接入基础统计与排行。</Typography.Text>
    </Card>
  );
}
