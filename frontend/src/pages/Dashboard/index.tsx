import { Alert, Card, Space, Spin, Typography } from 'antd';
import { useEffect, useState } from 'react';
import { getHealth } from '../../api/health';

export default function Dashboard() {
  const [healthMessage, setHealthMessage] = useState('');
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    getHealth()
      .then((result) => {
        setHealthMessage(result.data);
      })
      .catch(() => {
        setError('后端健康检查暂不可用');
      })
      .finally(() => {
        setLoading(false);
      });
  }, []);

  return (
    <Space direction="vertical" size={16} style={{ width: '100%' }}>
      <Typography.Title level={3}>首页概览</Typography.Title>
      <Card className="page-panel">
        <Space direction="vertical" size={12}>
          <Typography.Text strong>后端健康检查</Typography.Text>
          {loading && <Spin />}
          {!loading && healthMessage && (
            <Alert type="success" message={healthMessage} showIcon />
          )}
          {!loading && error && <Alert type="error" message={error} showIcon />}
        </Space>
      </Card>
    </Space>
  );
}
