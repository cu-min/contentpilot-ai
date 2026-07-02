import { Button, Card, Form, Input, Typography, message } from 'antd';
import { useNavigate } from 'react-router-dom';
import { login } from '../../api/auth';
import { authStore } from '../../stores';
import './style.css';

interface LoginFormValues {
  username: string;
  password: string;
}

export default function Login() {
  const navigate = useNavigate();

  const handleFinish = async (values: LoginFormValues) => {
    try {
      const result = await login(values);
      authStore.setAuth(result.data.token, result.data.user);
      message.success('登录成功');
      navigate('/dashboard', { replace: true });
    } catch (error) {
      message.error(error instanceof Error ? error.message : '登录失败');
    }
  };

  return (
    <main className="login-page">
      <Card className="login-card">
        <Typography.Title level={3}>AI内容营销系统</Typography.Title>
        <Form layout="vertical" onFinish={handleFinish} initialValues={{ username: 'admin' }}>
          <Form.Item
            label="用户名"
            name="username"
            rules={[{ required: true, message: '请输入用户名' }]}
          >
            <Input placeholder="请输入用户名" autoComplete="username" />
          </Form.Item>
          <Form.Item
            label="密码"
            name="password"
            rules={[{ required: true, message: '请输入密码' }]}
          >
            <Input.Password placeholder="请输入密码" autoComplete="current-password" />
          </Form.Item>
          <Button type="primary" block htmlType="submit">
            登录
          </Button>
        </Form>
      </Card>
    </main>
  );
}
