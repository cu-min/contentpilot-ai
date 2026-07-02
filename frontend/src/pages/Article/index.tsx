import { Card, Typography } from 'antd';

export default function Article() {
  return (
    <Card className="page-panel">
      <Typography.Title level={3}>文章库</Typography.Title>
      <Typography.Text className="muted-text">阶段 2 后续接入文章列表与详情。</Typography.Text>
    </Card>
  );
}
