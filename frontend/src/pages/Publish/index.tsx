import { Card, Typography } from 'antd';

export default function Publish() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>发布任务</Typography.Title>
      <Typography.Text className="muted-text">阶段 6 后续接入发布任务。</Typography.Text>
    </Card>
  );
}
