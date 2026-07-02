import { Card, Typography } from 'antd';

export default function Log() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>操作日志</Typography.Title>
      <Typography.Text className="muted-text">后续接入基础操作日志展示。</Typography.Text>
    </Card>
  );
}
