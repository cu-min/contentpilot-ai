import { Card, Typography } from 'antd';

export default function Product() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>产品配置</Typography.Title>
      <Typography.Text className="muted-text">阶段 2 后续接入单产品配置。</Typography.Text>
    </Card>
  );
}
