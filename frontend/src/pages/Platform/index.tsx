import { Card, Typography } from 'antd';

export default function Platform() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>平台管理</Typography.Title>
      <Typography.Text className="muted-text">后续接入平台列表与账号配置。</Typography.Text>
    </Card>
  );
}
